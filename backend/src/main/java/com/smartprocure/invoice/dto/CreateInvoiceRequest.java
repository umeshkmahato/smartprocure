package com.smartprocure.invoice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvoiceRequest(

        @NotNull(message = "Purchase order ID is required")
        Long purchaseOrderId,

        @NotBlank(message = "Invoice number is required")
        @Size(max = 60)
        String invoiceNumber,

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.01", message = "Total amount must be positive")
        BigDecimal totalAmount,

        @Size(max = 3)
        String currency,

        @NotNull(message = "Invoice date is required")
        LocalDate invoiceDate,

        LocalDate dueDate,

        /** URL or storage key to the uploaded invoice document. */
        @Size(max = 500)
        String documentUrl,

        String notes
) {}
