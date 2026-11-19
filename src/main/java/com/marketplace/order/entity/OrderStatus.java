package com.marketplace.order.entity;

public enum OrderStatus  {
    PENDING,
    PROCESSING,
    CONFIRMED,
    IN_DELIVERY,
    PARTIALLY_DELIVERED,
    DELIVERED,
    DELIVERY_FAILED,
    PARTIALLY_CANCELLED,
    CANCELLED,
}
