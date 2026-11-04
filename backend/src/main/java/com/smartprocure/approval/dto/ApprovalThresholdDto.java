package com.smartprocure.approval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.approval.entity.ApprovalThreshold;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApprovalThresholdDto(
        Long       id,
        Integer    level,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String     roleRequired,
        String     description,
        boolean    active
) {
    public static ApprovalThresholdDto fromEntity(ApprovalThreshold t) {
        return new ApprovalThresholdDto(
                t.getId(), t.getLevel(), t.getMinAmount(), t.getMaxAmount(),
                t.getRoleRequired(), t.getDescription(), t.isActive());
    }
}
