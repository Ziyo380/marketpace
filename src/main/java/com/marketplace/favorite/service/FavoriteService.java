package com.marketplace.favorite.service;

import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.favorite.dto.response.FavoriteResponse;
import com.marketplace.favorite.entity.Favorite;
import com.marketplace.favorite.repository.FavoriteRepository;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.mapper.ProductMapper;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository repository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public Page<FavoriteResponse> list(String username, Pageable pageable) {
        User user = user(username);
        return repository.findAllByUser_Id(user.getId(), pageable).map(this::response);
    }

    @Transactional
    public FavoriteResponse add(String username, UUID productId) {
        User user = user(username);
        if (repository.findByUser_IdAndProduct_Id(user.getId(), productId).isPresent()) {
            throw new ConflictException("Product is already in favorites");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getStatus() != ProductStatus.PUBLISHED || !product.getStore().isActive()
                || !product.getStore().isApproved()
                || product.getStore().getSeller().getStatus() != SellerStatus.APPROVED) {
            throw new ResourceNotFoundException("Product not available");
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        return response(repository.saveAndFlush(favorite));
    }

    @Transactional
    public void remove(String username, UUID productId) {
        User user = user(username);
        Favorite favorite = repository.findByUser_IdAndProduct_Id(user.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        repository.delete(favorite);
    }

    private User user(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private FavoriteResponse response(Favorite favorite) {
        return new FavoriteResponse(favorite.getId(), mapper.toResponse(favorite.getProduct()),
                favorite.getCreatedAt());
    }
}
