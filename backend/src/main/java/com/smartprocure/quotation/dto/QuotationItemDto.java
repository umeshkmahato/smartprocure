package com.smartprocure.quotation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.quotation.entity.QuotationItem;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuotationItemDto(
        Long id, String itemName, String description,
        BigDecimal quantity, String unit,
        BigDecimal unitPrice, BigDecimal totalPrice, String notes
) {
    public static QuotationItemDto fromEntity(QuotationItem i) {
        return new QuotationItemDto(i.getId(), i.getItemName(), i.getDescription(),
                i.getQuantity(), i.getUnit(), i.getUnitPrice(), i.getTotalPrice(), i.getNotes());
    }
}
