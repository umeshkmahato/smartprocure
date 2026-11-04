package com.smartprocure.quotation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.quotation.entity.Quotation;
import com.smartprocure.quotation.entity.QuotationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuotationDto(
        Long id, Long quotationRequestId, String rfqNumber,
        Long vendorId, String vendorName,
        QuotationStatus status, BigDecimal totalAmount, String currency,
        LocalDate validityDate, String notes, boolean selected,
        List<QuotationItemDto> items,
        LocalDateTime submittedAt, LocalDateTime reviewedAt
) {
    public static QuotationDto fromEntity(Quotation q) {
        return new QuotationDto(
                q.getId(),
                q.getQuotationRequest() != null ? q.getQuotationRequest().getId()       : null,
                q.getQuotationRequest() != null ? q.getQuotationRequest().getRfqNumber(): null,
                q.getVendor() != null ? q.getVendor().getId()   : null,
                q.getVendor() != null ? q.getVendor().getName() : null,
                q.getStatus(), q.getTotalAmount(), q.getCurrency(),
                q.getValidityDate(), q.getNotes(), q.isSelected(),
                q.getItems().stream().map(QuotationItemDto::fromEntity).toList(),
                q.getSubmittedAt(), q.getReviewedAt());
    }
}
