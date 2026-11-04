package com.smartprocure.quotation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.quotation.entity.QuotationRequest;
import com.smartprocure.quotation.entity.QuotationRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuotationRequestDto(
        Long id, String rfqNumber, Long purchaseRequestId,
        String title, String description,
        QuotationRequestStatus status, LocalDateTime deadline,
        String currency, String notes,
        List<Long> invitedVendorIds, List<String> invitedVendorNames,
        List<QuotationDto> quotations,
        LocalDateTime createdAt, LocalDateTime updatedAt,
        String createdBy, String updatedBy
) {
    public static QuotationRequestDto fromEntity(QuotationRequest r) {
        return new QuotationRequestDto(
                r.getId(), r.getRfqNumber(),
                r.getPurchaseRequest() != null ? r.getPurchaseRequest().getId() : null,
                r.getTitle(), r.getDescription(), r.getStatus(),
                r.getDeadline(), r.getCurrency(), r.getNotes(),
                r.getInvitedVendors().stream().map(v -> v.getId()).toList(),
                r.getInvitedVendors().stream().map(v -> v.getName()).toList(),
                r.getQuotations().stream().map(QuotationDto::fromEntity).toList(),
                r.getCreatedAt(), r.getUpdatedAt(),
                r.getCreatedBy(), r.getUpdatedBy());
    }
}
