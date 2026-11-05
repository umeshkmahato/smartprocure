package com.smartprocure.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeInvoiceRequest(

        @NotBlank(message = "Dispute reason is required")
        @Size(max = 1000)
        String reason
) {}
