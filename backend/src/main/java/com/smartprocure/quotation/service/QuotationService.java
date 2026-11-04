package com.smartprocure.quotation.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.quotation.dto.*;
import com.smartprocure.quotation.entity.*;
import com.smartprocure.quotation.repository.*;
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
public class QuotationService {

    private final QuotationRequestRepository rfqRepository;
    private final QuotationRepository        quotationRepository;
    private final VendorRepository           vendorRepository;
    private static final AtomicLong          SEQ = new AtomicLong(0);

    // ── RFQ lifecycle ─────────────────────────────────────────────────────────

    @Transactional
    public QuotationRequestDto createRfq(CreateQuotationRequestRequest req) {
        List<Vendor> vendors = vendorRepository.findAllById(req.vendorIds());
        if (vendors.size() != req.vendorIds().size()) {
            throw new BusinessException(ErrorCode.VENDOR_NOT_FOUND, "One or more vendor IDs not found");
        }

        QuotationRequest rfq = QuotationRequest.builder()
                .rfqNumber(generateRfqNumber())
                .title(req.title())
                .description(req.description())
                .deadline(req.deadline())
                .currency(req.currency() != null ? req.currency() : "USD")
                .notes(req.notes())
                .status(QuotationRequestStatus.DRAFT)
                .invitedVendors(vendors)
                .build();

        QuotationRequest saved = rfqRepository.save(rfq);
        log.info("RFQ created: id={}, rfqNumber={}", saved.getId(), saved.getRfqNumber());
        return QuotationRequestDto.fromEntity(saved);
    }

    public QuotationRequestDto getRfqById(Long id) {
        return QuotationRequestDto.fromEntity(findRfqOrThrow(id));
    }

    public PageResponse<QuotationRequestDto> searchRfqs(QuotationRequestStatus status,
                                                         String search, Pageable pageable) {
        return PageResponse.from(rfqRepository.search(status, search, pageable)
                .map(QuotationRequestDto::fromEntity));
    }

    @Transactional
    public QuotationRequestDto publishRfq(Long id) {
        QuotationRequest rfq = findRfqOrThrow(id);
        if (!rfq.isDraft()) {
            throw new BusinessException(ErrorCode.RFQ_INVALID_TRANSITION,
                    "Only DRAFT RFQs can be published");
        }
        rfq.setStatus(QuotationRequestStatus.PUBLISHED);
        log.info("RFQ published: id={}", id);
        return QuotationRequestDto.fromEntity(rfqRepository.save(rfq));
    }

    @Transactional
    public QuotationRequestDto closeRfq(Long id) {
        QuotationRequest rfq = findRfqOrThrow(id);
        if (!rfq.isPublished()) {
            throw new BusinessException(ErrorCode.RFQ_INVALID_TRANSITION,
                    "Only PUBLISHED RFQs can be closed");
        }
        rfq.setStatus(QuotationRequestStatus.CLOSED);
        log.info("RFQ closed: id={}", id);
        return QuotationRequestDto.fromEntity(rfqRepository.save(rfq));
    }

    @Transactional
    public QuotationRequestDto cancelRfq(Long id) {
        QuotationRequest rfq = findRfqOrThrow(id);
        if (rfq.isClosed()) {
            throw new BusinessException(ErrorCode.RFQ_INVALID_TRANSITION,
                    "CLOSED RFQs cannot be cancelled");
        }
        rfq.setStatus(QuotationRequestStatus.CANCELLED);
        log.info("RFQ cancelled: id={}", id);
        return QuotationRequestDto.fromEntity(rfqRepository.save(rfq));
    }

    // ── Quotation submission ───────────────────────────────────────────────────

