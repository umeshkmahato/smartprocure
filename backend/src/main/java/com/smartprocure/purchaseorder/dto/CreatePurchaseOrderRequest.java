package com.smartprocure.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(

        @NotNull(message = "Vendor ID is required")
        Long vendorId,

        Long purchaseRequestId,
        Long quotationId,

        @Size(max = 3) String currency,
        LocalDate deliveryDate,
        String deliveryAddress,
        String paymentTerms,
        String notes,

        @NotEmpty(message = "At least one line item is required")
        @Valid List<PoLineItemRequest> items
) {}
