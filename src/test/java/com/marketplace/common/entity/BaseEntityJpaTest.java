package com.marketplace.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.config.JpaConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import testfixture.AuditedEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.jpa.repositories.enabled=false"
})
@ActiveProfiles("test")
@EntityScan(basePackageClasses = AuditedEntity.class)
@Import({JpaConfig.class, BaseEntityJpaTest.FixedClockConfiguration.class})
class BaseEntityJpaTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-15T10:30:00Z");

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void populatesIdentifierAndAuditTimestamps() {
        AuditedEntity entity = new AuditedEntity("draft");

        entityManager.persistAndFlush(entity);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(entity.getUpdatedAt()).isEqualTo(FIXED_INSTANT);
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }
}
