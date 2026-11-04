package com.smartprocure.purchaserequest.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.purchaserequest.dto.*;
import com.smartprocure.purchaserequest.entity.PurchaseRequestPriority;
import com.smartprocure.purchaserequest.entity.PurchaseRequestStatus;
import com.smartprocure.purchaserequest.service.PurchaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for Purchase Request lifecycle.
 *
 * <pre>
 * POST   /api/v1/purchase-requests                      → create (DRAFT)
 * GET    /api/v1/purchase-requests/{id}                 → get by id
 * GET    /api/v1/purchase-requests                      → search (admin/officer)
 * GET    /api/v1/purchase-requests/my                   → requester's own PRs
 * PUT    /api/v1/purchase-requests/{id}                 → update (DRAFT only)
 * PATCH  /api/v1/purchase-requests/{id}/submit          → DRAFT → SUBMITTED
 * PATCH  /api/v1/purchase-requests/{id}/approve         → SUBMITTED → APPROVED (ADMIN)
 * PATCH  /api/v1/purchase-requests/{id}/reject          → SUBMITTED → REJECTED  (ADMIN)
 * PATCH  /api/v1/purchase-requests/{id}/cancel          → cancel (owner/admin)
 * POST   /api/v1/purchase-requests/{id}/items           → add line item (DRAFT)
 * DELETE /api/v1/purchase-requests/{id}/items/{itemId}  → remove line item (DRAFT)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService prService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> create(
            @Valid @RequestBody CreatePurchaseRequestRequest req,
            Authentication auth) {

        PurchaseRequestDto dto = prService.create(req, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Purchase request created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseRequestDto>>> search(
            @RequestParam(required = false) PurchaseRequestStatus   status,
            @RequestParam(required = false) PurchaseRequestPriority priority,
            @RequestParam(required = false) Long                    requesterId,
            @RequestParam(required = false) String                  search,
            @RequestParam(defaultValue = "0")            int page,
            @RequestParam(defaultValue = "20")           int size,
            @RequestParam(defaultValue = "createdAt")    String sort) {

        var pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(ApiResponse.success(
                prService.search(status, priority, requesterId, search, pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseRequestDto>>> myRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(prService.getMyRequests(auth, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePurchaseRequestRequest req,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                prService.update(id, req, auth), "Purchase request updated successfully"));
    }

    // ── Lifecycle transitions ─────────────────────────────────────────────────

    @PatchMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> submit(
            @PathVariable Long id, Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                prService.submit(id, auth), "Purchase request submitted for approval"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                prService.approve(id), "Purchase request approved"));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectPurchaseRequestRequest req) {

        return ResponseEntity.ok(ApiResponse.success(
                prService.reject(id, req), "Purchase request rejected"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> cancel(
            @PathVariable Long id, Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                prService.cancel(id, auth), "Purchase request cancelled"));
    }

    // ── Line items ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> addItem(
            @PathVariable Long id,
            @Valid @RequestBody LineItemRequest req,
            Authentication auth) {

        PurchaseRequestDto dto = prService.addItem(id, req, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Line item added successfully"));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseRequestDto>> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                prService.removeItem(id, itemId, auth), "Line item removed successfully"));
    }
}
