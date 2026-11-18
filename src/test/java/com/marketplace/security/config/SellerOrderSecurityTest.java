package com.marketplace.security.config;

import com.marketplace.order.service.SellerOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerOrderSecurityTest {
    private static final String ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SellerOrderService sellerOrderService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotConfirmOrCancelSellerOrder() throws Exception {
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/confirm", ID))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/cancel", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void sellerCanReachSellerOrderRoutes() throws Exception {
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/confirm", ID))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/seller/orders/{id}/cancel", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/seller/orders/{id}", ID))
                .andExpect(status().isOk());
    }
}
