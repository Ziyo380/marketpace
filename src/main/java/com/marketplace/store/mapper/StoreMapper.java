package com.marketplace.store.mapper;

import com.marketplace.store.dto.response.StoreResponse;
import com.marketplace.store.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {
    public StoreResponse toResponse(Store store) {
        StoreResponse response=new StoreResponse(
                store.getId(),
                store.getSeller().getId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.isActive(),
                store.isApproved(),
                store.getRejectionReason()
        );
        return response;
    }
}
