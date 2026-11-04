package com.smartprocure.approval.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDelegationRequest(

        @NotNull(message = "Delegate user ID is required")
        Long delegateId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        LocalDate endDate,

        String reason
) {}
