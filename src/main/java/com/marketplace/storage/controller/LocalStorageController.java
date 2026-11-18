package com.marketplace.storage.controller;

import com.marketplace.storage.service.LocalObjectStorageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/storage/objects")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.mode", havingValue = "local")
public class LocalStorageController {
    private final LocalObjectStorageService storage;

    @GetMapping("/products/{productId}/{filename}")
    public ResponseEntity<Resource> read(@PathVariable UUID productId, @PathVariable String filename) {
        String key = "products/" + productId + "/" + filename;
        MediaType contentType = filename.endsWith(".jpg") ? MediaType.IMAGE_JPEG
                : filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : MediaType.parseMediaType("image/webp");
        return ResponseEntity.ok().contentType(contentType)
                .cacheControl(CacheControl.noCache()).body(storage.read(key));
    }
}
