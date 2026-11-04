package com.smartprocure.approval.dto;

import jakarta.validation.constraints.NotNull;

public record ActionApprovalRequest(

        @NotNull(message = "Action is required (APPROVED or REJECTED)")
        ApprovalAction action,

        String comments
) {
    public enum ApprovalAction { APPROVED, REJECTED }
}
