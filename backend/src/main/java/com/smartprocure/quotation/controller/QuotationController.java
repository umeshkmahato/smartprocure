package com.smartprocure.quotation.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.quotation.dto.*;
import com.smartprocure.quotation.entity.QuotationRequestStatus;
import com.smartprocure.quotation.service.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Quotation / RFQ lifecycle.
 *
 * <pre>
 * POST   /api/v1/rfqs                              → create RFQ (DRAFT)
 * GET    /api/v1/rfqs/{id}                          → get RFQ
 * GET    /api/v1/rfqs                               → search RFQs
 * PATCH  /api/v1/rfqs/{id}/publish                 → DRAFT → PUBLISHED
 * PATCH  /api/v1/rfqs/{id}/close                   → PUBLISHED → CLOSED
 * PATCH  /api/v1/rfqs/{id}/cancel                  → cancel RFQ
 *
 * POST   /api/v1/rfqs/{id}/quotations/{vendorId}   → vendor submits quotation
 * GET    /api/v1/rfqs/{id}/quotations              → compare all quotations (sorted by price)
 * PATCH  /api/v1/rfqs/{id}/quotations/{qId}/select → select winning quotation
 *
 * GET    /api/v1/rfqs/vendors/{vendorId}/quotations → all quotations by a vendor
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/rfqs")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    // ── RFQ CRUD ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<QuotationRequestDto>> createRfq(
            @Valid @RequestBody CreateQuotationRequestRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(quotationService.createRfq(req), "RFQ created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<QuotationRequestDto>> getRfqById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(quotationService.getRfqById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<QuotationRequestDto>>> search(
            @RequestParam(required = false) QuotationRequestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.searchRfqs(status, search, pageable)));
    }

    // ── RFQ lifecycle ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<QuotationRequestDto>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.publishRfq(id), "RFQ published — vendors can now submit quotations"));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<QuotationRequestDto>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.closeRfq(id), "RFQ closed"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<QuotationRequestDto>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.cancelRfq(id), "RFQ cancelled"));
    }

    // ── Quotation submission & comparison ────────────────────────────────────

    @PostMapping("/{id}/quotations/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','VENDOR')")
    public ResponseEntity<ApiResponse<QuotationDto>> submitQuotation(
            @PathVariable Long id,
            @PathVariable Long vendorId,
            @Valid @RequestBody SubmitQuotationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        quotationService.submitQuotation(id, vendorId, req),
                        "Quotation submitted successfully"));
    }

    @GetMapping("/{id}/quotations")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<List<QuotationDto>>> compareQuotations(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(quotationService.compareQuotations(id)));
    }

    @PatchMapping("/{id}/quotations/{quotationId}/select")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<QuotationDto>> selectQuotation(
            @PathVariable Long id,
            @PathVariable Long quotationId) {
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.selectQuotation(id, quotationId),
                "Winning quotation selected"));
    }

    @GetMapping("/vendors/{vendorId}/quotations")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PageResponse<QuotationDto>>> vendorQuotations(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                quotationService.getVendorQuotations(vendorId, pageable)));
    }
}
