package com.smartprocure.invoice.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.invoice.dto.*;
import com.smartprocure.invoice.entity.Invoice;
import com.smartprocure.invoice.entity.InvoiceStatus;
import com.smartprocure.invoice.repository.InvoiceRepository;
import com.smartprocure.purchaseorder.entity.PurchaseOrder;
import com.smartprocure.purchaseorder.entity.PurchaseOrderStatus;
import com.smartprocure.purchaseorder.repository.PurchaseOrderRepository;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.entity.VendorStatus;
import com.smartprocure.vendor.repository.VendorRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService")
class InvoiceServiceTest {

    @Mock private InvoiceRepository       invoiceRepository;
    @Mock private PurchaseOrderRepository poRepository;
    @Mock private VendorRepository        vendorRepository;

    @InjectMocks private InvoiceService invoiceService;

    private Vendor        vendor;
    private PurchaseOrder deliveredPo;
    private PurchaseOrder draftPo;
    private Invoice       submittedInvoice;
    private Invoice       underReviewInvoice;
    private Invoice       verifiedInvoice;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setId(1L);
        vendor.setName("Acme Corp");
        vendor.setStatus(VendorStatus.APPROVED);

        deliveredPo = new PurchaseOrder();
        deliveredPo.setId(10L);
        deliveredPo.setPoNumber("PO-20250101-00001");
        deliveredPo.setStatus(PurchaseOrderStatus.DELIVERED);
        deliveredPo.setVendor(vendor);
        deliveredPo.setCurrency("USD");
        deliveredPo.setTotalAmount(new BigDecimal("1000.00"));
        deliveredPo.setItems(new ArrayList<>());

        draftPo = new PurchaseOrder();
        draftPo.setId(11L);
        draftPo.setPoNumber("PO-20250101-00002");
        draftPo.setStatus(PurchaseOrderStatus.DRAFT);
        draftPo.setVendor(vendor);
        draftPo.setTotalAmount(new BigDecimal("500.00"));
        draftPo.setItems(new ArrayList<>());

