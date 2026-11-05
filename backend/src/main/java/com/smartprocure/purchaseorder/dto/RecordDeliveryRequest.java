package com.smartprocure.purchaseorder.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record RecordDeliveryRequest(

        @NotEmpty(message = "At least one delivery line is required")
        List<DeliveryLine> lines
) {
    public record DeliveryLine(

            @NotNull(message = "Item ID is required")
            Long itemId,

            @NotNull(message = "Delivered quantity is required")
            @DecimalMin(value = "0.0001", message = "Delivered quantity must be positive")
            BigDecimal deliveredQty
    ) {}
}
