package testfixture;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audited_entity_test")
public class AuditedEntity extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    protected AuditedEntity() {
    }

    public AuditedEntity(String name) {
        this.name = name;
    }
}
