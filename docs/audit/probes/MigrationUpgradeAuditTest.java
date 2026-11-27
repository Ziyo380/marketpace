package com.marketplace.audit;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MigrationUpgradeAuditTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test void upgradesHistoricalOrdersFromV30WithoutInventingDiscountAllocations() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("30").load().migrate();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement sql = connection.createStatement()) {
            UUID user = UUID.randomUUID();
            sql.execute("INSERT INTO users(id,username,password_hash,created_at,updated_at) VALUES ('"+user+"','upgrade-audit','hash',now(),now())");
            UUID[] storeIds = {UUID.randomUUID(), UUID.randomUUID()};
            for (UUID store : storeIds) {
                UUID sellerUser = UUID.randomUUID();
                UUID seller = UUID.randomUUID();
                sql.execute("INSERT INTO users(id,username,password_hash,created_at,updated_at) VALUES ('"+sellerUser+"','"+sellerUser+"','hash',now(),now())");
                sql.execute("INSERT INTO sellers(id,user_id,status,created_at,updated_at) VALUES ('"+seller+"','"+sellerUser+"','APPROVED',now(),now())");
                sql.execute("INSERT INTO stores(id,seller_id,name,slug,created_at,updated_at) VALUES ('"+store+"','"+seller+"','Audit','"+store+"',now(),now())");
            }
            for (String status : new String[]{"PENDING", "PARTIALLY_CANCELLED", "CANCELLED"}) {
                UUID order = UUID.randomUUID();
                sql.execute("INSERT INTO orders(id,buyer_id,status,total_amount,subtotal_amount,discount_amount,created_at,updated_at) VALUES ('"+order+"','"+user+"','"+status+"',900,1000,100,now(),now())");
                for (int i=0; i<2; i++) {
                    String partStatus = status.equals("CANCELLED") || (status.equals("PARTIALLY_CANCELLED") && i==0) ? "CANCELLED" : "PENDING";
                    sql.execute("INSERT INTO seller_orders(id,order_id,store_id,status,total_amount,created_at,updated_at) VALUES ('"+UUID.randomUUID()+"','"+order+"','"+storeIds[i]+"','"+partStatus+"',500,now(),now())");
                }
            }
            Flyway latest = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(7);
            latest.validate();
            try (ResultSet rows = sql.executeQuery("SELECT status,total_amount,discount_amount,delivery_total_amount,current_total_amount FROM orders")) {
                int count=0;
                while (rows.next()) {
                    count++;
                    assertThat(rows.getBigDecimal("total_amount")).isEqualByComparingTo("900");
                    assertThat(rows.getBigDecimal("discount_amount")).isEqualByComparingTo("100");
                    assertThat(rows.getBigDecimal("delivery_total_amount")).isEqualByComparingTo("0");
                    switch (rows.getString("status")) {
                        case "PARTIALLY_CANCELLED" -> assertThat(rows.getBigDecimal("current_total_amount")).isNull();
                        case "CANCELLED" -> assertThat(rows.getBigDecimal("current_total_amount")).isEqualByComparingTo("0");
                        default -> assertThat(rows.getBigDecimal("current_total_amount")).isEqualByComparingTo("900");
                    }
                }
                assertThat(count).isEqualTo(3);
            }
            try (ResultSet rows = sql.executeQuery("SELECT count(*) FROM seller_orders WHERE discount_amount IS NULL AND payable_amount IS NULL")) {
                rows.next(); assertThat(rows.getInt(1)).isEqualTo(6);
            }
        }
    }
}
