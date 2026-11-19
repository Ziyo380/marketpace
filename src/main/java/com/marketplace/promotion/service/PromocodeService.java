package com.marketplace.promotion.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.entity.Order;
import com.marketplace.promotion.dto.request.CreatePromocodeRequest;
import com.marketplace.promotion.dto.response.DiscountResponse;
import com.marketplace.promotion.dto.response.PromocodeResponse;
import com.marketplace.promotion.entity.DiscountType;
import com.marketplace.promotion.entity.Promocode;
import com.marketplace.promotion.entity.PromocodeUsage;
import com.marketplace.promotion.repository.PromocodeRepository;
import com.marketplace.promotion.repository.PromocodeUsageRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromocodeService {
    private final PromocodeRepository repository;
    private final PromocodeUsageRepository usageRepository;
    private final UserRepository userRepository;

    @Transactional
    public PromocodeResponse create(CreatePromocodeRequest request) {
        String code = normalize(request.code());
        if (repository.findByCode(code).isPresent()) throw new ConflictException("Promocode already exists");
        if (request.discountType() == DiscountType.PERCENT
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("Percentage discount cannot exceed 100");
        }
        if (request.maxUses() != null && request.maxUses() <= 0) {
            throw new BusinessRuleException("Usage limit must be positive");
        }
        if (request.expiresAt() != null && request.startsAt() != null
                && !request.expiresAt().isAfter(request.startsAt())) {
            throw new BusinessRuleException("Expiration must be after start");
        }
        Promocode promo = new Promocode();
        promo.setCode(code);
        promo.setDiscountType(request.discountType());
        promo.setDiscountValue(request.discountValue());
        promo.setMinimumAmount(request.minimumAmount() == null ? BigDecimal.ZERO : request.minimumAmount());
        promo.setStartsAt(request.startsAt());
        promo.setExpiresAt(request.expiresAt());
        promo.setMaxUses(request.maxUses());
        return response(repository.saveAndFlush(promo));
    }

    @Transactional(readOnly = true)
    public Page<PromocodeResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::response);
    }

    @Transactional
    public PromocodeResponse deactivate(UUID id) {
        Promocode promo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promocode not found"));
        promo.setActive(false);
        return response(promo);
    }

    @Transactional(readOnly = true)
    public DiscountResponse preview(String code, BigDecimal subtotal, User user) {
        if (subtotal == null || subtotal.signum() <= 0) {
            throw new BusinessRuleException("Subtotal must be positive");
        }
        Promocode promo = repository.findByCode(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Promocode not found"));
        validate(promo, subtotal, user);
        BigDecimal discount = discount(promo, subtotal);
        return new DiscountResponse(promo.getCode(), subtotal, discount, subtotal.subtract(discount));
    }

    @Transactional(readOnly = true)
    public DiscountResponse previewForUser(String username, String code, BigDecimal subtotal) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return preview(code, subtotal, user);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(String code, BigDecimal subtotal, User user, Order order) {
        Promocode promo = repository.findByCodeForUpdate(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("Promocode not found"));
        validate(promo, subtotal, user);
        BigDecimal discount = discount(promo, subtotal);
        order.setPromocode(promo);
        order.setDiscountAmount(discount);
        promo.setUsedCount(promo.getUsedCount() + 1);
        PromocodeUsage usage = new PromocodeUsage();
        usage.setPromocode(promo);
        usage.setUser(user);
        usage.setOrder(order);
        usageRepository.save(usage);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Order order) {
        if (order.getPromocode() == null) return;
        usageRepository.findByOrder_Id(order.getId()).ifPresent(usage -> {
            Promocode promo = repository.findByCodeForUpdate(order.getPromocode().getCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Promocode not found"));
            promo.setUsedCount(Math.max(0, promo.getUsedCount() - 1));
            usageRepository.delete(usage);
        });
    }

    private void validate(Promocode promo, BigDecimal subtotal, User user) {
        Instant now = Instant.now();
        if (!promo.isActive() || (promo.getStartsAt() != null && promo.getStartsAt().isAfter(now))
                || (promo.getExpiresAt() != null && !promo.getExpiresAt().isAfter(now))) {
            throw new BusinessRuleException("Promocode is not active");
        }
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            throw new BusinessRuleException("Promocode usage limit reached");
        }
        if (subtotal.compareTo(promo.getMinimumAmount()) < 0) {
            throw new BusinessRuleException("Order amount is below promocode minimum");
        }
        if (usageRepository.existsByUser_IdAndPromocode_Id(user.getId(), promo.getId())) {
            throw new ConflictException("Promocode already used by this customer");
        }
    }

    private BigDecimal discount(Promocode promo, BigDecimal subtotal) {
        BigDecimal amount = promo.getDiscountType() == DiscountType.FIXED
                ? promo.getDiscountValue()
                : subtotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return amount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) throw new BusinessRuleException("Promocode is required");
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private PromocodeResponse response(Promocode promo) {
        return new PromocodeResponse(promo.getId(), promo.getCode(), promo.getDiscountType(),
                promo.getDiscountValue(), promo.getMinimumAmount(), promo.getStartsAt(),
                promo.getExpiresAt(), promo.getMaxUses(), promo.getUsedCount(), promo.isActive());
    }
}
