package com.smartprocure.invoice.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.invoice.dto.*;
import com.smartprocure.invoice.entity.InvoiceStatus;
import com.smartprocure.common.util.SortValidator;
import com.smartprocure.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for Invoice lifecycle.
 *
 * <pre>
 * POST   /api/v1/invoices                       → submit invoice (vendor)
 * GET    /api/v1/invoices/{id}                  → get by id
 * GET    /api/v1/invoices                       → search / filter
 * PUT    /api/v1/invoices/{id}                  → update SUBMITTED or DISPUTED
 * PATCH  /api/v1/invoices/{id}/review           → SUBMITTED → UNDER_REVIEW
 * PATCH  /api/v1/invoices/{id}/verify           → → VERIFIED (amount match check)
 * PATCH  /api/v1/invoices/{id}/dispute          → → DISPUTED (with reason)
 * PATCH  /api/v1/invoices/{id}/pay              → VERIFIED → PAID
 * PATCH  /api/v1/invoices/{id}/cancel           → cancel before payment
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private static final Set<String> SORT_FIELDS =
            Set.of("invoiceNumber", "status", "totalAmount", "invoiceDate", "dueDate", "createdAt");

    private final InvoiceService invoiceService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','VENDOR')")
    public ResponseEntity<ApiResponse<InvoiceDto>> create(
            @Valid @RequestBody CreateInvoiceRequest req) {
        InvoiceDto dto = invoiceService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Invoice submitted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvoiceDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceDto>>> search(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long          vendorId,
            @RequestParam(required = false) Long          poId,
            @RequestParam(required = false) String        keyword,
            @RequestParam(defaultValue = "0")           int    page,
            @RequestParam(defaultValue = "20")          int    size,
            @RequestParam(defaultValue = "createdAt")   String sort,
            @RequestParam(defaultValue = "desc")        String dir) {

        SortValidator.validate(sort, SORT_FIELDS);
        var pageable = PageRequest.of(page, size, Sort.by(SortValidator.direction(dir), sort));
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.search(status, vendorId, poId, keyword, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','VENDOR')")
    public ResponseEntity<ApiResponse<InvoiceDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.update(id, req), "Invoice updated successfully"));
    }

    // ── Lifecycle transitions ─────────────────────────────────────────────────

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceDto>> startReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.startReview(id), "Invoice moved to UNDER_REVIEW"));
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceDto>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.verify(id), "Invoice verified successfully"));
    }

    @PatchMapping("/{id}/dispute")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceDto>> dispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeInvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.dispute(id, req), "Invoice disputed"));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceDto>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.markPaid(id), "Invoice marked as PAID"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceDto>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.cancel(id), "Invoice cancelled"));
    }
}
