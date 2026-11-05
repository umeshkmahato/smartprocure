package com.smartprocure.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/ai/recommend.
 */
public record RecommendRequest(
        @NotBlank(message = "Query must not be blank")
        @Size(max = 1000, message = "Query must not exceed 1000 characters")
        String query,

        @Min(1) @Max(20)
        Integer topK,

        String filterStatus
) {}
