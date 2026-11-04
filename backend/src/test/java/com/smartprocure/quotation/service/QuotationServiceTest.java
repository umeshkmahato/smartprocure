package com.smartprocure.quotation.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.quotation.dto.*;
import com.smartprocure.quotation.entity.*;
import com.smartprocure.quotation.repository.*;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.entity.VendorStatus;
import com.smartprocure.vendor.repository.VendorRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotationService")
class QuotationServiceTest {

    @Mock private QuotationRequestRepository rfqRepository;
    @Mock private QuotationRepository        quotationRepository;
    @Mock private VendorRepository           vendorRepository;
    @InjectMocks private QuotationService    service;

    private Vendor           vendor;
    private QuotationRequest draftRfq;
    private QuotationRequest publishedRfq;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setId(1L);
        vendor.setName("Acme Corp");
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setActive(true);

        draftRfq = new QuotationRequest();
        draftRfq.setId(10L);
        draftRfq.setRfqNumber("RFQ-20250101-00001");
        draftRfq.setTitle("Server Hardware");
        draftRfq.setStatus(QuotationRequestStatus.DRAFT);
        draftRfq.setDeadline(LocalDateTime.now().plusDays(7));
        draftRfq.setCurrency("USD");
        draftRfq.setInvitedVendors(new ArrayList<>(List.of(vendor)));
        draftRfq.setQuotations(new ArrayList<>());

