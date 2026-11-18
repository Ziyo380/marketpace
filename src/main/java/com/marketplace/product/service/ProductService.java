package com.marketplace.product.service;

import com.marketplace.category.entity.Category;
import com.marketplace.category.repository.CategoryRepository;
import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.entity.Inventory;
import com.marketplace.inventory.repository.InventoryRepository;
import com.marketplace.product.dto.request.CreateProductRequest;
import com.marketplace.product.dto.request.UpdateProductRequest;
import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.entity.ProductVariant;
import com.marketplace.product.mapper.ProductMapper;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Subquery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;



@Service
@RequiredArgsConstructor
public class ProductService{
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final StoreRepository storeRepository;
    private final ProductMapper productMapper;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public ProductResponse createProduct(String username, CreateProductRequest request){
        User user = userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus() !=SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage products");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        if (!store.isActive()){
            throw new BusinessRuleException("Store with ID '" + store.getId() + "' is inactive");
        }
        Category category=categoryRepository.findById(request.categoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category with ID '" + request.categoryId() + "' was not found"));
        if (!category.isActive()){
            throw new BusinessRuleException("Category with ID '" + request.categoryId() + "' is inactive");
        }
        boolean exists=productRepository.existsByStore_IdAndSlug(store.getId(),request.slug());
        if (exists){
            throw new ConflictException("A product with slug '" + request.slug() + "' already exists in this store");
        }
        Product product=new Product();
        product.setStore(store);
        product.setCategory(category);
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setStatus(ProductStatus.DRAFT);
        product.setPrice(request.price());
        Product savedProduct=productRepository.save(product);
        Inventory inventory=new Inventory();
        inventory.setProduct(savedProduct);
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);
        return productMapper.toResponse(savedProduct);


    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(String username, Pageable pageable){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        Page<Product> products=productRepository.findAllByStore_Id(store.getId(),pageable);
        Page<ProductResponse> response=products.map(productMapper::toResponse);
        return response;

    }

    @Transactional(readOnly = true)
    public ProductResponse getMyProductById(String username, UUID productId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with ID '" + productId + "' was not found in your store"));
        return productMapper.toResponse(product);

    }

    @Transactional
    public ProductResponse updateProduct(String username,UUID productId, UpdateProductRequest request){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus() !=SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage products");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        if (!store.isActive()){
            throw new BusinessRuleException("Store with ID '" + store.getId() + "' is inactive");
        }
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with ID '" + productId + "' was not found in your store"));
        Category category=categoryRepository.findById(request.categoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category with ID '" + request.categoryId() + "' was not found"));
        if (!category.isActive()){
            throw new BusinessRuleException("Category with ID '" + request.categoryId() + "' is inactive");
        }
        if (!product.getSlug().equals(request.slug())){
            boolean exists=productRepository.existsByStore_IdAndSlug(store.getId(),request.slug());
            if (exists){
                throw new ConflictException("A product with slug '" + request.slug() + "' already exists in this store");
            }

        }
        product.setCategory(category);
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setPrice(request.price());
        Product savedProduct=productRepository.save(product);
        return productMapper.toResponse(savedProduct);

    }

