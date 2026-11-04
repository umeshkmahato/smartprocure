package com.smartprocure.approval.service;

import com.smartprocure.approval.dto.*;
import com.smartprocure.approval.entity.*;
import com.smartprocure.approval.repository.*;
import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.purchaserequest.entity.PurchaseRequestStatus;
import com.smartprocure.purchaserequest.repository.PurchaseRequestRepository;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalWorkflowService {

    private final ApprovalRequestRepository    approvalRequestRepo;
    private final ApprovalThresholdRepository  thresholdRepo;
    private final ApprovalDelegationRepository delegationRepo;
    private final PurchaseRequestRepository    prRepo;
    private final UserRepository               userRepo;

    // ── Initiate workflow ─────────────────────────────────────────────────────

    /**
     * Called after a PR is submitted — creates one ApprovalRequest per matching threshold.
     */
    @Transactional
    public List<ApprovalRequestDto> initiateWorkflow(Long prId) {
        PurchaseRequest pr = findPrOrThrow(prId);

        if (pr.getStatus() != PurchaseRequestStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.PR_INVALID_TRANSITION,
                    "Approval workflow can only be initiated for SUBMITTED purchase requests");
        }

        List<ApprovalThreshold> thresholds =
                thresholdRepo.findApplicableThresholds(pr.getTotalAmount());

        if (thresholds.isEmpty()) {
            // No threshold applies — auto-approve
            pr.setStatus(PurchaseRequestStatus.APPROVED);
            prRepo.save(pr);
            log.info("PR id={} auto-approved (no applicable threshold)", prId);
            return List.of();
        }

        pr.setStatus(PurchaseRequestStatus.UNDER_REVIEW);
        prRepo.save(pr);

        List<ApprovalRequest> steps = thresholds.stream().map(t -> ApprovalRequest.builder()
                .purchaseRequest(pr)
                .threshold(t)
                .level(t.getLevel())
                .status(ApprovalStatus.PENDING)
                .build()).toList();

        List<ApprovalRequest> saved = approvalRequestRepo.saveAll(steps);
        log.info("Workflow initiated: prId={}, steps={}", prId, saved.size());
        return saved.stream().map(ApprovalRequestDto::fromEntity).toList();
    }

    // ── Action (approve / reject) ─────────────────────────────────────────────

    @Transactional
    public ApprovalRequestDto action(Long approvalId, ActionApprovalRequest req, Authentication auth) {
        ApprovalRequest ar = findApprovalOrThrow(approvalId);

        if (!ar.isPending()) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_ACTIONED,
                    "Approval step has already been actioned");
        }

        User actor = resolveUser(auth.getName());
        validateApprover(ar, actor);

        ar.setApprover(actor);
        ar.setActionedAt(LocalDateTime.now());
        ar.setComments(req.comments());

        if (req.action() == ActionApprovalRequest.ApprovalAction.APPROVED) {
            ar.setStatus(ApprovalStatus.APPROVED);
            advanceWorkflow(ar.getPurchaseRequest());
        } else {
            ar.setStatus(ApprovalStatus.REJECTED);
            rejectWorkflow(ar.getPurchaseRequest(), req.comments());
        }

        ApprovalRequest saved = approvalRequestRepo.save(ar);
        log.info("Approval step actioned: id={}, action={}, actor={}", approvalId, req.action(), auth.getName());
        return ApprovalRequestDto.fromEntity(saved);
    }

    // ── Delegation ────────────────────────────────────────────────────────────

    @Transactional
    public ApprovalDelegationDto createDelegation(CreateDelegationRequest req, Authentication auth) {
        User delegator = resolveUser(auth.getName());
        User delegate  = userRepo.findById(req.delegateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "Delegate user not found: " + req.delegateId()));

        if (delegator.getId().equals(delegate.getId())) {
            throw new BusinessException(ErrorCode.APPROVAL_SELF_DELEGATION,
                    "Cannot delegate to yourself");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BusinessException(ErrorCode.APPROVAL_INVALID_DATES,
                    "End date must not be before start date");
        }
        if (delegationRepo.hasOverlappingDelegation(delegator.getId(), req.startDate(), req.endDate())) {
            throw new BusinessException(ErrorCode.APPROVAL_DELEGATION_OVERLAP,
                    "An active delegation already exists in the requested date range");
        }

        ApprovalDelegation delegation = ApprovalDelegation.builder()
                .delegator(delegator)
                .delegate(delegate)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .reason(req.reason())
                .active(true)
                .build();

        ApprovalDelegation saved = delegationRepo.save(delegation);
        log.info("Delegation created: delegatorId={}, delegateId={}", delegator.getId(), delegate.getId());
        return ApprovalDelegationDto.fromEntity(saved);
    }

    @Transactional
    public ApprovalDelegationDto revokeDelegation(Long delegationId, Authentication auth) {
        ApprovalDelegation delegation = delegationRepo.findById(delegationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_DELEGATION_NOT_FOUND));

        if (!delegation.getDelegator().getUsername().equals(auth.getName())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only the delegator can revoke a delegation");
        }
        delegation.setActive(false);
        log.info("Delegation revoked: id={}", delegationId);
        return ApprovalDelegationDto.fromEntity(delegationRepo.save(delegation));
    }

    public List<ApprovalDelegationDto> myDelegations(Authentication auth) {
        User user = resolveUser(auth.getName());
        return delegationRepo.findByDelegatorId(user.getId())
                .stream().map(ApprovalDelegationDto::fromEntity).toList();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<ApprovalRequestDto> getStepsForPr(Long prId) {
        findPrOrThrow(prId);
        return approvalRequestRepo.findByPurchaseRequestIdOrderByLevelAsc(prId)
                .stream().map(ApprovalRequestDto::fromEntity).toList();
    }

    public PageResponse<ApprovalRequestDto> myPendingApprovals(Authentication auth, Pageable pageable) {
        User user = resolveUser(auth.getName());
        return PageResponse.from(
                approvalRequestRepo.findPendingForApprover(user.getId(), pageable)
                        .map(ApprovalRequestDto::fromEntity));
    }

    public PageResponse<ApprovalRequestDto> myApprovalHistory(Authentication auth, Pageable pageable) {
        User user = resolveUser(auth.getName());
        return PageResponse.from(
                approvalRequestRepo.findByApproverId(user.getId(), pageable)
                        .map(ApprovalRequestDto::fromEntity));
    }

    public List<ApprovalThresholdDto> getThresholds() {
        return thresholdRepo.findByActiveTrueOrderByLevelAsc()
                .stream().map(ApprovalThresholdDto::fromEntity).toList();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void advanceWorkflow(PurchaseRequest pr) {
        long approved = approvalRequestRepo.countApprovedSteps(pr.getId());
        long total    = approvalRequestRepo.countTotalSteps(pr.getId());

        if (approved >= total) {
            pr.setStatus(PurchaseRequestStatus.APPROVED);
            prRepo.save(pr);
            log.info("PR fully approved: id={}", pr.getId());
        }
        // else next level stays PENDING — awaiting its approver
    }

    private void rejectWorkflow(PurchaseRequest pr, String reason) {
        // Skip all remaining pending steps
        List<ApprovalRequest> pending = approvalRequestRepo
                .findByPurchaseRequestIdAndStatus(pr.getId(), ApprovalStatus.PENDING);
        pending.forEach(step -> {
            step.setStatus(ApprovalStatus.SKIPPED);
            step.setActionedAt(LocalDateTime.now());
        });
        approvalRequestRepo.saveAll(pending);

        pr.setStatus(PurchaseRequestStatus.REJECTED);
        pr.setRejectionReason(reason);
        prRepo.save(pr);
        log.info("PR rejected via workflow: id={}", pr.getId());
    }

    private void validateApprover(ApprovalRequest ar, User actor) {
        // Check if actor is the designated approver or has an active delegation
        boolean isDesignated = ar.getApprover() == null
                || ar.getApprover().getId().equals(actor.getId());

        if (!isDesignated) {
            // Check if actor is a delegate for the designated approver today
            boolean isDelegated = delegationRepo
                    .findActiveDelegation(ar.getApprover().getId(), LocalDate.now())
                    .map(d -> d.getDelegate().getId().equals(actor.getId()))
                    .orElse(false);
            if (!isDelegated) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED,
                        "You are not authorised to action this approval step");
            }
        }
    }

    private PurchaseRequest findPrOrThrow(Long prId) {
        return prRepo.findById(prId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_NOT_FOUND,
                        "Purchase Request " + prId + " not found"));
    }

    private ApprovalRequest findApprovalOrThrow(Long id) {
        return approvalRequestRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND,
                        "Approval step " + id + " not found"));
    }

    private User resolveUser(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
