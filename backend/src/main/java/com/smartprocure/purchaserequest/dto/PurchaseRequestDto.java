package com.smartprocure.purchaserequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.purchaserequest.entity.PurchaseRequestPriority;
import com.smartprocure.purchaserequest.entity.PurchaseRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseRequestDto(
        Long                          id,
        String                        prNumber,
        String                        title,
        String                        description,
        PurchaseRequestStatus         status,
        PurchaseRequestPriority       priority,
        Long                          requesterId,
        String                        requesterName,
        String                        department,
        LocalDate                     requiredByDate,
        BigDecimal                    totalAmount,
        String                        currency,
        String                        rejectionReason,
        String                        notes,
        List<PurchaseRequestItemDto>  items,
        LocalDateTime                 createdAt,
        LocalDateTime                 updatedAt,
        String                        createdBy,
        String                        updatedBy
) {
    public static PurchaseRequestDto fromEntity(PurchaseRequest pr) {
        return new PurchaseRequestDto(
                pr.getId(),
                pr.getPrNumber(),
                pr.getTitle(),
                pr.getDescription(),
                pr.getStatus(),
                pr.getPriority(),
                pr.getRequester() != null ? pr.getRequester().getId()       : null,
                pr.getRequester() != null ? pr.getRequester().getFullName() : null,
                pr.getDepartment(),
                pr.getRequiredByDate(),
                pr.getTotalAmount(),
                pr.getCurrency(),
                pr.getRejectionReason(),
                pr.getNotes(),
                pr.getItems() != null
                        ? pr.getItems().stream().map(PurchaseRequestItemDto::fromEntity).toList()
                        : List.of(),
                pr.getCreatedAt(),
                pr.getUpdatedAt(),
                pr.getCreatedBy(),
                pr.getUpdatedBy()
        );
    }
}
