package com.smartprocure.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse factory methods")
class ApiResponseTest {

    @Test
    @DisplayName("success(data) – fields set correctly")
    void successWithData_setsCorrectFields() {
        ApiResponse<String> r = ApiResponse.success("payload");
        assertThat(r.success()).isTrue();
        assertThat(r.data()).isEqualTo("payload");
        assertThat(r.message()).isEqualTo("Success");
        assertThat(r.timestamp()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("success(data, message) – custom message used")
    void successWithCustomMessage_usesSuppliedMessage() {
        ApiResponse<Integer> r = ApiResponse.success(42, "Found it");
        assertThat(r.success()).isTrue();
        assertThat(r.data()).isEqualTo(42);
        assertThat(r.message()).isEqualTo("Found it");
    }

    @Test
    @DisplayName("success(message) – data is null")
    void successMessageOnly_hasNullData() {
        ApiResponse<Void> r = ApiResponse.success("Done");
        assertThat(r.success()).isTrue();
        assertThat(r.data()).isNull();
        assertThat(r.message()).isEqualTo("Done");
    }

    @Test
    @DisplayName("created(data) – standard creation message")
    void created_setsCreationMessage() {
        ApiResponse<String> r = ApiResponse.created("entity-1");
        assertThat(r.success()).isTrue();
        assertThat(r.data()).isEqualTo("entity-1");
        assertThat(r.message()).isEqualTo("Resource created successfully");
    }

    @Test
    @DisplayName("created(data, message) – custom creation message")
    void created_withCustomMessage_usesIt() {
        ApiResponse<Long> r = ApiResponse.created(99L, "Vendor created");
        assertThat(r.message()).isEqualTo("Vendor created");
    }

    @Test
    @DisplayName("timestamp is set at call time (within 2 seconds)")
    void timestamp_isRecent() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ApiResponse<String> r = ApiResponse.success("x");
        LocalDateTime after  = LocalDateTime.now().plusSeconds(1);
        assertThat(r.timestamp()).isBetween(before, after);
    }
}
