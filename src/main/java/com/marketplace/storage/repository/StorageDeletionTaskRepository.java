package com.marketplace.storage.repository;

import com.marketplace.storage.entity.StorageDeletionTask;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorageDeletionTaskRepository extends JpaRepository<StorageDeletionTask, UUID> {
    Optional<StorageDeletionTask> findByObjectKey(String objectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from StorageDeletionTask t where t.id = :id")
    Optional<StorageDeletionTask> findByIdForUpdate(@Param("id") UUID id);

    @Query("select t.id from StorageDeletionTask t where t.nextAttemptAt <= :now order by t.nextAttemptAt, t.id")
    List<UUID> findDueIds(@Param("now") Instant now, Pageable pageable);
}
