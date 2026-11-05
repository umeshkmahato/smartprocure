package com.smartprocure.purchaseorder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.purchaseorder.entity.PurchaseOrder;
import com.smartprocure.purchaseorder.entity.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseOrderDto(
        Long id, String poNumber,
        Long purchaseRequestId, Long quotationId,
        Long vendorId, String vendorName,
        PurchaseOrderStatus status,
        Long issuedById, String issuedByName,
        BigDecimal totalAmount, String currency,
        LocalDate deliveryDate, String deliveryAddress,
        String paymentTerms, String notes,
        LocalDateTime issuedAt, LocalDateTime acknowledgedAt, LocalDateTime deliveredAt,
        boolean fullyDelivered,
        List<PurchaseOrderItemDto> items,
        LocalDateTime createdAt, LocalDateTime updatedAt,
        String createdBy, String updatedBy
) {
    public static PurchaseOrderDto fromEntity(PurchaseOrder po) {
        return new PurchaseOrderDto(
                po.getId(), po.getPoNumber(),
                po.getPurchaseRequest() != null ? po.getPurchaseRequest().getId() : null,
                po.getQuotation()       != null ? po.getQuotation().getId()       : null,
                po.getVendor()          != null ? po.getVendor().getId()          : null,
                po.getVendor()          != null ? po.getVendor().getName()        : null,
                po.getStatus(),
                po.getIssuedBy()        != null ? po.getIssuedBy().getId()        : null,
                po.getIssuedBy()        != null ? po.getIssuedBy().getFullName()  : null,
                po.getTotalAmount(), po.getCurrency(),
                po.getDeliveryDate(), po.getDeliveryAddress(),
                po.getPaymentTerms(), po.getNotes(),
                po.getIssuedAt(), po.getAcknowledgedAt(), po.getDeliveredAt(),
                po.isFullyDelivered(),
                po.getItems().stream().map(PurchaseOrderItemDto::fromEntity).toList(),
                po.getCreatedAt(), po.getUpdatedAt(),
                po.getCreatedBy(), po.getUpdatedBy());
    }
}
