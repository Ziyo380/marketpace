package com.marketplace.admin.dto.response;

public record PlatformStatisticsResponse(long users, long sellers, long stores,
        long products, long orders, long confirmedOrders, long reviews) {}
