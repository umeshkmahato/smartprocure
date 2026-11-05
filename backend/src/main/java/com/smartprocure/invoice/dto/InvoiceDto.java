package com.smartprocure.invoice.dto;

import com.smartprocure.invoice.entity.Invoice;
import com.smartprocure.invoice.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceDto(
        Long            id,
        Long            purchaseOrderId,
        String          poNumber,
        Long            vendorId,
        String          vendorName,
        String          invoiceNumber,
        InvoiceStatus   status,
        BigDecimal      totalAmount,
        String          currency,
        LocalDate       invoiceDate,
        LocalDate       dueDate,
        String          documentUrl,
        String          notes,
        String          disputeReason,
        LocalDateTime   verifiedAt,
        LocalDateTime   paidAt,
        LocalDateTime   createdAt,
        LocalDateTime   updatedAt
) {
    public static InvoiceDto fromEntity(Invoice inv) {
        return new InvoiceDto(
                inv.getId(),
                inv.getPurchaseOrder().getId(),
                inv.getPurchaseOrder().getPoNumber(),
                inv.getVendor().getId(),
                inv.getVendor().getName(),
                inv.getInvoiceNumber(),
                inv.getStatus(),
                inv.getTotalAmount(),
                inv.getCurrency(),
                inv.getInvoiceDate(),
                inv.getDueDate(),
                inv.getDocumentUrl(),
                inv.getNotes(),
                inv.getDisputeReason(),
                inv.getVerifiedAt(),
                inv.getPaidAt(),
                inv.getCreatedAt(),
                inv.getUpdatedAt()
        );
    }
}
