package com.marketplace.review.service;
import com.marketplace.admin.service.AuditService;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.order.repository.OrderItemRepository;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.review.dto.request.CreateReviewRequest;
import com.marketplace.review.dto.request.UpdateReviewRequest;
import com.marketplace.review.dto.response.ProductRatingResponse;
import com.marketplace.review.dto.response.ReviewResponse;
import com.marketplace.review.entity.Review;
import com.marketplace.review.entity.ReviewStatus;
import com.marketplace.review.repository.ReviewRepository;
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
public class ReviewService {
    private final ReviewRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    @Transactional
    public ReviewResponse create(String username, CreateReviewRequest request) {
        User user = user(username);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!orderItemRepository.hasPurchaseWithStatus(user.getId(), product.getId(), SellerOrderStatus.CONFIRMED)) {
            throw new BusinessRuleException("A confirmed purchase is required before reviewing");
        }
        if (repository.findByUser_IdAndProduct_Id(user.getId(), product.getId()).isPresent()) {
            throw new ConflictException("You have already reviewed this product");
        }
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.rating());
        review.setComment(clean(request.comment()));
        return response(repository.saveAndFlush(review));
    }

    @Transactional
    public ReviewResponse update(String username, UUID id, UpdateReviewRequest request) {
        User user = user(username);
        Review review = repository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setRating(request.rating());
        review.setComment(clean(request.comment()));
        review.setStatus(ReviewStatus.PENDING);
        return response(review);
    }

    @Transactional
    public void delete(String username, UUID id) {
        User user = user(username);
        Review review = repository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        repository.delete(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> publicReviews(UUID productId, Pageable pageable) {
        return repository.findAllByProduct_IdAndStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> myReviews(String username, Pageable pageable) {
        return repository.findAllByUser_Id(user(username).getId(), pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public ProductRatingResponse rating(UUID productId) {
        if (!productRepository.existsById(productId)) throw new ResourceNotFoundException("Product not found");
        long count = repository.countByProduct_IdAndStatus(productId, ReviewStatus.APPROVED);
        Double average = repository.averageRating(productId, ReviewStatus.APPROVED);
        return new ProductRatingResponse(productId, count, average == null ? 0 : average);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> pending(Pageable pageable) {
        return repository.findAllByStatus(ReviewStatus.PENDING, pageable).map(this::response);
    }

    @Transactional
    public ReviewResponse moderate(UUID id, boolean approve, String actorName) {
        Review review = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new ConflictException("Only pending reviews can be moderated");
        }
        review.setStatus(approve ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
        auditService.record(user(actorName), approve ? "APPROVE_REVIEW" : "REJECT_REVIEW",
                "REVIEW", id, null);
        return response(review);
    }

    private User user(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String clean(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private ReviewResponse response(Review review) {
        return new ReviewResponse(review.getId(), review.getProduct().getId(),
                review.getUser().getUsername(), review.getRating(), review.getComment(),
                review.getStatus(), review.getCreatedAt());
    }
}
