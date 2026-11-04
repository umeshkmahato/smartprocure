package com.smartprocure.purchaserequest.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.purchaserequest.dto.*;
import com.smartprocure.purchaserequest.entity.*;
import com.smartprocure.purchaserequest.repository.PurchaseRequestItemRepository;
import com.smartprocure.purchaserequest.repository.PurchaseRequestRepository;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestService {

    private final PurchaseRequestRepository     prRepository;
    private final PurchaseRequestItemRepository itemRepository;
    private final UserRepository                userRepository;
    private final VendorRepository              vendorRepository;

    // Simple in-memory sequence suffix — reset-safe across restarts via DB count
    private static final AtomicLong SEQ = new AtomicLong(0);

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseRequestDto create(CreatePurchaseRequestRequest req, Authentication auth) {
        User requester = resolveUser(auth.getName());

        PurchaseRequest pr = PurchaseRequest.builder()
                .prNumber(generatePrNumber())
                .title(req.title())
                .description(req.description())
                .status(PurchaseRequestStatus.DRAFT)
                .priority(req.priority() != null ? req.priority() : PurchaseRequestPriority.MEDIUM)
                .requester(requester)
                .department(req.department())
                .requiredByDate(req.requiredByDate())
                .currency(req.currency() != null ? req.currency() : "USD")
                .notes(req.notes())
                .build();

        req.items().forEach(itemReq -> pr.addItem(buildItem(itemReq, pr)));
        pr.recalcTotal();

        PurchaseRequest saved = prRepository.save(pr);
        log.info("PR created: id={}, prNumber={}, requester={}", saved.getId(), saved.getPrNumber(), auth.getName());
        return PurchaseRequestDto.fromEntity(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public PurchaseRequestDto getById(Long id) {
        return PurchaseRequestDto.fromEntity(findOrThrow(id));
    }

    public PageResponse<PurchaseRequestDto> search(PurchaseRequestStatus   status,
                                                   PurchaseRequestPriority priority,
                                                   Long                    requesterId,
                                                   String                  search,
                                                   Pageable                pageable) {
        return PageResponse.from(
                prRepository.search(status, priority, requesterId, search, pageable)
                            .map(PurchaseRequestDto::fromEntity));
    }

    public PageResponse<PurchaseRequestDto> getMyRequests(Authentication auth, Pageable pageable) {
        User requester = resolveUser(auth.getName());
        return PageResponse.from(
                prRepository.findByRequesterId(requester.getId(), pageable)
                            .map(PurchaseRequestDto::fromEntity));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseRequestDto update(Long id, UpdatePurchaseRequestRequest req, Authentication auth) {
        PurchaseRequest pr = findOrThrow(id);

        ensureOwnerOrAdmin(pr, auth);

        if (!pr.isDraft()) {
            throw new BusinessException(ErrorCode.PR_NOT_EDITABLE,
                    "Only DRAFT purchase requests can be updated");
        }

        if (req.title()        != null) pr.setTitle(req.title());
        if (req.description()  != null) pr.setDescription(req.description());
        if (req.priority()     != null) pr.setPriority(req.priority());
        if (req.department()   != null) pr.setDepartment(req.department());
        if (req.requiredByDate() != null) pr.setRequiredByDate(req.requiredByDate());
        if (req.currency()     != null) pr.setCurrency(req.currency());
        if (req.notes()        != null) pr.setNotes(req.notes());

        if (req.items() != null && !req.items().isEmpty()) {
            itemRepository.deleteAllByPurchaseRequestId(pr.getId());
            pr.getItems().clear();
            req.items().forEach(itemReq -> pr.addItem(buildItem(itemReq, pr)));
            pr.recalcTotal();
        }

        log.info("PR updated: id={}, updatedBy={}", id, auth.getName());
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    // ── Lifecycle transitions ─────────────────────────────────────────────────

    @Transactional
    public PurchaseRequestDto submit(Long id, Authentication auth) {
        PurchaseRequest pr = findOrThrow(id);
        ensureOwnerOrAdmin(pr, auth);

        if (!pr.isDraft()) {
            throw new BusinessException(ErrorCode.PR_INVALID_TRANSITION,
                    "Only DRAFT purchase requests can be submitted");
        }
        pr.setStatus(PurchaseRequestStatus.SUBMITTED);
        log.info("PR submitted: id={}", id);
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequestDto approve(Long id) {
        PurchaseRequest pr = findOrThrow(id);
        if (pr.getStatus() != PurchaseRequestStatus.SUBMITTED
                && pr.getStatus() != PurchaseRequestStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.PR_INVALID_TRANSITION,
                    "Only SUBMITTED or UNDER_REVIEW purchase requests can be approved");
        }
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        log.info("PR approved: id={}", id);
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequestDto reject(Long id, RejectPurchaseRequestRequest req) {
        PurchaseRequest pr = findOrThrow(id);
        if (pr.getStatus() != PurchaseRequestStatus.SUBMITTED
                && pr.getStatus() != PurchaseRequestStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.PR_INVALID_TRANSITION,
                    "Only SUBMITTED or UNDER_REVIEW purchase requests can be rejected");
        }
        pr.setStatus(PurchaseRequestStatus.REJECTED);
        pr.setRejectionReason(req.reason());
        log.info("PR rejected: id={}, reason={}", id, req.reason());
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequestDto cancel(Long id, Authentication auth) {
        PurchaseRequest pr = findOrThrow(id);
        ensureOwnerOrAdmin(pr, auth);

        if (!pr.isCancellable()) {
            throw new BusinessException(ErrorCode.PR_INVALID_TRANSITION,
                    "Purchase request cannot be cancelled in its current state: " + pr.getStatus());
        }
        pr.setStatus(PurchaseRequestStatus.CANCELLED);
        log.info("PR cancelled: id={}", id);
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    // ── Line items ────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseRequestDto addItem(Long prId, LineItemRequest req, Authentication auth) {
        PurchaseRequest pr = findOrThrow(prId);
        ensureOwnerOrAdmin(pr, auth);

        if (!pr.isDraft()) {
            throw new BusinessException(ErrorCode.PR_NOT_EDITABLE,
                    "Items can only be added to DRAFT purchase requests");
        }

        pr.addItem(buildItem(req, pr));
        pr.recalcTotal();
        log.info("Item added to PR: prId={}", prId);
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    @Transactional
    public PurchaseRequestDto removeItem(Long prId, Long itemId, Authentication auth) {
        PurchaseRequest pr = findOrThrow(prId);
        ensureOwnerOrAdmin(pr, auth);

        if (!pr.isDraft()) {
            throw new BusinessException(ErrorCode.PR_NOT_EDITABLE,
                    "Items can only be removed from DRAFT purchase requests");
        }

        PurchaseRequestItem item = itemRepository.findByIdAndPurchaseRequestId(itemId, prId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_ITEM_NOT_FOUND,
                        "Line item " + itemId + " not found on PR " + prId));

        pr.removeItem(item);
        log.info("Item removed from PR: prId={}, itemId={}", prId, itemId);
        return PurchaseRequestDto.fromEntity(prRepository.save(pr));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private PurchaseRequest findOrThrow(Long id) {
        return prRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_NOT_FOUND,
                        "Purchase Request with id " + id + " not found"));
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found: " + username));
    }

    private void ensureOwnerOrAdmin(PurchaseRequest pr, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = pr.getRequester().getUsername().equals(auth.getName());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "You do not have permission to modify this purchase request");
        }
    }

    private PurchaseRequestItem buildItem(LineItemRequest req, PurchaseRequest pr) {
        Vendor vendor = null;
        if (req.preferredVendorId() != null) {
            vendor = vendorRepository.findById(req.preferredVendorId()).orElse(null);
        }
        PurchaseRequestItem item = PurchaseRequestItem.builder()
                .purchaseRequest(pr)
                .itemName(req.itemName())
                .description(req.description())
                .quantity(req.quantity())
                .unit(req.unit())
                .unitPrice(req.unitPrice())
                .totalPrice(req.quantity().multiply(req.unitPrice()))
                .preferredVendor(vendor)
                .notes(req.notes())
                .build();
        return item;
    }

    private String generatePrNumber() {
        String prefix = "PR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = prRepository.count() + SEQ.incrementAndGet();
        return prefix + String.format("%05d", count);
    }
}
