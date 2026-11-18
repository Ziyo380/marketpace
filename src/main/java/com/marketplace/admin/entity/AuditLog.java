package com.marketplace.admin.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;
    @Column(name = "action", nullable = false, length = 40)
    private String action;
    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;
    @Column(name = "target_id", nullable = false)
    private UUID targetId;
    @Column(name = "details", length = 1000)
    private String details;
}
