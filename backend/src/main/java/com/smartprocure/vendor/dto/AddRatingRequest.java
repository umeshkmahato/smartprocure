package com.smartprocure.vendor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for submitting a performance rating for a vendor.
 */
public record AddRatingRequest(

        @NotNull(message = "Score is required")
        @Min(value = 1, message = "Score must be at least 1")
        @Max(value = 5, message = "Score must not exceed 5")
        Integer score,

        String comment
) {}