        submittedInvoice = buildInvoice(1L, "INV-001", InvoiceStatus.SUBMITTED,
                new BigDecimal("1000.00"), deliveredPo);
        underReviewInvoice = buildInvoice(2L, "INV-002", InvoiceStatus.UNDER_REVIEW,
                new BigDecimal("1000.00"), deliveredPo);
        verifiedInvoice = buildInvoice(3L, "INV-003", InvoiceStatus.VERIFIED,
                new BigDecimal("1000.00"), deliveredPo);
    }

    private Invoice buildInvoice(Long id, String number, InvoiceStatus status,
                                  BigDecimal amount, PurchaseOrder po) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setInvoiceNumber(number);
        inv.setStatus(status);
        inv.setTotalAmount(amount);
        inv.setCurrency("USD");
        inv.setInvoiceDate(LocalDate.now());
        inv.setPurchaseOrder(po);
        inv.setVendor(vendor);
        return inv;
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("create")
    class Create {

        @Test @DisplayName("DELIVERED PO + unique number → invoice SUBMITTED")
        void create_valid_savesSubmittedInvoice() {
            var req = new CreateInvoiceRequest(10L, "INV-NEW-001",
                    new BigDecimal("1000.00"), "USD",
                    LocalDate.now(), LocalDate.now().plusDays(30),
                    null, null);

            when(invoiceRepository.existsByInvoiceNumber("INV-NEW-001")).thenReturn(false);
            when(poRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(deliveredPo));
            when(invoiceRepository.save(any())).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(100L);
                return i;
            });

            InvoiceDto dto = invoiceService.create(req);

            assertThat(dto.status()).isEqualTo(InvoiceStatus.SUBMITTED);
            assertThat(dto.invoiceNumber()).isEqualTo("INV-NEW-001");
            assertThat(dto.totalAmount()).isEqualByComparingTo("1000.00");
            verify(invoiceRepository).save(any());
        }

        @Test @DisplayName("duplicate invoice number → INVOICE_DUPLICATE")
        void create_duplicateNumber_throws() {
            var req = new CreateInvoiceRequest(10L, "INV-DUP",
                    BigDecimal.TEN, null, LocalDate.now(), null, null, null);
            when(invoiceRepository.existsByInvoiceNumber("INV-DUP")).thenReturn(true);

            assertThatThrownBy(() -> invoiceService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_DUPLICATE));
        }

        @Test @DisplayName("PO not found → PURCHASE_ORDER_NOT_FOUND")
        void create_poNotFound_throws() {
            var req = new CreateInvoiceRequest(99L, "INV-X",
                    BigDecimal.TEN, null, LocalDate.now(), null, null, null);
            when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
            when(poRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND));
        }

        @Test @DisplayName("DRAFT PO → INVOICE_PO_NOT_DELIVERED")
        void create_draftPo_throws() {
            var req = new CreateInvoiceRequest(11L, "INV-Y",
                    BigDecimal.TEN, null, LocalDate.now(), null, null, null);
            when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
            when(poRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(draftPo));

            assertThatThrownBy(() -> invoiceService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_PO_NOT_DELIVERED));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("getById")
    class GetById {

        @Test @DisplayName("existing id → returns DTO")
        void getById_found_returnsDto() {
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            InvoiceDto dto = invoiceService.getById(1L);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.invoiceNumber()).isEqualTo("INV-001");
        }

        @Test @DisplayName("missing id → INVOICE_NOT_FOUND")
        void getById_notFound_throws() {
            when(invoiceRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> invoiceService.getById(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_NOT_FOUND));
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("update")
    class Update {

        @Test @DisplayName("SUBMITTED invoice → updates allowed fields")
        void update_submitted_succeeds() {
            var req = new UpdateInvoiceRequest(new BigDecimal("950.00"),
                    "EUR", null, null, null, "Revised amount");
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InvoiceDto dto = invoiceService.update(1L, req);

            assertThat(dto.totalAmount()).isEqualByComparingTo("950.00");
            assertThat(dto.currency()).isEqualTo("EUR");
        }

        @Test @DisplayName("VERIFIED invoice → INVOICE_ALREADY_VERIFIED")
        void update_verified_throws() {
            var req = new UpdateInvoiceRequest(BigDecimal.TEN, null, null, null, null, null);
            when(invoiceRepository.findByIdWithDetails(3L))
                    .thenReturn(Optional.of(verifiedInvoice));

            assertThatThrownBy(() -> invoiceService.update(3L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_ALREADY_VERIFIED));
        }
    }

    // ── startReview ───────────────────────────────────────────────────────────

    @Nested @DisplayName("startReview")
    class StartReview {

        @Test @DisplayName("SUBMITTED → UNDER_REVIEW")
        void startReview_submitted_becomesUnderReview() {
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InvoiceDto dto = invoiceService.startReview(1L);
            assertThat(dto.status()).isEqualTo(InvoiceStatus.UNDER_REVIEW);
        }

        @Test @DisplayName("UNDER_REVIEW → INVOICE_ALREADY_VERIFIED")
        void startReview_alreadyUnderReview_throws() {
            when(invoiceRepository.findByIdWithDetails(2L))
                    .thenReturn(Optional.of(underReviewInvoice));
            assertThatThrownBy(() -> invoiceService.startReview(2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_ALREADY_VERIFIED));
        }
    }

    // ── verify ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("verify")
    class Verify {

        @Test @DisplayName("amount within 1% of PO total → VERIFIED, PO → INVOICED")
        void verify_amountMatch_becomesVerified() {
            // 1000.00 invoice vs 1000.00 PO → exact match
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(poRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InvoiceDto dto = invoiceService.verify(1L);

            assertThat(dto.status()).isEqualTo(InvoiceStatus.VERIFIED);
            assertThat(dto.verifiedAt()).isNotNull();
            verify(poRepository).save(argThat(po ->
                    po.getStatus() == PurchaseOrderStatus.INVOICED));
        }

        @Test @DisplayName("amount more than 1% off → INVOICE_AMOUNT_MISMATCH")
        void verify_amountMismatch_throws() {
            // invoice = 1200, PO = 1000 → 20% diff
            Invoice mismatchInvoice = buildInvoice(5L, "INV-MISMATCH",
                    InvoiceStatus.SUBMITTED, new BigDecimal("1200.00"), deliveredPo);
            when(invoiceRepository.findByIdWithDetails(5L))
                    .thenReturn(Optional.of(mismatchInvoice));

            assertThatThrownBy(() -> invoiceService.verify(5L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_AMOUNT_MISMATCH));
        }

        @Test @DisplayName("VERIFIED invoice → INVOICE_ALREADY_VERIFIED")
        void verify_alreadyVerified_throws() {
            when(invoiceRepository.findByIdWithDetails(3L))
                    .thenReturn(Optional.of(verifiedInvoice));
            assertThatThrownBy(() -> invoiceService.verify(3L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_ALREADY_VERIFIED));
        }
    }

    // ── dispute ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("dispute")
    class Dispute {

        @Test @DisplayName("SUBMITTED → DISPUTED with reason")
        void dispute_submitted_becomesDisputed() {
            var req = new DisputeInvoiceRequest("Wrong amount on line 2");
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InvoiceDto dto = invoiceService.dispute(1L, req);

            assertThat(dto.status()).isEqualTo(InvoiceStatus.DISPUTED);
            assertThat(dto.disputeReason()).isEqualTo("Wrong amount on line 2");
        }

        @Test @DisplayName("VERIFIED → INVOICE_ALREADY_VERIFIED")
        void dispute_verified_throws() {
            var req = new DisputeInvoiceRequest("Too late");
            when(invoiceRepository.findByIdWithDetails(3L))
                    .thenReturn(Optional.of(verifiedInvoice));
            assertThatThrownBy(() -> invoiceService.dispute(3L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_ALREADY_VERIFIED));
        }
    }

    // ── markPaid ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("markPaid")
    class MarkPaid {

        @Test @DisplayName("VERIFIED → PAID with paidAt timestamp")
        void markPaid_verified_becomesPaid() {
            when(invoiceRepository.findByIdWithDetails(3L))
                    .thenReturn(Optional.of(verifiedInvoice));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InvoiceDto dto = invoiceService.markPaid(3L);

            assertThat(dto.status()).isEqualTo(InvoiceStatus.PAID);
            assertThat(dto.paidAt()).isNotNull();
        }

        @Test @DisplayName("SUBMITTED → INVOICE_ALREADY_VERIFIED (not yet verified)")
        void markPaid_notVerified_throws() {
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            assertThatThrownBy(() -> invoiceService.markPaid(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_ALREADY_VERIFIED));
        }
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("cancel")
    class Cancel {

        @Test @DisplayName("SUBMITTED → CANCELLED")
        void cancel_submitted_becomesCancelled() {
            when(invoiceRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(submittedInvoice));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InvoiceDto dto = invoiceService.cancel(1L);
            assertThat(dto.status()).isEqualTo(InvoiceStatus.CANCELLED);
        }

        @Test @DisplayName("VERIFIED → INVOICE_ALREADY_VERIFIED (cannot cancel)")
        void cancel_verified_throws() {
            when(invoiceRepository.findByIdWithDetails(3L))
                    .thenReturn(Optional.of(verifiedInvoice));
            assertThatThrownBy(() -> invoiceService.cancel(3L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVOICE_ALREADY_VERIFIED));
        }
    }
}
