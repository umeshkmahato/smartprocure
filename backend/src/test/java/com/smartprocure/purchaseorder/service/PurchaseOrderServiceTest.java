package com.smartprocure.purchaseorder.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.purchaseorder.dto.*;
import com.smartprocure.purchaseorder.entity.*;
import com.smartprocure.purchaseorder.repository.PurchaseOrderItemRepository;
import com.smartprocure.purchaseorder.repository.PurchaseOrderRepository;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.purchaserequest.repository.PurchaseRequestRepository;
import com.smartprocure.quotation.repository.QuotationRepository;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.entity.VendorStatus;
import com.smartprocure.vendor.repository.VendorRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService")
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository     poRepository;
    @Mock private PurchaseOrderItemRepository itemRepository;
    @Mock private VendorRepository            vendorRepository;
    @Mock private UserRepository              userRepository;
    @Mock private PurchaseRequestRepository   prRepository;
    @Mock private QuotationRepository         quotationRepository;
    @Mock private Authentication              auth;

    @InjectMocks private PurchaseOrderService poService;

    private Vendor       approvedVendor;
    private Vendor       unapprovedVendor;
    private User         buyer;
    private PurchaseOrder draftPo;
    private PurchaseOrder issuedPo;
    private PurchaseOrder acknowledgedPo;

    @BeforeEach
    void setUp() {
        approvedVendor = new Vendor();
        approvedVendor.setId(1L);
        approvedVendor.setName("Acme Corp");
        approvedVendor.setStatus(VendorStatus.APPROVED);

        unapprovedVendor = new Vendor();
        unapprovedVendor.setId(2L);
        unapprovedVendor.setName("New Vendor");
        unapprovedVendor.setStatus(VendorStatus.PENDING);

        buyer = new User();
        buyer.setId(10L);
        buyer.setUsername("buyer1");
        buyer.setFirstName("John");
        buyer.setLastName("Buyer");

        draftPo = buildPo(1L, "PO-20250101-00001", PurchaseOrderStatus.DRAFT);
        issuedPo = buildPo(2L, "PO-20250101-00002", PurchaseOrderStatus.ISSUED);
        acknowledgedPo = buildPo(3L, "PO-20250101-00003", PurchaseOrderStatus.ACKNOWLEDGED);
    }

    private PurchaseOrder buildPo(Long id, String number, PurchaseOrderStatus status) {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(id);
        po.setPoNumber(number);
        po.setStatus(status);
        po.setVendor(approvedVendor);
        po.setIssuedBy(buyer);
        po.setCurrency("USD");
        po.setTotalAmount(BigDecimal.ZERO);
        po.setItems(new ArrayList<>());
        return po;
    }

    private PoLineItemRequest lineItemReq() {
        return new PoLineItemRequest("Laptop", null,
                BigDecimal.ONE, "unit", new BigDecimal("1200.00"), null);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("create")
    class Create {

        @Test @DisplayName("approved vendor + valid items → PO saved as DRAFT")
        void create_approvedVendor_savesDraftPo() {
            var req = new CreatePurchaseOrderRequest(
                    1L, null, null, "USD",
                    LocalDate.now().plusDays(30),
                    "123 Main St", "Net 30", null,
                    List.of(lineItemReq()));

            when(auth.getName()).thenReturn("buyer1");
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(approvedVendor));
            when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
            when(poRepository.count()).thenReturn(0L);
            when(poRepository.save(any())).thenAnswer(inv -> {
                PurchaseOrder po = inv.getArgument(0);
                po.setId(100L);
                return po;
            });

            PurchaseOrderDto dto = poService.create(req, auth);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(dto.vendorId()).isEqualTo(1L);
            assertThat(dto.totalAmount()).isEqualByComparingTo("1200.00");
            verify(poRepository).save(any());
        }

        @Test @DisplayName("vendor not found → VENDOR_NOT_FOUND")
        void create_vendorNotFound_throws() {
            var req = new CreatePurchaseOrderRequest(
                    99L, null, null, null, null, null, null, null,
                    List.of(lineItemReq()));
            when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> poService.create(req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_FOUND));
        }

        @Test @DisplayName("unapproved vendor → VENDOR_NOT_APPROVED")
        void create_unapprovedVendor_throws() {
            var req = new CreatePurchaseOrderRequest(
                    2L, null, null, null, null, null, null, null,
                    List.of(lineItemReq()));
            when(vendorRepository.findById(2L)).thenReturn(Optional.of(unapprovedVendor));

            assertThatThrownBy(() -> poService.create(req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_APPROVED));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("getById")
    class GetById {

        @Test @DisplayName("existing id → returns DTO")
        void getById_found_returnsDto() {
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));
            PurchaseOrderDto dto = poService.getById(1L);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.poNumber()).isEqualTo("PO-20250101-00001");
        }

        @Test @DisplayName("missing id → PURCHASE_ORDER_NOT_FOUND")
        void getById_notFound_throws() {
            when(poRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> poService.getById(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND));
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("update")
    class Update {

        @Test @DisplayName("DRAFT PO → allowed fields updated")
        void update_draft_succeeds() {
            var req = new UpdatePurchaseOrderRequest("EUR", null,
                    "456 New St", "Net 60", "Updated notes", null);
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.update(1L, req);

            assertThat(dto.currency()).isEqualTo("EUR");
            assertThat(dto.paymentTerms()).isEqualTo("Net 60");
        }

        @Test @DisplayName("non-DRAFT PO → PURCHASE_ORDER_INVALID_TRANSITION")
        void update_nonDraft_throws() {
            var req = new UpdatePurchaseOrderRequest(null, null, null, null, null, null);
            when(poRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(issuedPo));

            assertThatThrownBy(() -> poService.update(2L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION));
        }
    }

    // ── issue ─────────────────────────────────────────────────────────────────

    @Nested @DisplayName("issue")
    class Issue {

        @Test @DisplayName("DRAFT PO → becomes ISSUED with issuedAt timestamp")
        void issue_draft_becomesIssued() {
            when(auth.getName()).thenReturn("buyer1");
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));
            when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.issue(1L, auth);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.ISSUED);
            assertThat(dto.issuedAt()).isNotNull();
        }

        @Test @DisplayName("non-DRAFT PO → PURCHASE_ORDER_INVALID_TRANSITION")
        void issue_nonDraft_throws() {
            when(poRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(issuedPo));

            assertThatThrownBy(() -> poService.issue(2L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION));
        }
    }

    // ── acknowledge ───────────────────────────────────────────────────────────

    @Nested @DisplayName("acknowledge")
    class Acknowledge {

        @Test @DisplayName("ISSUED PO → becomes ACKNOWLEDGED")
        void acknowledge_issued_becomesAcknowledged() {
            when(poRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(issuedPo));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.acknowledge(2L);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.ACKNOWLEDGED);
            assertThat(dto.acknowledgedAt()).isNotNull();
        }

        @Test @DisplayName("DRAFT PO → PURCHASE_ORDER_INVALID_TRANSITION")
        void acknowledge_draft_throws() {
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));

            assertThatThrownBy(() -> poService.acknowledge(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION));
        }
    }

    // ── recordDelivery ────────────────────────────────────────────────────────

    @Nested @DisplayName("recordDelivery")
    class RecordDelivery {

        @Test @DisplayName("partial delivery → status becomes PARTIALLY_DELIVERED")
        void recordDelivery_partial_becomesPartiallyDelivered() {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setId(10L);
            item.setItemName("Laptop");
            item.setQuantity(new BigDecimal("5"));
            item.setUnitPrice(new BigDecimal("1200.00"));
            item.setTotalPrice(new BigDecimal("6000.00"));
            item.setDeliveredQty(BigDecimal.ZERO);
            item.setPurchaseOrder(acknowledgedPo);
            acknowledgedPo.getItems().add(item);

            var req = new RecordDeliveryRequest(
                    List.of(new RecordDeliveryRequest.DeliveryLine(10L, new BigDecimal("3"))));

            when(poRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(acknowledgedPo));
            when(itemRepository.findByIdAndPurchaseOrderId(10L, 3L)).thenReturn(Optional.of(item));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.recordDelivery(3L, req);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.PARTIALLY_DELIVERED);
            assertThat(item.getDeliveredQty()).isEqualByComparingTo("3");
        }

        @Test @DisplayName("full delivery → status becomes DELIVERED with deliveredAt")
        void recordDelivery_full_becomesDelivered() {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setId(11L);
            item.setItemName("Chair");
            item.setQuantity(new BigDecimal("2"));
            item.setUnitPrice(new BigDecimal("300.00"));
            item.setTotalPrice(new BigDecimal("600.00"));
            item.setDeliveredQty(BigDecimal.ZERO);
            item.setPurchaseOrder(acknowledgedPo);
            acknowledgedPo.getItems().add(item);

            var req = new RecordDeliveryRequest(
                    List.of(new RecordDeliveryRequest.DeliveryLine(11L, new BigDecimal("2"))));

            when(poRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(acknowledgedPo));
            when(itemRepository.findByIdAndPurchaseOrderId(11L, 3L)).thenReturn(Optional.of(item));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.recordDelivery(3L, req);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.DELIVERED);
            assertThat(dto.deliveredAt()).isNotNull();
        }

        @Test @DisplayName("wrong status → PURCHASE_ORDER_INVALID_TRANSITION")
        void recordDelivery_wrongStatus_throws() {
            var req = new RecordDeliveryRequest(
                    List.of(new RecordDeliveryRequest.DeliveryLine(1L, BigDecimal.ONE)));
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));

            assertThatThrownBy(() -> poService.recordDelivery(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION));
        }
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("cancel")
    class Cancel {

        @Test @DisplayName("DRAFT PO → cancelled")
        void cancel_draft_becomesCancelled() {
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.cancel(1L);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        }

        @Test @DisplayName("ISSUED PO → cancelled")
        void cancel_issued_becomesCancelled() {
            when(poRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(issuedPo));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.cancel(2L);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        }

        @Test @DisplayName("ACKNOWLEDGED PO → PURCHASE_ORDER_CANNOT_CANCEL")
        void cancel_acknowledged_throws() {
            when(poRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(acknowledgedPo));

            assertThatThrownBy(() -> poService.cancel(3L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_CANNOT_CANCEL));
        }
    }

    // ── close ─────────────────────────────────────────────────────────────────

    @Nested @DisplayName("close")
    class Close {

        @Test @DisplayName("INVOICED PO → becomes CLOSED")
        void close_invoiced_becomesClosed() {
            PurchaseOrder invoicedPo = buildPo(4L, "PO-20250101-00004", PurchaseOrderStatus.INVOICED);
            when(poRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(invoicedPo));
            when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseOrderDto dto = poService.close(4L);

            assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.CLOSED);
        }

        @Test @DisplayName("DRAFT PO → PURCHASE_ORDER_INVALID_TRANSITION")
        void close_draft_throws() {
            when(poRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(draftPo));

            assertThatThrownBy(() -> poService.close(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_INVALID_TRANSITION));
        }
    }
}
