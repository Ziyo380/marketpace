
package com.marketplace.storage.service;

import com.marketplace.storage.config.StorageProperties;
import com.marketplace.storage.dto.StoredObjectResponse;
import com.marketplace.storage.exception.StorageOperationException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "storage.mode", havingValue = "minio", matchIfMissing = true)
public class MinioObjectStorageService implements ObjectStorageService {

    private final StorageProperties properties;
    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;

    public MinioObjectStorageService(StorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .region(properties.getRegion())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        this.publicMinioClient = MinioClient.builder()
                .endpoint(properties.getPublicEndpoint())
                .region(properties.getRegion())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public StoredObjectResponse upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType
    ) {
        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            return new StoredObjectResponse(objectKey, createReadUrl(objectKey));
        } catch (Exception exception) {
            throw new StorageOperationException("Could not upload object to storage", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new StorageOperationException("Could not delete object from storage", exception);
        }
    }

    @Override
    public String createReadUrl(String objectKey) {
        try {
            long expirationSeconds = properties.getReadUrlExpiration().toSeconds();
            int safeExpirationSeconds = Math.toIntExact(Math.min(expirationSeconds, 604_800));
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry(safeExpirationSeconds)
                            .build()
            );
        } catch (Exception exception) {
            throw new StorageOperationException("Could not create object read URL", exception);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build()
            );
        }
    }
}
