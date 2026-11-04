package com.smartprocure.purchaserequest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.purchaserequest.entity.PurchaseRequestItem;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseRequestItemDto(
        Long       id,
        String     itemName,
        String     description,
        BigDecimal quantity,
        String     unit,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Long       preferredVendorId,
        String     preferredVendorName,
        String     notes
) {
    public static PurchaseRequestItemDto fromEntity(PurchaseRequestItem i) {
        return new PurchaseRequestItemDto(
                i.getId(),
                i.getItemName(),
                i.getDescription(),
                i.getQuantity(),
                i.getUnit(),
                i.getUnitPrice(),
                i.getTotalPrice(),
                i.getPreferredVendor() != null ? i.getPreferredVendor().getId()   : null,
                i.getPreferredVendor() != null ? i.getPreferredVendor().getName() : null,
                i.getNotes()
        );
    }
}
