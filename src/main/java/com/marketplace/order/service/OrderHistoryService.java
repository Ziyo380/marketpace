package com.marketplace.order.service;

import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderStatusHistory;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.repository.OrderStatusHistoryRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {
    private final OrderStatusHistoryRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Order order, SellerOrder part, String previous, String next,
                       String actor, String reason) {
        if (Objects.equals(previous, next)) return;
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setSellerOrder(part);
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setActor(actor);
        history.setReason(reason);
        repository.save(history);
    }
}
