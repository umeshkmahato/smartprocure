package com.smartprocure.purchaseorder.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.purchaseorder.dto.*;
import com.smartprocure.purchaseorder.entity.*;
import com.smartprocure.purchaseorder.repository.PurchaseOrderItemRepository;
import com.smartprocure.purchaseorder.repository.PurchaseOrderRepository;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.purchaserequest.repository.PurchaseRequestRepository;
import com.smartprocure.quotation.entity.Quotation;
import com.smartprocure.quotation.repository.QuotationRepository;
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
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository     poRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final VendorRepository            vendorRepository;
    private final UserRepository              userRepository;
    private final PurchaseRequestRepository   prRepository;
    private final QuotationRepository         quotationRepository;

    private static final AtomicLong SEQ = new AtomicLong(0);

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseOrderDto create(CreatePurchaseOrderRequest req, Authentication auth) {
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VENDOR_NOT_FOUND,
                        "Vendor with id " + req.vendorId() + " not found"));

        if (!vendor.isApproved()) {
            throw new BusinessException(ErrorCode.VENDOR_NOT_APPROVED,
                    "Vendor must be APPROVED before issuing a purchase order");
        }

        PurchaseRequest pr = null;
        if (req.purchaseRequestId() != null) {
            pr = prRepository.findById(req.purchaseRequestId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_REQUEST_NOT_FOUND,
                            "Purchase request with id " + req.purchaseRequestId() + " not found"));
        }

        Quotation quotation = null;
        if (req.quotationId() != null) {
            quotation = quotationRepository.findById(req.quotationId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND,
                            "Quotation with id " + req.quotationId() + " not found"));
        }

        User issuedBy = resolveUser(auth.getName());

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .vendor(vendor)
                .purchaseRequest(pr)
                .quotation(quotation)
                .issuedBy(issuedBy)
                .status(PurchaseOrderStatus.DRAFT)
                .currency(req.currency() != null ? req.currency() : "USD")
                .deliveryDate(req.deliveryDate())
                .deliveryAddress(req.deliveryAddress())
                .paymentTerms(req.paymentTerms())
                .notes(req.notes())
                .build();

        req.items().forEach(lineReq -> po.addItem(buildItem(lineReq, po)));
        po.recalcTotal();

        PurchaseOrder saved = poRepository.save(po);
        log.info("PO created: id={}, poNumber={}, vendor={}, issuedBy={}",
                saved.getId(), saved.getPoNumber(), vendor.getName(), auth.getName());
        return PurchaseOrderDto.fromEntity(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public PurchaseOrderDto getById(Long id) {
        return PurchaseOrderDto.fromEntity(findOrThrow(id));
    }

    public PurchaseOrderDto getByPoNumber(String poNumber) {
        PurchaseOrder po = poRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND,
                        "Purchase order with number " + poNumber + " not found"));
        // eagerly load items for DTO mapping
        po.getItems().size();
        return PurchaseOrderDto.fromEntity(po);
    }

    public PageResponse<PurchaseOrderDto> search(PurchaseOrderStatus status,
                                                  Long vendorId,
                                                  String keyword,
                                                  Pageable pageable) {
        return PageResponse.from(
                poRepository.search(status, vendorId, keyword, pageable)
                            .map(po -> {
                                po.getItems().size(); // init lazy collection
                                return PurchaseOrderDto.fromEntity(po);
                            }));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public PurchaseOrderDto update(Long id, UpdatePurchaseOrderRequest req) {
        PurchaseOrder po = findOrThrow(id);

        if (!po.isDraft()) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION,
                    "Only DRAFT purchase orders can be updated");
        }

        if (req.currency()        != null) po.setCurrency(req.currency());
        if (req.deliveryDate()    != null) po.setDeliveryDate(req.deliveryDate());
        if (req.deliveryAddress() != null) po.setDeliveryAddress(req.deliveryAddress());
        if (req.paymentTerms()    != null) po.setPaymentTerms(req.paymentTerms());
        if (req.notes()           != null) po.setNotes(req.notes());

        if (req.items() != null && !req.items().isEmpty()) {
            itemRepository.deleteByPurchaseOrderId(po.getId());
            po.getItems().clear();
            req.items().forEach(lineReq -> po.addItem(buildItem(lineReq, po)));
            po.recalcTotal();
        }

        log.info("PO updated: id={}", id);
        return PurchaseOrderDto.fromEntity(poRepository.save(po));
    }

    // ── Lifecycle transitions ─────────────────────────────────────────────────

    @Transactional
    public PurchaseOrderDto issue(Long id, Authentication auth) {
        PurchaseOrder po = findOrThrow(id);

        if (!po.isDraft()) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION,
                    "Only DRAFT purchase orders can be issued");
        }

        po.setStatus(PurchaseOrderStatus.ISSUED);
        po.setIssuedAt(LocalDateTime.now());
        po.setIssuedBy(resolveUser(auth.getName()));

        log.info("PO issued: id={}, poNumber={}", id, po.getPoNumber());
        return PurchaseOrderDto.fromEntity(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDto acknowledge(Long id) {
        PurchaseOrder po = findOrThrow(id);

        if (!po.isIssued()) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION,
                    "Only ISSUED purchase orders can be acknowledged");
        }

        po.setStatus(PurchaseOrderStatus.ACKNOWLEDGED);
        po.setAcknowledgedAt(LocalDateTime.now());

        log.info("PO acknowledged: id={}", id);
        return PurchaseOrderDto.fromEntity(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDto recordDelivery(Long id, RecordDeliveryRequest req) {
        PurchaseOrder po = findOrThrow(id);

        if (po.getStatus() != PurchaseOrderStatus.ACKNOWLEDGED
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_DELIVERED) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION,
                    "Delivery can only be recorded for ACKNOWLEDGED or PARTIALLY_DELIVERED orders");
        }

        req.lines().forEach(line -> {
            PurchaseOrderItem item = itemRepository
                    .findByIdAndPurchaseOrderId(line.itemId(), id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND,
                            "Item " + line.itemId() + " not found on PO " + id));
            item.setDeliveredQty(
                    item.getDeliveredQty().add(line.deliveredQty()));
        });

        // Advance status
        if (po.isFullyDelivered()) {
            po.setStatus(PurchaseOrderStatus.DELIVERED);
            po.setDeliveredAt(LocalDateTime.now());
            log.info("PO fully delivered: id={}", id);
        } else {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_DELIVERED);
            log.info("PO partially delivered: id={}", id);
        }

        return PurchaseOrderDto.fromEntity(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDto cancel(Long id) {
        PurchaseOrder po = findOrThrow(id);

        if (!po.isCancellable()) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_CANNOT_CANCEL,
                    "Purchase order cannot be cancelled in status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        log.info("PO cancelled: id={}", id);
        return PurchaseOrderDto.fromEntity(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDto close(Long id) {
        PurchaseOrder po = findOrThrow(id);

        if (po.getStatus() != PurchaseOrderStatus.INVOICED) {
            throw new BusinessException(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION,
                    "Only INVOICED purchase orders can be closed");
        }

        po.setStatus(PurchaseOrderStatus.CLOSED);
        log.info("PO closed: id={}", id);
        return PurchaseOrderDto.fromEntity(poRepository.save(po));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private PurchaseOrder findOrThrow(Long id) {
        PurchaseOrder po = poRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND,
                        "Purchase order with id " + id + " not found"));
        po.getItems().size(); // initialise lazy collection within transaction
        return po;
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "User not found: " + username));
    }

    private PurchaseOrderItem buildItem(PoLineItemRequest req, PurchaseOrder po) {
        return PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .itemName(req.itemName())
                .description(req.description())
                .quantity(req.quantity())
                .unit(req.unit())
                .unitPrice(req.unitPrice())
                .totalPrice(req.quantity().multiply(req.unitPrice()))
                .notes(req.notes())
                .build();
    }

    private String generatePoNumber() {
        String prefix = "PO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = poRepository.count() + SEQ.incrementAndGet();
        return prefix + String.format("%05d", count);
    }
}
