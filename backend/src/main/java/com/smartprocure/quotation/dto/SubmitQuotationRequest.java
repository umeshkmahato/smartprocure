package com.smartprocure.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubmitQuotationRequest(

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Total amount must be positive")
        BigDecimal totalAmount,

        @Size(max = 3) String currency,

        @NotNull(message = "Validity date is required")
        @Future(message = "Validity date must be in the future")
        LocalDate validityDate,

        String notes,

        @NotEmpty(message = "At least one line item is required")
        @Valid
        List<QuotationLineItemRequest> items
) {}
