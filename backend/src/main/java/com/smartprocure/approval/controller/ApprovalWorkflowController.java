package com.smartprocure.approval.controller;

import com.smartprocure.approval.dto.*;
import com.smartprocure.approval.service.ApprovalWorkflowService;
import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.common.util.SortValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST API for the multi-level Approval Workflow.
 *
 * <pre>
 * POST   /api/v1/approvals/purchase-requests/{prId}/initiate  → start workflow
 * GET    /api/v1/approvals/purchase-requests/{prId}           → steps for a PR
 * PATCH  /api/v1/approvals/{approvalId}/action                → approve / reject step
 *
 * GET    /api/v1/approvals/my/pending                         → my pending queue
 * GET    /api/v1/approvals/my/history                         → my action history
 *
 * POST   /api/v1/approvals/delegations                        → create delegation
 * GET    /api/v1/approvals/delegations/my                     → my delegations
 * DELETE /api/v1/approvals/delegations/{id}                   → revoke delegation
 *
 * GET    /api/v1/approvals/thresholds                         → list thresholds (admin)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalWorkflowController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "actionedAt", "level", "status");

    private final ApprovalWorkflowService approvalService;

    // ── Workflow lifecycle ────────────────────────────────────────────────────

    @PostMapping("/purchase-requests/{prId}/initiate")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<List<ApprovalRequestDto>>> initiate(
            @PathVariable Long prId) {

        List<ApprovalRequestDto> steps = approvalService.initiateWorkflow(prId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(steps, "Approval workflow initiated"));
    }

    @GetMapping("/purchase-requests/{prId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ApprovalRequestDto>>> getStepsForPr(
            @PathVariable Long prId) {

        return ResponseEntity.ok(ApiResponse.success(approvalService.getStepsForPr(prId)));
    }

    // ── Action ────────────────────────────────────────────────────────────────

    @PatchMapping("/{approvalId}/action")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalRequestDto>> action(
            @PathVariable Long approvalId,
            @Valid @RequestBody ActionApprovalRequest req,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                approvalService.action(approvalId, req, auth),
                "Approval step actioned successfully"));
    }

    // ── My queues ─────────────────────────────────────────────────────────────

    @GetMapping("/my/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalRequestDto>>> myPending(
            @RequestParam(defaultValue = "0")          int    page,
            @RequestParam(defaultValue = "20")         int    size,
            @RequestParam(defaultValue = "createdAt")  String sort,
            @RequestParam(defaultValue = "asc")        String dir,
            Authentication auth) {

        SortValidator.validate(sort, SORT_FIELDS);
        var pageable = PageRequest.of(page, size, Sort.by(SortValidator.direction(dir), sort));
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.myPendingApprovals(auth, pageable)));
    }

    @GetMapping("/my/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ApprovalRequestDto>>> myHistory(
            @RequestParam(defaultValue = "0")           int    page,
            @RequestParam(defaultValue = "20")          int    size,
            @RequestParam(defaultValue = "actionedAt")  String sort,
            @RequestParam(defaultValue = "desc")        String dir,
            Authentication auth) {

        SortValidator.validate(sort, SORT_FIELDS);
        var pageable = PageRequest.of(page, size, Sort.by(SortValidator.direction(dir), sort));
        return ResponseEntity.ok(ApiResponse.success(
                approvalService.myApprovalHistory(auth, pageable)));
    }

    // ── Delegations ───────────────────────────────────────────────────────────

    @PostMapping("/delegations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalDelegationDto>> createDelegation(
            @Valid @RequestBody CreateDelegationRequest req,
            Authentication auth) {

        ApprovalDelegationDto dto = approvalService.createDelegation(req, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Delegation created successfully"));
    }

    @GetMapping("/delegations/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ApprovalDelegationDto>>> myDelegations(
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(approvalService.myDelegations(auth)));
    }

    @DeleteMapping("/delegations/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalDelegationDto>> revokeDelegation(
            @PathVariable Long id,
            Authentication auth) {

        return ResponseEntity.ok(ApiResponse.success(
                approvalService.revokeDelegation(id, auth),
                "Delegation revoked successfully"));
    }

    // ── Thresholds ────────────────────────────────────────────────────────────

    @GetMapping("/thresholds")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<List<ApprovalThresholdDto>>> getThresholds() {
        return ResponseEntity.ok(ApiResponse.success(approvalService.getThresholds()));
    }
}
