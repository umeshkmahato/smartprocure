package com.smartprocure.quotation.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record QuotationLineItemRequest(

        @NotBlank(message = "Item name is required")
        @Size(max = 255)
        String itemName,

        String description,

        @NotNull @DecimalMin(value = "0.0001")
        BigDecimal quantity,

        @Size(max = 50) String unit,

        @NotNull @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal unitPrice,

        String notes
) {}
