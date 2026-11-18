package com.marketplace.delivery.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;




@Entity
@Table(name = "delivery_regions")
@Getter
@NoArgsConstructor
public class DeliveryRegion {
    @Id
    @Column(name = "code",length=32, nullable=false)
    private String code;

    @Column(name = "name", length = 100, nullable=false)
    private String name;
}
