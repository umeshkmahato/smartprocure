package com.smartprocure.purchaserequest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LineItemRequest(

        @NotBlank(message = "Item name is required")
        @Size(max = 255)
        String itemName,

        String description,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @Size(max = 50)
        String unit,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Unit price must be non-negative")
        BigDecimal unitPrice,

        Long preferredVendorId,

        String notes
) {}
