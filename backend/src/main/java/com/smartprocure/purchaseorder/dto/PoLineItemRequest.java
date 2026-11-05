package com.smartprocure.purchaseorder.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PoLineItemRequest(

        @NotBlank(message = "Item name is required")
        @Size(max = 255)
        String itemName,

        String description,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be positive")
        BigDecimal quantity,

        @Size(max = 50) String unit,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
        BigDecimal unitPrice,

        String notes
) {}
