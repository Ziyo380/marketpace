package com.marketplace.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.storage.config.StorageProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageServiceTest {
    @TempDir Path directory;

    @Test
    void savesReadsAndDeletesImageWithinStorageDirectory() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setLocalDirectory(directory);
        LocalObjectStorageService service = new LocalObjectStorageService(properties);
        String key = "products/00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002.png";
        byte[] image = new byte[] {1, 2, 3};

        assertThat(service.upload(key, new ByteArrayInputStream(image), image.length, "image/png").url())
                .isEqualTo("/api/v1/storage/objects/" + key);
        assertThat(service.read(key).getContentAsByteArray()).isEqualTo(image);
        service.delete(key);
        assertThatThrownBy(() -> service.read(key)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.read("../outside.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