    @Transactional
    public ProductResponse publishMyProduct(String username,UUID productId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus() !=SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage products");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        if (!store.isApproved()) {
            throw new BusinessRuleException("Store requires administrator approval");
        }
        if (!store.isActive()){
            throw new BusinessRuleException("Store with ID '" + store.getId() + "' is inactive");
        }
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with ID '" + productId + "' was not found in your store"));
        if (product.getStatus() != ProductStatus.DRAFT){
            throw new BusinessRuleException("Only draft products can be published");
        }
        if (product.isModerationBlocked()) {
            throw new BusinessRuleException("Product is blocked by an administrator");
        }
        Category category=product.getCategory();
        if (!category.isActive()){
            throw new BusinessRuleException("Category with ID '" + category.getId() + "' is inactive");
        }
        if (product.getPrice()==null || product.getPrice().compareTo(BigDecimal.ZERO)<=0){
            throw new BusinessRuleException("Price must be greater than zero");
        }
        product.setStatus(ProductStatus.PUBLISHED);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse archiveMyProduct(String username,UUID productId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus() !=SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage products");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with ID '" + productId + "' was not found in your store"));
        if (product.getStatus() != ProductStatus.PUBLISHED){
            throw new BusinessRuleException("Only published products can be archived");
        }
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }


    @Transactional
    public ProductResponse restoreMyProduct(String username,UUID productId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus() !=SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage products");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with ID '" + productId + "' was not found in your store"));
        if (product.getStatus() != ProductStatus.ARCHIVED){
            throw new BusinessRuleException("Only archived products can be restored");
        }
        product.setStatus(ProductStatus.DRAFT);
        productRepository.save(product);
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(UUID categoryId, String search,BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable)  {
        Pageable preparedPageable = preparePublicProductPageable(pageable);
        validatePriceRange(minPrice,maxPrice);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<Product> products;

        if (categoryId == null && normalizedSearch == null) {
            if (minPrice == null && maxPrice == null){
                products = productRepository.findAllByStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            }else if (minPrice != null && maxPrice == null){
                products=productRepository.findAllByPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        minPrice,
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            }else if (minPrice == null && maxPrice != null){
                products=productRepository.findAllByPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        maxPrice,
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            } else {
                products=productRepository.findAllByPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        minPrice,
                        maxPrice,
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            }

        } else if (categoryId != null && normalizedSearch == null) {
            if (minPrice == null && maxPrice == null){
                products=productRepository.findAllByCategory_IdAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        categoryId,
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            }else if (minPrice != null && maxPrice == null){
                products=productRepository.findAllByCategory_IdAndPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        categoryId,
                        minPrice,
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            }else if (minPrice == null && maxPrice != null){
                products=productRepository.findAllByCategory_IdAndPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        categoryId,
                        maxPrice,
                        ProductStatus.PUBLISHED,
                        SellerStatus.APPROVED,
                        preparedPageable
                );
            } else products=productRepository.findAllByCategory_IdAndPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                    categoryId,
                    minPrice,
                    maxPrice,
                    ProductStatus.PUBLISHED,
                    SellerStatus.APPROVED,
                    preparedPageable
            );

        } else if (categoryId == null) {
            if (minPrice == null && maxPrice == null){
                products = productRepository.findAllByNameContainingIgnoreCaseAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        normalizedSearch, ProductStatus.PUBLISHED, SellerStatus.APPROVED, preparedPageable);
            }else if (minPrice != null && maxPrice == null){
                products=productRepository.findAllByNameContainingIgnoreCaseAndPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        normalizedSearch,minPrice,ProductStatus.PUBLISHED,SellerStatus.APPROVED,preparedPageable
                );
            }else if (minPrice == null && maxPrice != null){
                products=productRepository.findAllByNameContainingIgnoreCaseAndPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        normalizedSearch,maxPrice,ProductStatus.PUBLISHED,SellerStatus.APPROVED,preparedPageable
                );
            }else products=productRepository.findAllByNameContainingIgnoreCaseAndPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                    normalizedSearch,minPrice,maxPrice,ProductStatus.PUBLISHED,SellerStatus.APPROVED,preparedPageable
            );
        } else {
            if (minPrice == null && maxPrice == null){
                products = productRepository.findAllByCategory_IdAndNameContainingIgnoreCaseAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        categoryId, normalizedSearch, ProductStatus.PUBLISHED, SellerStatus.APPROVED, preparedPageable);
            }else if (minPrice != null && maxPrice == null){
                products=productRepository.findAllByCategory_IdAndNameContainingIgnoreCaseAndPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        categoryId,normalizedSearch,minPrice,ProductStatus.PUBLISHED,SellerStatus.APPROVED,preparedPageable
                );
            }else if (minPrice == null && maxPrice != null){
                products=productRepository.findAllByCategory_IdAndNameContainingIgnoreCaseAndPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                        categoryId,normalizedSearch,maxPrice,ProductStatus.PUBLISHED,SellerStatus.APPROVED,preparedPageable
                );
            }else products=productRepository.findAllByCategory_IdAndNameContainingIgnoreCaseAndPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
                    categoryId,normalizedSearch,minPrice,maxPrice,ProductStatus.PUBLISHED,SellerStatus.APPROVED,preparedPageable
            );
        }

        return products.map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(UUID categoryId, String search,
            BigDecimal minPrice, BigDecimal maxPrice, String storeSlug, boolean availableOnly,
            Pageable pageable) {
        if ((storeSlug == null || storeSlug.isBlank()) && !availableOnly) {
            return getPublicProducts(categoryId, search, minPrice, maxPrice, pageable);
        }
        validatePriceRange(minPrice, maxPrice);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedStore = storeSlug == null || storeSlug.isBlank() ? null : storeSlug.trim();
        Specification<Product> specification = (root, query, cb) -> {
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            conditions.add(cb.equal(root.get("status"), ProductStatus.PUBLISHED));
            conditions.add(cb.isFalse(root.get("moderationBlocked")));
            conditions.add(cb.isTrue(root.get("store").get("active")));
            conditions.add(cb.isTrue(root.get("store").get("approved")));
            conditions.add(cb.equal(root.get("store").get("seller").get("status"), SellerStatus.APPROVED));
            conditions.add(cb.isTrue(root.get("category").get("active")));
            if (categoryId != null) conditions.add(cb.equal(root.get("category").get("id"), categoryId));
            if (normalizedSearch != null) {
                String escaped = normalizedSearch.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                conditions.add(cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\'));
            }
            if (minPrice != null) conditions.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) conditions.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (normalizedStore != null) conditions.add(cb.equal(root.get("store").get("slug"), normalizedStore));
            if (availableOnly) {
                Subquery<Integer> stock = query.subquery(Integer.class);
                var inventory = stock.from(Inventory.class);
                stock.select(cb.literal(1)).where(
                        cb.equal(inventory.get("product").get("id"), root.get("id")),
                        cb.greaterThan(cb.diff(inventory.get("quantity"), inventory.get("reservedQuantity")), 0));
                Subquery<Integer> variants = query.subquery(Integer.class);
                var variant = variants.from(ProductVariant.class);
                variants.select(cb.literal(1)).where(
                        cb.equal(variant.get("product").get("id"), root.get("id")),
                        cb.isTrue(variant.get("active")),
                        cb.greaterThan(cb.diff(variant.get("quantity"), variant.get("reservedQuantity")), 0));
                Subquery<Integer> anyVariant = query.subquery(Integer.class);
                var otherVariant = anyVariant.from(ProductVariant.class);
                anyVariant.select(cb.literal(1)).where(
                        cb.equal(otherVariant.get("product").get("id"), root.get("id")));
                conditions.add(cb.or(cb.exists(variants), cb.and(cb.not(cb.exists(anyVariant)), cb.exists(stock))));
            }
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return productRepository.findAll(specification, preparePublicProductPageable(pageable))
                .map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublicProductBySlugs(String storeSlug, String productSlug){
        Store store=storeRepository.findBySlug(storeSlug)
                .orElseThrow(()-> new ResourceNotFoundException("Store with slug '" + storeSlug + "' was not found"));
        if (!store.isActive() || !store.isApproved()){
            throw new ResourceNotFoundException("Store with slug '" + storeSlug + "' is inactive or unavailable");
        }
        Seller seller=store.getSeller();
        if (seller.getStatus() != SellerStatus.APPROVED){
            throw new ResourceNotFoundException("This product is not available");
        }
        Product product=productRepository.findByStore_IdAndSlug(store.getId(), productSlug)
                .orElseThrow(()-> new ResourceNotFoundException("Product with slug '" + productSlug + "' was not found in this store"));
        if (product.getStatus() != ProductStatus.PUBLISHED){
            throw new ResourceNotFoundException("Product with slug '" + productSlug + "' is not available");
        }
        if (product.isModerationBlocked()) {
            throw new ResourceNotFoundException("Product is not available");
        }
        Category category=product.getCategory();
        if (!category.isActive()){
            throw new ResourceNotFoundException("Product with slug '" + productSlug + "' is not available");
        }
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProductsByStoreSlug(String storeSlug, Pageable pageable){
        Store store=storeRepository.findBySlug(storeSlug)
                .orElseThrow(()-> new ResourceNotFoundException("Store with slug '" + storeSlug + "' was not found"));
        if (!store.isActive() || !store.isApproved()){
            throw new ResourceNotFoundException("Store with slug '" + storeSlug + "' is inactive or unavailable");
        }
        if (store.getSeller().getStatus() != SellerStatus.APPROVED){
            throw new ResourceNotFoundException("Store with slug '" + storeSlug + "' is unavailable");
        }

        Pageable preparedPageable=preparePublicProductPageable(pageable);
        Page<Product> products=productRepository.findAllByStore_IdAndStatusAndModerationBlockedFalseAndCategory_ActiveTrue(
                store.getId(),
                ProductStatus.PUBLISHED,
                preparedPageable
        );
        return products.map(productMapper::toResponse);
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price filter cannot be negative");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price filter cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice)>0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MinPrice cannot be greater than MaxPrice");
        }
    }

    private Pageable preparePublicProductPageable(Pageable pageable){
        Sort sort=pageable.getSort();
        if (sort.isUnsorted()){
            sort=Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        }else {
            Set<String> allowedFields=Set.of(
                    "price",
                    "name",
                    "createdAt"
            );

            for (Sort.Order order: sort){
                if (!allowedFields.contains(order.getProperty())){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorting by " + order.getProperty() + " is not allowed");
                }
            }
            sort=sort.and(Sort.by(Sort.Order.asc("id")));
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),sort
        );

    }

}


