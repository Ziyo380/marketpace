package com.marketplace.promotion.controller;

import com.marketplace.promotion.dto.request.CreatePromocodeRequest;
import com.marketplace.promotion.dto.response.PromocodeResponse;
import com.marketplace.promotion.service.PromocodeService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/promocodes")
@RequiredArgsConstructor
public class AdminPromocodeController {
    private final PromocodeService service;

    @PostMapping
    public ResponseEntity<PromocodeResponse> create(@Valid @RequestBody CreatePromocodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
    @GetMapping
    public Page<PromocodeResponse> list(Pageable pageable) { return service.list(pageable); }
    @PatchMapping("/{id}/deactivate")
    public PromocodeResponse deactivate(@PathVariable UUID id) { return service.deactivate(id); }
}
