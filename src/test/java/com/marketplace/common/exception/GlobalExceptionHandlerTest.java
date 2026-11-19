package com.marketplace.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marketplace.common.util.ClockProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-15T10:30:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        GlobalExceptionHandler exceptionHandler =
                new GlobalExceptionHandler(new ClockProvider(clock));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(exceptionHandler)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsStableNotFoundResponse() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").value("2026-01-15T10:30:00Z"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product with identifier '123' was not found"))
                .andExpect(jsonPath("$.path").value("/test/missing"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void returnsConflictResponse() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Resource already exists"));
    }

    @Test
    void returnsStableOptimisticLockConflictResponse() throws Exception {
        mockMvc.perform(get("/test/optimistic-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").value("2026-01-15T10:30:00Z"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(
                        "Resource was modified by another request. Please refresh and try again"
                ))
                .andExpect(jsonPath("$.path").value("/test/optimistic-conflict"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void returnsFieldValidationErrorsWithoutRejectedValue() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("name must not be blank"))
                .andExpect(jsonPath("$.validationErrors[0].rejectedValue").doesNotExist());
    }

    @Test
    void doesNotExposeUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void returnsForbiddenWithoutExposingAccessDeniedDetails() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/test/forbidden"))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/forbidden")
        String forbidden() {
            throw new org.springframework.security.access.AccessDeniedException("Private ownership details");
        }

        @GetMapping("/missing")
        String missing() {
            throw new ResourceNotFoundException("Product", 123);
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ConflictException("Resource already exists");
        }

        @GetMapping("/optimistic-conflict")
        String optimisticConflict() {
            throw new OptimisticLockingFailureException("Concurrent modification");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException("Sensitive internal details");
        }
    }

    record TestRequest(@NotBlank(message = "name must not be blank") String name) {
    }
}
