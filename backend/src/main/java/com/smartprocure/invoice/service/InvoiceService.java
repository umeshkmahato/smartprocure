package com.smartprocure.invoice.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.invoice.dto.*;
import com.smartprocure.invoice.entity.Invoice;
import com.smartprocure.invoice.entity.InvoiceStatus;
import com.smartprocure.invoice.repository.InvoiceRepository;
import com.smartprocure.purchaseorder.entity.PurchaseOrder;
import com.smartprocure.purchaseorder.entity.PurchaseOrderStatus;
import com.smartprocure.purchaseorder.repository.PurchaseOrderRepository;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.repository.VendorRepository;
import com.smartprocure.events.InvoiceVerifiedEvent;
import com.smartprocure.events.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository       invoiceRepository;
    private final PurchaseOrderRepository poRepository;
    private final VendorRepository        vendorRepository;
    private final KafkaEventPublisher     eventPublisher;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceDto create(CreateInvoiceRequest req) {

        if (invoiceRepository.existsByInvoiceNumber(req.invoiceNumber())) {
            throw new BusinessException(ErrorCode.INVOICE_DUPLICATE,
                    "Invoice number already exists: " + req.invoiceNumber());
        }

        PurchaseOrder po = poRepository.findByIdWithDetails(req.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND,
                        "Purchase order not found: " + req.purchaseOrderId()));

        if (po.getStatus() != PurchaseOrderStatus.DELIVERED
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_DELIVERED) {
            throw new BusinessException(ErrorCode.INVOICE_PO_NOT_DELIVERED,
                    "Purchase order must be DELIVERED or PARTIALLY_DELIVERED before invoicing");
        }

        Vendor vendor = po.getVendor();

        Invoice invoice = Invoice.builder()
                .purchaseOrder(po)
                .vendor(vendor)
                .invoiceNumber(req.invoiceNumber())
                .status(InvoiceStatus.SUBMITTED)
                .totalAmount(req.totalAmount())
                .currency(req.currency() != null ? req.currency() : po.getCurrency())
                .invoiceDate(req.invoiceDate())
                .dueDate(req.dueDate())
                .documentUrl(req.documentUrl())
                .notes(req.notes())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created: id={}, invoiceNumber={}, poId={}",
                saved.getId(), saved.getInvoiceNumber(), po.getId());
        return InvoiceDto.fromEntity(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public InvoiceDto getById(Long id) {
        return InvoiceDto.fromEntity(findOrThrow(id));
    }

    public PageResponse<InvoiceDto> search(InvoiceStatus status,
                                            Long vendorId,
                                            Long poId,
                                            String keyword,
                                            Pageable pageable) {
        return PageResponse.from(
                invoiceRepository.search(status, vendorId, poId, keyword, pageable)
                                 .map(InvoiceDto::fromEntity));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceDto update(Long id, UpdateInvoiceRequest req) {
        Invoice invoice = findOrThrow(id);

        if (invoice.getStatus() != InvoiceStatus.SUBMITTED
                && invoice.getStatus() != InvoiceStatus.DISPUTED) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VERIFIED,
                    "Only SUBMITTED or DISPUTED invoices can be updated");
        }

        if (req.totalAmount()  != null) invoice.setTotalAmount(req.totalAmount());
        if (req.currency()     != null) invoice.setCurrency(req.currency());
        if (req.invoiceDate()  != null) invoice.setInvoiceDate(req.invoiceDate());
        if (req.dueDate()      != null) invoice.setDueDate(req.dueDate());
        if (req.documentUrl()  != null) invoice.setDocumentUrl(req.documentUrl());
        if (req.notes()        != null) invoice.setNotes(req.notes());

        log.info("Invoice updated: id={}", id);
        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    // ── Lifecycle transitions ─────────────────────────────────────────────────

    @Transactional
    public InvoiceDto startReview(Long id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VERIFIED,
                    "Only SUBMITTED invoices can be moved to UNDER_REVIEW");
        }
        invoice.setStatus(InvoiceStatus.UNDER_REVIEW);
        log.info("Invoice under review: id={}", id);
        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceDto verify(Long id) {
        Invoice invoice = findOrThrow(id);
        if (!invoice.isVerifiable()) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VERIFIED,
                    "Invoice cannot be verified in status: " + invoice.getStatus());
        }

        // Tolerance-based amount match: within 1% of PO total
        PurchaseOrder po = invoice.getPurchaseOrder();
        if (po.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) != 0) {
            java.math.BigDecimal diff = invoice.getTotalAmount()
                    .subtract(po.getTotalAmount()).abs();
            java.math.BigDecimal tolerance = po.getTotalAmount()
                    .multiply(new java.math.BigDecimal("0.01"));
            if (diff.compareTo(tolerance) > 0) {
                throw new BusinessException(ErrorCode.INVOICE_AMOUNT_MISMATCH,
                        "Invoice total " + invoice.getTotalAmount()
                        + " differs from PO total " + po.getTotalAmount()
                        + " by more than 1%");
            }
        }

        invoice.setStatus(InvoiceStatus.VERIFIED);
        invoice.setVerifiedAt(LocalDateTime.now());

        // Advance PO to INVOICED
        po.setStatus(PurchaseOrderStatus.INVOICED);
        poRepository.save(po);

        Invoice verifiedInvoice = invoiceRepository.save(invoice);
        eventPublisher.publishInvoiceVerified(
                InvoiceVerifiedEvent.of(
                        verifiedInvoice.getId(), verifiedInvoice.getInvoiceNumber(),
                        po.getId(), po.getPoNumber(),
                        po.getVendor().getId(), po.getVendor().getName(),
                        po.getVendor().getEmail(),
                        verifiedInvoice.getTotalAmount(), verifiedInvoice.getCurrency()));
        log.info("Invoice verified: id={}, poId={}", id, po.getId());
        return InvoiceDto.fromEntity(verifiedInvoice);
    }

    @Transactional
    public InvoiceDto dispute(Long id, DisputeInvoiceRequest req) {
        Invoice invoice = findOrThrow(id);
        if (!invoice.isDisputable()) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VERIFIED,
                    "Invoice cannot be disputed in status: " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.DISPUTED);
        invoice.setDisputeReason(req.reason());
        log.info("Invoice disputed: id={}, reason={}", id, req.reason());
        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceDto markPaid(Long id) {
        Invoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.VERIFIED) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VERIFIED,
                    "Only VERIFIED invoices can be marked as PAID");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        log.info("Invoice marked paid: id={}", id);
        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceDto cancel(Long id) {
        Invoice invoice = findOrThrow(id);
        if (!invoice.isCancellable()) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_VERIFIED,
                    "Invoice cannot be cancelled in status: " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        log.info("Invoice cancelled: id={}", id);
        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND,
                        "Invoice with id " + id + " not found"));
    }
}
