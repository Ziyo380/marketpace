package com.marketplace.order.service;

import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationJob {
    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;

    @Scheduled(fixedDelayString = "${order.expiration-check-ms:60000}")
    public void releaseExpiredReservations() {
        List<java.util.UUID> ids = orderRepository.findExpiredIds(Instant.now(),
                List.of(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.PARTIALLY_CANCELLED),
                SellerOrderStatus.PENDING);
        for (var id : ids) {
            try {
                cancellationService.expireOrder(id);
            } catch (RuntimeException e) {
                log.error("Could not expire order {}", id, e);
            }
        }
    }
}
