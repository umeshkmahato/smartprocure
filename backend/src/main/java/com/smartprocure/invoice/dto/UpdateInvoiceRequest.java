package com.smartprocure.invoice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInvoiceRequest(

        @DecimalMin(value = "0.01", message = "Total amount must be positive")
        BigDecimal totalAmount,

        @Size(max = 3) String currency,

        LocalDate invoiceDate,
        LocalDate dueDate,

        @Size(max = 500) String documentUrl,

        String notes
) {}
