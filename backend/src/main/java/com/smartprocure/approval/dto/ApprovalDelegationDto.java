package com.smartprocure.approval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.approval.entity.ApprovalDelegation;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApprovalDelegationDto(
        Long          id,
        Long          delegatorId,
        String        delegatorName,
        Long          delegateId,
        String        delegateName,
        LocalDate     startDate,
        LocalDate     endDate,
        String        reason,
        boolean       active,
        LocalDateTime createdAt
) {
    public static ApprovalDelegationDto fromEntity(ApprovalDelegation d) {
        return new ApprovalDelegationDto(
                d.getId(),
                d.getDelegator() != null ? d.getDelegator().getId()       : null,
                d.getDelegator() != null ? d.getDelegator().getFullName() : null,
                d.getDelegate()  != null ? d.getDelegate().getId()        : null,
                d.getDelegate()  != null ? d.getDelegate().getFullName()  : null,
                d.getStartDate(), d.getEndDate(),
                d.getReason(), d.isActive(), d.getCreatedAt());
    }
}
