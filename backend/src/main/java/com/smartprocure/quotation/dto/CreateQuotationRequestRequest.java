package com.smartprocure.quotation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateQuotationRequestRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        String description,

        Long purchaseRequestId,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDateTime deadline,

        @Size(max = 3)
        String currency,

        String notes,

        @NotEmpty(message = "At least one vendor must be invited")
        List<Long> vendorIds
) {}
