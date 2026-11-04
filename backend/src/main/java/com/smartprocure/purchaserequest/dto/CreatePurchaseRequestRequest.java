package com.smartprocure.purchaserequest.dto;

import com.smartprocure.purchaserequest.entity.PurchaseRequestPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseRequestRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        String description,

        PurchaseRequestPriority priority,

        @Size(max = 100)
        String department,

        LocalDate requiredByDate,

        @Size(max = 3)
        String currency,

        String notes,

        @NotEmpty(message = "At least one line item is required")
        @Valid
        List<LineItemRequest> items
) {}
