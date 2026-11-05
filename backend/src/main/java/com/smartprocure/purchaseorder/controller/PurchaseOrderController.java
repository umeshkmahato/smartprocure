package com.smartprocure.purchaseorder.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.purchaseorder.dto.*;
import com.smartprocure.purchaseorder.entity.PurchaseOrderStatus;
import com.smartprocure.common.util.SortValidator;
import com.smartprocure.purchaseorder.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for Purchase Order lifecycle.
 *
 * <pre>
 * POST   /api/v1/purchase-orders                        → create (DRAFT)
 * GET    /api/v1/purchase-orders/{id}                   → get by id
 * GET    /api/v1/purchase-orders/number/{poNumber}      → get by PO number
 * GET    /api/v1/purchase-orders                        → search
 * PUT    /api/v1/purchase-orders/{id}                   → update (DRAFT only)
 * PATCH  /api/v1/purchase-orders/{id}/issue             → DRAFT → ISSUED
 * PATCH  /api/v1/purchase-orders/{id}/acknowledge       → ISSUED → ACKNOWLEDGED
 * PATCH  /api/v1/purchase-orders/{id}/deliver           → record delivery lines
 * PATCH  /api/v1/purchase-orders/{id}/cancel            → cancel (DRAFT or ISSUED)
 * PATCH  /api/v1/purchase-orders/{id}/close             → INVOICED → CLOSED
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final Set<String> SORT_FIELDS =
            Set.of("poNumber", "status", "totalAmount", "deliveryDate", "issuedAt", "createdAt");

    private final PurchaseOrderService poService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> create(
            @Valid @RequestBody CreatePurchaseOrderRequest req,
            Authentication auth) {

        PurchaseOrderDto dto = poService.create(req, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Purchase order created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(poService.getById(id)));
    }

    @GetMapping("/number/{poNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getByPoNumber(
            @PathVariable String poNumber) {
        return ResponseEntity.ok(ApiResponse.success(poService.getByPoNumber(poNumber)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseOrderDto>>> search(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long               vendorId,
            @RequestParam(required = false) String             keyword,
            @RequestParam(defaultValue = "0")            int    page,
            @RequestParam(defaultValue = "20")           int    size,
            @RequestParam(defaultValue = "createdAt")    String sort,
            @RequestParam(defaultValue = "desc")         String dir) {

        SortValidator.validate(sort, SORT_FIELDS);
        var pageable = PageRequest.of(page, size, Sort.by(SortValidator.direction(dir), sort));
        return ResponseEntity.ok(ApiResponse.success(
                poService.search(status, vendorId, keyword, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseOrderRequest req) {

        return ResponseEntity.ok(ApiResponse.success(
                poService.update(id, req), "Purchase order updated successfully"));
    }

    // ── Lifecycle transitions ─────────────────────────────────────────────────

    @PatchMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> issue(
            @PathVariable Long id, Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                poService.issue(id, auth), "Purchase order issued successfully"));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','VENDOR')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.acknowledge(id), "Purchase order acknowledged"));
    }

    @PatchMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> recordDelivery(
            @PathVariable Long id,
            @Valid @RequestBody RecordDeliveryRequest req) {

        return ResponseEntity.ok(ApiResponse.success(
                poService.recordDelivery(id, req), "Delivery recorded successfully"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.cancel(id), "Purchase order cancelled"));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.close(id), "Purchase order closed"));
    }
}
