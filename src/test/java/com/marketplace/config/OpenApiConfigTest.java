package com.marketplace.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void definesApiInformationAndBearerScheme() {
        OpenAPI openAPI = new OpenApiConfig().marketplaceOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Marketplace Backend API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.BEARER_AUTH_SCHEME);

        SecurityScheme scheme = openAPI.getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfig.BEARER_AUTH_SCHEME);
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }
}
