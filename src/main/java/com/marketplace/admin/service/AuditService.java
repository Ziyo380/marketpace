package com.marketplace.admin.service;

import com.marketplace.admin.dto.response.AuditLogResponse;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.repository.AuditLogRepository;
import com.marketplace.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User actor, String action, String targetType, UUID targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(log ->
                new AuditLogResponse(log.getId(), log.getActor().getUsername(), log.getAction(),
                        log.getTargetType(), log.getTargetId(), log.getDetails(), log.getCreatedAt()));
    }
}
