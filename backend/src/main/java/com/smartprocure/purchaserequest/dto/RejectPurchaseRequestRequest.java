package com.smartprocure.purchaserequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPurchaseRequestRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String reason
) {}
