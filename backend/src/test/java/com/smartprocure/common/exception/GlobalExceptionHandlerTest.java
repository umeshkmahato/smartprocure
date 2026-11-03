package com.smartprocure.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * Uses MockMvc standalone setup so no Spring context is required.
 * A {@link LocalValidatorFactoryBean} is wired in to enable {@code @Valid} processing.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    // ── Stub controller that throws each type of exception ─────────────────
    @RestController
    static class StubController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Purchase Request", "id", 99L);
        }

        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(ErrorCode.VENDOR_NOT_APPROVED);
        }

        @GetMapping("/test/unauthorized")
        String unauthorized() {
            throw new UnauthorizedException(ErrorCode.FORBIDDEN, "Admin access required");
        }

        @GetMapping("/test/server-error")
        String serverError() {
            throw new RuntimeException("Unexpected failure");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody ValidRequest body) {
            return "ok";
        }

        // Simple DTO with constraints to trigger MethodArgumentNotValidException
        static class ValidRequest {
            @NotBlank  private String  name;
            @NotNull @Positive private Integer quantity;

            public String  getName()    { return name; }
            public void    setName(String n)  { this.name = n; }
            public Integer getQuantity()      { return quantity; }
            public void    setQuantity(Integer q) { this.quantity = q; }
        }
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ── Tests ──────────────────────────────────────────────────────────────

    @Test @DisplayName("ResourceNotFoundException → 404 RESOURCE_NOT_FOUND")
    void resourceNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test @DisplayName("BusinessException → status from ErrorCode")
    void businessException_mapsToErrorCodeStatus() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())   // VENDOR_NOT_APPROVED → 400
                .andExpect(jsonPath("$.errorCode").value("VENDOR_NOT_APPROVED"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test @DisplayName("UnauthorizedException → status from ErrorCode (403)")
    void unauthorizedException_mapsToForbidden() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test @DisplayName("Generic Exception → 500 INTERNAL_SERVER_ERROR")
    void genericException_returns500() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test @DisplayName("@Valid failure → 400 with field errors array")
    void validationFailure_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))                  // empty body → 2 constraint violations
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test @DisplayName("Malformed JSON body → 400 VALIDATION_ERROR")
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
