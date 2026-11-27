package com.marketplace.storage.service;

import com.marketplace.storage.entity.StorageDeletionTask;
import com.marketplace.storage.repository.StorageDeletionTaskRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCleanupService {
    private final StorageDeletionTaskRepository tasks;
    private final StorageDeletionProcessor processor;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAfterCommit(String objectKey) {
        UUID taskId = tasks.save(new StorageDeletionTask(objectKey)).getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                processQuietly(taskId);
            }
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAfterRollback(String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) return;
                try {
                    processor.cleanRolledBackUpload(objectKey);
                } catch (RuntimeException exception) {
                    log.error("Could not schedule cleanup for rolled-back upload {}", objectKey, exception);
                }
            }
        });
    }

    @Scheduled(fixedDelayString = "${storage.cleanup-check-ms:60000}",
            initialDelayString = "${storage.cleanup-check-ms:60000}")
    public void retryPendingDeletions() {
        tasks.findDueIds(Instant.now(), PageRequest.of(0, 50)).forEach(this::processQuietly);
    }

    private void processQuietly(UUID taskId) {
        try {
            processor.process(taskId);
        } catch (RuntimeException exception) {
            // The committed task survives, including when the processor's transaction fails.
            log.error("Storage deletion task {} remains pending", taskId, exception);
        }
    }
}
