package com.marketplace.storage.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "storage_deletion_tasks")
@Getter
@NoArgsConstructor
public class StorageDeletionTask extends BaseEntity {
    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    private String objectKey;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Version
    private Long version;

    public StorageDeletionTask(String objectKey) {
        this.objectKey = objectKey;
        this.nextAttemptAt = Instant.now();
    }

    public void retryLater() {
        if (attempts < Integer.MAX_VALUE) attempts++;
        long delaySeconds = Math.min(3600, 60L << Math.min(attempts - 1, 6));
        nextAttemptAt = Instant.now().plusSeconds(delaySeconds);
    }
}