        publishedRfq = new QuotationRequest();
        publishedRfq.setId(11L);
        publishedRfq.setRfqNumber("RFQ-20250101-00002");
        publishedRfq.setTitle("Office Supplies");
        publishedRfq.setStatus(QuotationRequestStatus.PUBLISHED);
        publishedRfq.setDeadline(LocalDateTime.now().plusDays(5));
        publishedRfq.setCurrency("USD");
        publishedRfq.setInvitedVendors(new ArrayList<>(List.of(vendor)));
        publishedRfq.setQuotations(new ArrayList<>());
    }

    // ── createRfq ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("createRfq")
    class CreateRfq {

        @Test @DisplayName("valid request → RFQ saved as DRAFT")
        void create_valid_returnsDraftDto() {
            var req = new CreateQuotationRequestRequest(
                    "Server Hardware", null, null,
                    LocalDateTime.now().plusDays(10), "USD", null, List.of(1L));

            when(vendorRepository.findAllById(List.of(1L))).thenReturn(List.of(vendor));
            when(rfqRepository.count()).thenReturn(0L);
            when(rfqRepository.save(any())).thenAnswer(inv -> {
                QuotationRequest r = inv.getArgument(0);
                r.setId(100L);
                return r;
            });

            QuotationRequestDto dto = service.createRfq(req);

            assertThat(dto.status()).isEqualTo(QuotationRequestStatus.DRAFT);
            assertThat(dto.title()).isEqualTo("Server Hardware");
        }

        @Test @DisplayName("invalid vendor ID → VENDOR_NOT_FOUND")
        void create_invalidVendor_throws() {
            var req = new CreateQuotationRequestRequest(
                    "T", null, null, LocalDateTime.now().plusDays(1), null, null, List.of(99L));
            when(vendorRepository.findAllById(List.of(99L))).thenReturn(List.of());

            assertThatThrownBy(() -> service.createRfq(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_FOUND));
        }
    }

    // ── publishRfq ────────────────────────────────────────────────────────────

    @Nested @DisplayName("publishRfq")
    class PublishRfq {

        @Test @DisplayName("DRAFT RFQ → becomes PUBLISHED")
        void publish_draft_becomesPublished() {
            when(rfqRepository.findById(10L)).thenReturn(Optional.of(draftRfq));
            when(rfqRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuotationRequestDto dto = service.publishRfq(10L);

            assertThat(dto.status()).isEqualTo(QuotationRequestStatus.PUBLISHED);
        }

        @Test @DisplayName("PUBLISHED RFQ → RFQ_INVALID_TRANSITION")
        void publish_published_throws() {
            when(rfqRepository.findById(11L)).thenReturn(Optional.of(publishedRfq));

            assertThatThrownBy(() -> service.publishRfq(11L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RFQ_INVALID_TRANSITION));
        }
    }

    // ── submitQuotation ───────────────────────────────────────────────────────

    @Nested @DisplayName("submitQuotation")
    class SubmitQuotation {

        @Test @DisplayName("valid submission → quotation saved")
        void submit_valid_saved() {
            var itemReq = new QuotationLineItemRequest(
                    "Server", null, BigDecimal.ONE, "unit", new BigDecimal("5000.00"), null);
            var req = new SubmitQuotationRequest(
                    new BigDecimal("5000.00"), "USD",
                    LocalDate.now().plusDays(30), null, List.of(itemReq));

            when(rfqRepository.findById(11L)).thenReturn(Optional.of(publishedRfq));
            when(quotationRepository.existsByQuotationRequestIdAndVendorId(11L, 1L)).thenReturn(false);
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
            when(quotationRepository.save(any())).thenAnswer(inv -> {
                Quotation q = inv.getArgument(0);
                q.setId(200L);
                return q;
            });

            QuotationDto dto = service.submitQuotation(11L, 1L, req);

            assertThat(dto.totalAmount()).isEqualByComparingTo("5000.00");
            assertThat(dto.status()).isEqualTo(QuotationStatus.SUBMITTED);
        }

        @Test @DisplayName("non-PUBLISHED RFQ → RFQ_NOT_PUBLISHED")
        void submit_nonPublished_throws() {
            var req = new SubmitQuotationRequest(BigDecimal.TEN, null,
                    LocalDate.now().plusDays(1), null,
                    List.of(new QuotationLineItemRequest("X", null, BigDecimal.ONE, null, BigDecimal.ONE, null)));
            when(rfqRepository.findById(10L)).thenReturn(Optional.of(draftRfq));

            assertThatThrownBy(() -> service.submitQuotation(10L, 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RFQ_NOT_PUBLISHED));
        }

        @Test @DisplayName("duplicate submission → QUOTATION_ALREADY_SUBMITTED")
        void submit_duplicate_throws() {
            var req = new SubmitQuotationRequest(BigDecimal.TEN, null,
                    LocalDate.now().plusDays(1), null,
                    List.of(new QuotationLineItemRequest("X", null, BigDecimal.ONE, null, BigDecimal.ONE, null)));
            when(rfqRepository.findById(11L)).thenReturn(Optional.of(publishedRfq));
            when(quotationRepository.existsByQuotationRequestIdAndVendorId(11L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.submitQuotation(11L, 1L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.QUOTATION_ALREADY_SUBMITTED));
        }

        @Test @DisplayName("uninvited vendor → VENDOR_NOT_INVITED")
        void submit_notInvited_throws() {
            Vendor stranger = new Vendor(); stranger.setId(99L);
            var req = new SubmitQuotationRequest(BigDecimal.TEN, null,
                    LocalDate.now().plusDays(1), null,
                    List.of(new QuotationLineItemRequest("X", null, BigDecimal.ONE, null, BigDecimal.ONE, null)));
            when(rfqRepository.findById(11L)).thenReturn(Optional.of(publishedRfq));
            when(quotationRepository.existsByQuotationRequestIdAndVendorId(11L, 99L)).thenReturn(false);
            when(vendorRepository.findById(99L)).thenReturn(Optional.of(stranger));

            assertThatThrownBy(() -> service.submitQuotation(11L, 99L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_INVITED));
        }
    }

    // ── compareQuotations ─────────────────────────────────────────────────────

    @Nested @DisplayName("compareQuotations")
    class CompareQuotations {

        @Test @DisplayName("returns quotations sorted by total amount ASC")
        void compare_returnsSorted() {
            Quotation q1 = new Quotation(); q1.setId(1L); q1.setTotalAmount(new BigDecimal("3000.00"));
            q1.setVendor(vendor); q1.setQuotationRequest(publishedRfq);
            q1.setStatus(QuotationStatus.SUBMITTED); q1.setItems(new ArrayList<>());

            Quotation q2 = new Quotation(); q2.setId(2L); q2.setTotalAmount(new BigDecimal("5000.00"));
            q2.setVendor(vendor); q2.setQuotationRequest(publishedRfq);
            q2.setStatus(QuotationStatus.SUBMITTED); q2.setItems(new ArrayList<>());

            when(rfqRepository.findById(11L)).thenReturn(Optional.of(publishedRfq));
            when(quotationRepository.findByQuotationRequestIdOrderByTotalAmountAsc(11L))
                    .thenReturn(List.of(q1, q2));

            List<QuotationDto> result = service.compareQuotations(11L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).totalAmount()).isLessThan(result.get(1).totalAmount());
        }
    }

    // ── selectQuotation ───────────────────────────────────────────────────────

    @Nested @DisplayName("selectQuotation")
    class SelectQuotation {

        @Test @DisplayName("CLOSED RFQ → winning quotation selected, others rejected")
        void select_closedRfq_selects() {
            QuotationRequest closedRfq = new QuotationRequest();
            closedRfq.setId(12L); closedRfq.setStatus(QuotationRequestStatus.CLOSED);
            closedRfq.setInvitedVendors(new ArrayList<>()); closedRfq.setQuotations(new ArrayList<>());

            Quotation winner = new Quotation();
            winner.setId(300L); winner.setQuotationRequest(closedRfq);
            winner.setVendor(vendor); winner.setStatus(QuotationStatus.SUBMITTED);
            winner.setTotalAmount(new BigDecimal("3000.00")); winner.setItems(new ArrayList<>());

            when(rfqRepository.findById(12L)).thenReturn(Optional.of(closedRfq));
            when(quotationRepository.findById(300L)).thenReturn(Optional.of(winner));
            when(quotationRepository.findByQuotationRequestIdAndStatus(12L, QuotationStatus.SUBMITTED))
                    .thenReturn(List.of(winner));
            when(quotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            QuotationDto dto = service.selectQuotation(12L, 300L);

            assertThat(dto.selected()).isTrue();
            assertThat(dto.status()).isEqualTo(QuotationStatus.ACCEPTED);
        }

        @Test @DisplayName("non-CLOSED RFQ → RFQ_INVALID_TRANSITION")
        void select_nonClosed_throws() {
            when(rfqRepository.findById(11L)).thenReturn(Optional.of(publishedRfq));

            assertThatThrownBy(() -> service.selectQuotation(11L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.RFQ_INVALID_TRANSITION));
        }
    }
}
