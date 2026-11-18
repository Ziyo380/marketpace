package com.marketplace.storage.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;

class MinioObjectStorageServiceTest {
    @Test
    void signsPublicUrlWithoutContactingPublicEndpointFromBackend() {
        var properties = new StorageProperties();
        properties.setEndpoint("http://127.0.0.1:1");
        properties.setPublicEndpoint("http://127.0.0.1:2");
        properties.setRegion("us-east-1");

        String url = new MinioObjectStorageService(properties)
                .createReadUrl("products/00000000-0000-0000-0000-000000000001/image.png");

        assertThat(url).startsWith("http://127.0.0.1:2/marketplace-products/products/");
        assertThat(url).contains("X-Amz-Signature=");
    }
}
