package com.marketplace.delivery.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "shipment_status_history")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentStatusHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private ShipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private ShipmentStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ShipmentActorType actorType;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "details", length = 255)
    private String details;
}
