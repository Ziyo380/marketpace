package com.marketplace.seller.mapper;

import com.marketplace.seller.dto.response.SellerResponse;
import com.marketplace.seller.entity.Seller;
import org.springframework.stereotype.Component;

@Component
public class SellerMapper {
    public SellerResponse toResponse(Seller seller){
        SellerResponse response=new SellerResponse(
                seller.getId(),
                seller.getUser().getId(),
                seller.getStatus(),
                seller.getApprovedAt(),
                seller.getRejectionReason()
        );
        return response;

    }
}
