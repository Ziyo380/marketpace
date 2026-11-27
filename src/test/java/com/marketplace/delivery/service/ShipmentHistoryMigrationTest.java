package com.marketplace.delivery.service;

import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.*;

class ShipmentHistoryMigrationTest {
    @Test
    void migrationAllowsCreationAndTransitionsButRejectsInvalidEvents() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")) {
            var source = new SingleConnectionDataSource(connection, true);
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE DOMAIN TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
            jdbc.execute("CREATE TABLE shipments (id UUID PRIMARY KEY)");
            UUID shipmentId = UUID.randomUUID();
            jdbc.update("INSERT INTO shipments(id) VALUES (?)", shipmentId);
            new ResourceDatabasePopulator(new ClassPathResource(
                    "db/migration/V34__create_shipment_status_history.sql")).execute(source);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shipment_status_history", Long.class)).isZero();

            insert(jdbc, shipmentId, null, "CREATED", "SYSTEM");
            insert(jdbc, shipmentId, "CREATED", "PACKING", "SELLER");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shipment_status_history", Long.class)).isEqualTo(2);

            assertThatThrownBy(() -> insert(jdbc, shipmentId, null, "PACKING", "SELLER"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insert(jdbc, shipmentId, "PACKING", "CREATED", "SELLER"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insert(jdbc, shipmentId, null, "CREATED", "CUSTOMER"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insert(jdbc, shipmentId, "INVALID", "PACKING", "SELLER"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insert(jdbc, UUID.randomUUID(), null, "CREATED", "SYSTEM"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shipments", Long.class)).isEqualTo(1);
        }
    }

    private void insert(JdbcTemplate jdbc, UUID shipmentId, String from, String to, String actor) {
        jdbc.update("""
                INSERT INTO shipment_status_history
                    (id, shipment_id, from_status, to_status, actor_type, changed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), shipmentId, from, to, actor);
    }
}
