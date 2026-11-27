package com.marketplace.storage.service;

import com.marketplace.storage.entity.StorageDeletionTask;
import com.marketplace.storage.repository.StorageDeletionTaskRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDeletionProcessor {
    private final StorageDeletionTaskRepository tasks;
    private final ObjectStorageService storage;

    // Called after the image transaction finishes; a separate transaction is required.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID taskId) {
        tasks.findByIdForUpdate(taskId)
                .filter(task -> !task.getNextAttemptAt().isAfter(Instant.now()))
                .ifPresent(this::deleteObject);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanRolledBackUpload(String objectKey) {
        StorageDeletionTask task = tasks.findByObjectKey(objectKey)
                .orElseGet(() -> tasks.save(new StorageDeletionTask(objectKey)));
        deleteObject(task);
    }

    private void deleteObject(StorageDeletionTask task) {
        try {
            storage.delete(task.getObjectKey());
        } catch (RuntimeException exception) {
            task.retryLater();
            log.warn("Storage deletion {} failed; retry {} scheduled", task.getId(), task.getAttempts(), exception);
            return;
        }
        // Object deletion is idempotent if a later DB failure leaves the task to retry.
        tasks.delete(task);
    }
}
