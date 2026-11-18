package com.marketplace.storage.service;

import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.storage.config.StorageProperties;
import com.marketplace.storage.dto.StoredObjectResponse;
import com.marketplace.storage.exception.StorageOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.mode", havingValue = "local")
public class LocalObjectStorageService implements ObjectStorageService {
    private static final Pattern KEY = Pattern.compile(
            "products/[0-9a-f-]{36}/[0-9a-f-]{36}\\.(jpg|png|webp)");
    private final StorageProperties properties;

    @Override
    public StoredObjectResponse upload(String objectKey, InputStream inputStream, long size, String contentType) {
        Path target = path(objectKey);
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                inputStream.transferTo(output);
            }
            return new StoredObjectResponse(objectKey, createReadUrl(objectKey));
        } catch (IOException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { /* Original failure is preserved. */ }
            throw new StorageOperationException("Could not save uploaded image", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try { Files.deleteIfExists(path(objectKey)); }
        catch (IOException exception) { throw new StorageOperationException("Could not delete uploaded image", exception); }
    }

    @Override
    public String createReadUrl(String objectKey) {
        path(objectKey);
        return "/api/v1/storage/objects/" + objectKey;
    }

    public Resource read(String objectKey) {
        Path target = path(objectKey);
        if (!Files.isRegularFile(target)) throw new ResourceNotFoundException("Image not found");
        return new FileSystemResource(target);
    }

    private Path path(String objectKey) {
        if (objectKey == null || !KEY.matcher(objectKey).matches()) {
            throw new ResourceNotFoundException("Image not found");
        }
        Path root = properties.getLocalDirectory().toAbsolutePath().normalize();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) throw new ResourceNotFoundException("Image not found");
        return target;
    }
}
