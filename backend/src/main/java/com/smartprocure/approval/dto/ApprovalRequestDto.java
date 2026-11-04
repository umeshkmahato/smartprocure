package com.smartprocure.approval.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.approval.entity.ApprovalRequest;
import com.smartprocure.approval.entity.ApprovalStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApprovalRequestDto(
        Long            id,
        Long            purchaseRequestId,
        String          purchaseRequestTitle,
        String          prNumber,
        Integer         level,
        Long            approverId,
        String          approverName,
        ApprovalStatus  status,
        String          comments,
        LocalDateTime   actionedAt,
        Long            delegatedToId,
        String          delegatedToName,
        LocalDateTime   createdAt
) {
    public static ApprovalRequestDto fromEntity(ApprovalRequest ar) {
        return new ApprovalRequestDto(
                ar.getId(),
                ar.getPurchaseRequest() != null ? ar.getPurchaseRequest().getId()       : null,
                ar.getPurchaseRequest() != null ? ar.getPurchaseRequest().getTitle()    : null,
                ar.getPurchaseRequest() != null ? ar.getPurchaseRequest().getPrNumber() : null,
                ar.getLevel(),
                ar.getApprover()     != null ? ar.getApprover().getId()          : null,
                ar.getApprover()     != null ? ar.getApprover().getFullName()    : null,
                ar.getStatus(),
                ar.getComments(),
                ar.getActionedAt(),
                ar.getDelegatedTo()  != null ? ar.getDelegatedTo().getId()       : null,
                ar.getDelegatedTo()  != null ? ar.getDelegatedTo().getFullName() : null,
                ar.getCreatedAt()
        );
    }
}
