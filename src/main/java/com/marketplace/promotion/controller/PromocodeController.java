package com.marketplace.promotion.controller;

import com.marketplace.promotion.dto.response.DiscountResponse;
import com.marketplace.promotion.service.PromocodeService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/promocodes")
@RequiredArgsConstructor
public class PromocodeController {
    private final PromocodeService service;

    @GetMapping("/preview")
    public DiscountResponse preview(@RequestParam String code, @RequestParam BigDecimal subtotal,
            Authentication authentication) {
        return service.previewForUser(authentication.getName(), code, subtotal);
    }
}