    @Transactional
    public QuotationDto submitQuotation(Long rfqId, Long vendorId, SubmitQuotationRequest req) {
        QuotationRequest rfq = findRfqOrThrow(rfqId);

        if (!rfq.isPublished()) {
            throw new BusinessException(ErrorCode.RFQ_NOT_PUBLISHED,
                    "Quotations can only be submitted for PUBLISHED RFQs");
        }
        if (rfq.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.RFQ_DEADLINE_PASSED,
                    "The deadline for this RFQ has passed");
        }
        if (quotationRepository.existsByQuotationRequestIdAndVendorId(rfqId, vendorId)) {
            throw new BusinessException(ErrorCode.QUOTATION_ALREADY_SUBMITTED,
                    "Vendor has already submitted a quotation for this RFQ");
        }

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VENDOR_NOT_FOUND));

        boolean isInvited = rfq.getInvitedVendors().stream()
                .anyMatch(v -> v.getId().equals(vendorId));
        if (!isInvited) {
            throw new BusinessException(ErrorCode.VENDOR_NOT_INVITED,
                    "Vendor is not invited to this RFQ");
        }

        Quotation quotation = Quotation.builder()
                .quotationRequest(rfq)
                .vendor(vendor)
                .totalAmount(req.totalAmount())
                .currency(req.currency() != null ? req.currency() : rfq.getCurrency())
                .validityDate(req.validityDate())
                .notes(req.notes())
                .status(QuotationStatus.SUBMITTED)
                .selected(false)
                .build();

        req.items().forEach(itemReq -> {
            QuotationItem item = QuotationItem.builder()
                    .quotation(quotation)
                    .itemName(itemReq.itemName())
                    .description(itemReq.description())
                    .quantity(itemReq.quantity())
                    .unit(itemReq.unit())
                    .unitPrice(itemReq.unitPrice())
                    .totalPrice(itemReq.quantity().multiply(itemReq.unitPrice()))
                    .notes(itemReq.notes())
                    .build();
            quotation.getItems().add(item);
        });

        Quotation saved = quotationRepository.save(quotation);
        log.info("Quotation submitted: rfqId={}, vendorId={}, total={}",
                rfqId, vendorId, req.totalAmount());
        return QuotationDto.fromEntity(saved);
    }

    // ── Comparison & selection ────────────────────────────────────────────────

    public List<QuotationDto> compareQuotations(Long rfqId) {
        findRfqOrThrow(rfqId);
        return quotationRepository.findByQuotationRequestIdOrderByTotalAmountAsc(rfqId)
                .stream().map(QuotationDto::fromEntity).toList();
    }

    @Transactional
    public QuotationDto selectQuotation(Long rfqId, Long quotationId) {
        QuotationRequest rfq = findRfqOrThrow(rfqId);
        if (!rfq.isClosed()) {
            throw new BusinessException(ErrorCode.RFQ_INVALID_TRANSITION,
                    "RFQ must be CLOSED before a quotation can be selected");
        }

        quotationRepository.clearSelectedForRfq(rfqId);

        Quotation quotation = quotationRepository.findById(quotationId)
                .filter(q -> q.getQuotationRequest().getId().equals(rfqId))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND,
                        "Quotation not found in this RFQ"));

        quotation.setSelected(true);
        quotation.setStatus(QuotationStatus.ACCEPTED);
        quotation.setReviewedAt(LocalDateTime.now());

        // Reject all other submitted quotations
        quotationRepository.findByQuotationRequestIdAndStatus(rfqId, QuotationStatus.SUBMITTED)
                .forEach(q -> {
                    if (!q.getId().equals(quotationId)) {
                        q.setStatus(QuotationStatus.REJECTED);
                        q.setReviewedAt(LocalDateTime.now());
                    }
                });

        Quotation saved = quotationRepository.save(quotation);
        log.info("Quotation selected: rfqId={}, quotationId={}", rfqId, quotationId);
        return QuotationDto.fromEntity(saved);
    }

    public PageResponse<QuotationDto> getVendorQuotations(Long vendorId, Pageable pageable) {
        return PageResponse.from(quotationRepository.findByVendorId(vendorId, pageable)
                .map(QuotationDto::fromEntity));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private QuotationRequest findRfqOrThrow(Long id) {
        return rfqRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RFQ_NOT_FOUND,
                        "RFQ with id " + id + " not found"));
    }

    private String generateRfqNumber() {
        String prefix = "RFQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        return prefix + String.format("%05d", rfqRepository.count() + SEQ.incrementAndGet());
    }
}
