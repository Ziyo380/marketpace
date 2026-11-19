
package com.marketplace.product.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.product.dto.request.ReorderProductImagesRequest;
import com.marketplace.product.dto.response.ProductImageResponse;
import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductImage;
import com.marketplace.product.mapper.ProductImageMapper;
import com.marketplace.product.repository.ProductImageRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.storage.config.StorageProperties;
import com.marketplace.storage.dto.StoredObjectResponse;
import com.marketplace.storage.exception.StorageOperationException;
import com.marketplace.storage.service.ObjectStorageService;
import com.marketplace.storage.service.StorageCleanupService;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final StoreRepository storeRepository;
    private final ProductService productService;
    private final ProductImageMapper productImageMapper;
    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;
    private final StorageCleanupService storageCleanupService;

    @Transactional
    public ProductImageResponse uploadImage(String username, UUID productId, MultipartFile file) {
        Product product = getOwnedProduct(username, productId);
        validateFile(file);

        List<ProductImage> currentImages = productImageRepository
                .findAllByProduct_IdOrderBySortOrderAsc(productId);
        if (currentImages.size() >= storageProperties.getMaxImagesPerProduct()) {
            throw new BusinessRuleException(
                    "A product cannot have more than "
                            + storageProperties.getMaxImagesPerProduct()
                            + " images"
            );
        }

        String contentType = file.getContentType();
        String objectKey = buildObjectKey(productId, contentType);
        StoredObjectResponse storedObject;
        try (InputStream inputStream = file.getInputStream()) {
            storedObject = objectStorageService.upload(
                    objectKey,
                    inputStream,
                    file.getSize(),
                    contentType
            );
            storageCleanupService.deleteAfterRollback(storedObject.objectKey());
        } catch (IOException exception) {
            throw new StorageOperationException("Could not read uploaded image", exception);
        }

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setObjectKey(storedObject.objectKey());
        image.setOriginalFilename(normalizeFilename(file.getOriginalFilename(), contentType));
        image.setContentType(contentType);
        image.setSizeBytes(file.getSize());
        image.setPrimary(currentImages.isEmpty());
        image.setSortOrder(nextSortOrder(currentImages));
        return productImageMapper.toResponse(productImageRepository.saveAndFlush(image));
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getMyProductImages(String username, UUID productId) {
        Product product = getOwnedProduct(username, productId);
        return toResponses(productImageRepository.findAllByProduct_IdOrderBySortOrderAsc(product.getId()));
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getPublicProductImages(String storeSlug, String productSlug) {
        ProductResponse product = productService.getPublicProductBySlugs(storeSlug, productSlug);
        return toResponses(productImageRepository.findAllByProduct_IdOrderBySortOrderAsc(product.id()));
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(String username, UUID productId, UUID imageId) {
        Product product = getOwnedProduct(username, productId);
        List<ProductImage> images = productImageRepository
                .findAllByProduct_IdOrderBySortOrderAsc(product.getId());
        ProductImage selectedImage = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image with ID '" + imageId + "' was not found for this product"
                ));

        if (!selectedImage.isPrimary()) {
            images.forEach(image -> image.setPrimary(image.getId().equals(imageId)));
            productImageRepository.saveAll(images);
        }
        return productImageMapper.toResponse(selectedImage);
    }

    @Transactional
    public List<ProductImageResponse> reorderImages(
            String username,
            UUID productId,
            ReorderProductImagesRequest request
    ) {
        Product product = getOwnedProduct(username, productId);
        List<ProductImage> images = productImageRepository
                .findAllByProduct_IdOrderBySortOrderAsc(product.getId());
        validateRequestedOrder(images, request.imageIds());

        int temporaryStart = images.stream()
                .mapToInt(ProductImage::getSortOrder)
                .max()
                .orElse(0) + images.size() + 1;
        for (int index = 0; index < images.size(); index++) {
            images.get(index).setSortOrder(temporaryStart + index);
        }
        productImageRepository.saveAllAndFlush(images);

        Map<UUID, ProductImage> imagesById = new HashMap<>();
        images.forEach(image -> imagesById.put(image.getId(), image));
        for (int index = 0; index < request.imageIds().size(); index++) {
            imagesById.get(request.imageIds().get(index)).setSortOrder(index);
        }
        productImageRepository.saveAllAndFlush(images);

        return request.imageIds().stream()
                .map(imagesById::get)
                .map(productImageMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteImage(String username, UUID productId, UUID imageId) {
        Product product = getOwnedProduct(username, productId);
        ProductImage image = productImageRepository.findByIdAndProduct_Id(imageId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image with ID '" + imageId + "' was not found for this product"
                ));

        List<ProductImage> images = productImageRepository
                .findAllByProduct_IdOrderBySortOrderAsc(product.getId());
        storageCleanupService.deleteAfterCommit(image.getObjectKey());
        productImageRepository.delete(image);

        if (image.isPrimary()) {
            images.stream()
                    .filter(candidate -> !candidate.getId().equals(imageId))
                    .findFirst()
                    .ifPresent(candidate -> {
                        candidate.setPrimary(true);
                        productImageRepository.save(candidate);
                    });
        }
    }

    private Product getOwnedProduct(String username, UUID productId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with username '" + username + "' was not found"
                ));
        Seller seller = sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller profile for user with ID '" + user.getId() + "' was not found"
                ));
        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Your seller account must be approved before you can manage product images"
            );
        }
        Store store = storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store for seller with ID '" + seller.getId() + "' was not found"
                ));
        if (!store.isActive() || !store.isApproved()) {
            throw new BusinessRuleException("Store with ID '" + store.getId() + "' is inactive");
        }
        return productRepository.findByIdAndStore_Id(productId, store.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with ID '" + productId + "' was not found in your store"
                ));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file must not be empty");
        }
        if (file.getSize() > storageProperties.getMaxFileSize().toBytes()) {
            throw new BusinessRuleException(
                    "Image file must not exceed " + storageProperties.getMaxFileSize()
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException("Only JPEG, PNG and WebP images are allowed");
        }
        validateFileSignature(file, contentType);
    }

    private void validateFileSignature(MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            boolean valid = switch (contentType) {
                case "image/jpeg" -> header.length >= 3
                        && unsigned(header[0]) == 0xFF
                        && unsigned(header[1]) == 0xD8
                        && unsigned(header[2]) == 0xFF;
                case "image/png" -> header.length >= 8
                        && unsigned(header[0]) == 0x89
                        && header[1] == 0x50
                        && header[2] == 0x4E
                        && header[3] == 0x47
                        && header[4] == 0x0D
                        && header[5] == 0x0A
                        && header[6] == 0x1A
                        && header[7] == 0x0A;
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == 'F'
                        && header[8] == 'W'
                        && header[9] == 'E'
                        && header[10] == 'B'
                        && header[11] == 'P';
                default -> false;
            };
            if (!valid) {
                throw new BusinessRuleException("File content does not match its image type");
            }
        } catch (IOException exception) {
            throw new BusinessRuleException("Uploaded image could not be read");
        }
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private int nextSortOrder(List<ProductImage> images) {
        return images.stream()
                .mapToInt(ProductImage::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private String buildObjectKey(UUID productId, String contentType) {
        return "products/" + productId + "/" + UUID.randomUUID() + extensionFor(contentType);
    }

    private String normalizeFilename(String originalFilename, String contentType) {
        String fallback = "image" + extensionFor(contentType);
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }
        String normalized = originalFilename.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(normalized.length() - 255);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private void validateRequestedOrder(List<ProductImage> images, List<UUID> requestedIds) {
        Set<UUID> existingIds = images.stream()
                .map(ProductImage::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> uniqueRequestedIds = new HashSet<>(requestedIds);
        if (requestedIds.size() != images.size()
                || uniqueRequestedIds.size() != requestedIds.size()
                || !uniqueRequestedIds.equals(existingIds)) {
            throw new BusinessRuleException(
                    "Image order must contain every product image exactly once"
            );
        }
    }

    private List<ProductImageResponse> toResponses(List<ProductImage> images) {
        return images.stream()
                .map(productImageMapper::toResponse)
                .toList();
    }

}
