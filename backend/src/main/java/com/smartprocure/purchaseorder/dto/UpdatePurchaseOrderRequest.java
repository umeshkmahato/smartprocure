package com.smartprocure.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdatePurchaseOrderRequest(

        @Size(max = 3) String currency,
        LocalDate deliveryDate,
        String deliveryAddress,
        String paymentTerms,
        String notes,

        @Valid List<PoLineItemRequest> items
) {}
