
package com.marketplace.storage.service;

import com.marketplace.storage.dto.StoredObjectResponse;

import java.io.InputStream;

public interface ObjectStorageService {

    StoredObjectResponse upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType
    );

    void delete(String objectKey);

    String createReadUrl(String objectKey);
}
