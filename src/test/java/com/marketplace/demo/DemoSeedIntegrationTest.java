package com.marketplace.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.auth.dto.request.LoginRequest;
import com.marketplace.auth.service.AuthService;
import com.marketplace.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DemoSeedIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ProductService productService;
    @Autowired AuthService authService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void cleanDatabaseCanBeSeededTwiceAndUsedForCheckout() throws Exception {
        var script = new FileSystemResource("scripts/demo-data.sql");
        assertThat(script.exists()).isTrue();
        var populator = new ResourceDatabasePopulator(script);
        populator.execute(dataSource);
        populator.execute(dataSource);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE price > 0 AND status = 'PUBLISHED'", Integer.class)).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventories WHERE quantity > 0 AND reserved_quantity = 0", Integer.class)).isEqualTo(12);

        var catalog = productService.getPublicProducts(null, null, null, null, PageRequest.of(0, 20));
        assertThat(catalog.getTotalElements()).isEqualTo(12);
        assertThat(catalog.getContent()).allSatisfy(product -> assertThat(product.price()).isPositive());
        assertThat(authService.login(new LoginRequest("demo-customer", "Customer123!")).accessToken()).isNotBlank();

        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo-customer\",\"password\":\"Customer123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).path("accessToken").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/v1/products?size=20"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"50000000-0000-0000-0000-000000000001\",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        String orderJson = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "clean-demo-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delivery\":{\"recipientName\":\"Demo Buyer\",\"recipientPhone\":\"+998901234567\",\"region\":\"TASHKENT_CITY\",\"city\":\"Tashkent\",\"addressLine\":\"Amir Temur 10\"}}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderJson).path("orderId").asText();
        assertThat(objectMapper.readTree(orderJson).path("status").asText()).isEqualTo("PENDING");
        assertThat(objectMapper.readTree(orderJson).path("delivery").path("recipientName").asText())
                .isEqualTo("Demo Buyer");
        assertThat(orderId).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reserved_quantity FROM inventories WHERE product_id = ?", Integer.class,
                java.util.UUID.fromString("50000000-0000-0000-0000-000000000001"))).isEqualTo(1);

        mockMvc.perform(patch("/api/v1/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reserved_quantity FROM inventories WHERE product_id = ?", Integer.class,
                java.util.UUID.fromString("50000000-0000-0000-0000-000000000001"))).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM stock_reservations", String.class)).isEqualTo("RELEASED");
    }
}
