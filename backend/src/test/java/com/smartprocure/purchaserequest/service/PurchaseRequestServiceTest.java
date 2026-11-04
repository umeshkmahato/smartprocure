package com.smartprocure.purchaserequest.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.purchaserequest.dto.*;
import com.smartprocure.purchaserequest.entity.*;
import com.smartprocure.purchaserequest.repository.PurchaseRequestItemRepository;
import com.smartprocure.purchaserequest.repository.PurchaseRequestRepository;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import com.smartprocure.vendor.repository.VendorRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseRequestService")
class PurchaseRequestServiceTest {

    @Mock private PurchaseRequestRepository     prRepository;
    @Mock private PurchaseRequestItemRepository itemRepository;
    @Mock private UserRepository                userRepository;
    @Mock private VendorRepository              vendorRepository;
    @Mock private Authentication                auth;

    @InjectMocks private PurchaseRequestService prService;

    private User   requester;
    private PurchaseRequest draftPr;
    private PurchaseRequest submittedPr;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setUsername("buyer1");
        requester.setFirstName("John");
        requester.setLastName("Buyer");

        draftPr = new PurchaseRequest();
        draftPr.setId(10L);
        draftPr.setPrNumber("PR-20250101-00001");
        draftPr.setTitle("Office Supplies");
        draftPr.setStatus(PurchaseRequestStatus.DRAFT);
        draftPr.setPriority(PurchaseRequestPriority.MEDIUM);
        draftPr.setRequester(requester);
        draftPr.setTotalAmount(BigDecimal.ZERO);
        draftPr.setCurrency("USD");
        draftPr.setItems(new java.util.ArrayList<>());

        submittedPr = new PurchaseRequest();
        submittedPr.setId(11L);
        submittedPr.setPrNumber("PR-20250101-00002");
        submittedPr.setTitle("IT Equipment");
        submittedPr.setStatus(PurchaseRequestStatus.SUBMITTED);
        submittedPr.setPriority(PurchaseRequestPriority.HIGH);
        submittedPr.setRequester(requester);
        submittedPr.setTotalAmount(new BigDecimal("5000.00"));
        submittedPr.setCurrency("USD");
        submittedPr.setItems(new java.util.ArrayList<>());
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("create")
    class Create {

        @Test @DisplayName("valid request → PR saved with DRAFT status and correct total")
        void create_valid_savesDraftPr() {
            var itemReq = new LineItemRequest("Pen", null,
                    BigDecimal.TEN, "box", new BigDecimal("2.50"), null, null);
            var req = new CreatePurchaseRequestRequest(
                    "Office Supplies", null, PurchaseRequestPriority.LOW,
                    "Finance", null, "USD", null, List.of(itemReq));

            when(auth.getName()).thenReturn("buyer1");
            when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(requester));
            when(prRepository.count()).thenReturn(0L);
            when(prRepository.save(any())).thenAnswer(inv -> {
                PurchaseRequest pr = inv.getArgument(0);
                pr.setId(100L);
                return pr;
            });

            PurchaseRequestDto dto = prService.create(req, auth);

            assertThat(dto.status()).isEqualTo(PurchaseRequestStatus.DRAFT);
            assertThat(dto.title()).isEqualTo("Office Supplies");
            assertThat(dto.totalAmount()).isEqualByComparingTo("25.00");
            verify(prRepository).save(any());
        }

        @Test @DisplayName("unknown user → USER_NOT_FOUND")
        void create_unknownUser_throws() {
            var itemReq = new LineItemRequest("X", null, BigDecimal.ONE, null, BigDecimal.ONE, null, null);
            var req = new CreatePurchaseRequestRequest("T", null, null, null, null, null, null, List.of(itemReq));
            when(auth.getName()).thenReturn("ghost");
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> prService.create(req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("getById")
    class GetById {

        @Test @DisplayName("existing id → returns DTO")
        void getById_found_returnsDto() {
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));
            PurchaseRequestDto dto = prService.getById(10L);
            assertThat(dto.id()).isEqualTo(10L);
            assertThat(dto.prNumber()).isEqualTo("PR-20250101-00001");
        }

        @Test @DisplayName("missing id → PR_NOT_FOUND")
        void getById_notFound_throws() {
            when(prRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> prService.getById(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_NOT_FOUND));
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("update")
    class Update {

        @Test @DisplayName("DRAFT PR + owner → updates allowed fields")
        void update_draftOwner_succeeds() {
            var req = new UpdatePurchaseRequestRequest(
                    "Updated Title", null, PurchaseRequestPriority.HIGH,
                    null, null, null, null, null);
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));
            when(prRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseRequestDto dto = prService.update(10L, req, auth);

            assertThat(dto.title()).isEqualTo("Updated Title");
            assertThat(dto.priority()).isEqualTo(PurchaseRequestPriority.HIGH);
        }

        @Test @DisplayName("non-DRAFT PR → PR_NOT_EDITABLE")
        void update_nonDraft_throws() {
            var req = new UpdatePurchaseRequestRequest("X", null, null, null, null, null, null, null);
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(11L)).thenReturn(Optional.of(submittedPr));

            assertThatThrownBy(() -> prService.update(11L, req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_NOT_EDITABLE));
        }
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("submit")
    class Submit {

        @Test @DisplayName("DRAFT PR + owner → becomes SUBMITTED")
        void submit_draft_becomesSubmitted() {
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));
            when(prRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseRequestDto dto = prService.submit(10L, auth);

            assertThat(dto.status()).isEqualTo(PurchaseRequestStatus.SUBMITTED);
        }

        @Test @DisplayName("non-DRAFT PR → PR_INVALID_TRANSITION")
        void submit_nonDraft_throws() {
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(11L)).thenReturn(Optional.of(submittedPr));

            assertThatThrownBy(() -> prService.submit(11L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_INVALID_TRANSITION));
        }
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("approve")
    class Approve {

        @Test @DisplayName("SUBMITTED PR → becomes APPROVED")
        void approve_submitted_becomesApproved() {
            when(prRepository.findById(11L)).thenReturn(Optional.of(submittedPr));
            when(prRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseRequestDto dto = prService.approve(11L);

            assertThat(dto.status()).isEqualTo(PurchaseRequestStatus.APPROVED);
        }

        @Test @DisplayName("DRAFT PR → PR_INVALID_TRANSITION")
        void approve_draft_throws() {
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));

            assertThatThrownBy(() -> prService.approve(10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_INVALID_TRANSITION));
        }
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("reject")
    class Reject {

        @Test @DisplayName("SUBMITTED PR → becomes REJECTED with reason")
        void reject_submitted_becomesRejected() {
            var req = new RejectPurchaseRequestRequest("Budget exceeded");
            when(prRepository.findById(11L)).thenReturn(Optional.of(submittedPr));
            when(prRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseRequestDto dto = prService.reject(11L, req);

            assertThat(dto.status()).isEqualTo(PurchaseRequestStatus.REJECTED);
            assertThat(dto.rejectionReason()).isEqualTo("Budget exceeded");
        }

        @Test @DisplayName("DRAFT PR → PR_INVALID_TRANSITION")
        void reject_draft_throws() {
            var req = new RejectPurchaseRequestRequest("No budget");
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));

            assertThatThrownBy(() -> prService.reject(10L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_INVALID_TRANSITION));
        }
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("cancel")
    class Cancel {

        @Test @DisplayName("DRAFT PR + owner → becomes CANCELLED")
        void cancel_draft_becomesCancelled() {
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));
            when(prRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseRequestDto dto = prService.cancel(10L, auth);

            assertThat(dto.status()).isEqualTo(PurchaseRequestStatus.CANCELLED);
        }

        @Test @DisplayName("APPROVED PR → PR_INVALID_TRANSITION (not cancellable)")
        void cancel_approved_throws() {
            PurchaseRequest approvedPr = new PurchaseRequest();
            approvedPr.setId(12L);
            approvedPr.setStatus(PurchaseRequestStatus.APPROVED);
            approvedPr.setRequester(requester);
            approvedPr.setItems(new java.util.ArrayList<>());

            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(12L)).thenReturn(Optional.of(approvedPr));

            assertThatThrownBy(() -> prService.cancel(12L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_INVALID_TRANSITION));
        }
    }

    // ── line items ────────────────────────────────────────────────────────────

    @Nested @DisplayName("line items")
    class LineItems {

        @Test @DisplayName("addItem to DRAFT PR → item added, total recalculated")
        void addItem_draft_succeeds() {
            var req = new LineItemRequest("Laptop", null,
                    BigDecimal.ONE, "unit", new BigDecimal("1200.00"), null, null);
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));
            when(prRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseRequestDto dto = prService.addItem(10L, req, auth);

            assertThat(dto.items()).hasSize(1);
            assertThat(dto.totalAmount()).isEqualByComparingTo("1200.00");
        }

        @Test @DisplayName("addItem to SUBMITTED PR → PR_NOT_EDITABLE")
        void addItem_submitted_throws() {
            var req = new LineItemRequest("X", null, BigDecimal.ONE, null, BigDecimal.ONE, null, null);
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(11L)).thenReturn(Optional.of(submittedPr));

            assertThatThrownBy(() -> prService.addItem(11L, req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_NOT_EDITABLE));
        }

        @Test @DisplayName("removeItem: item not found → PR_ITEM_NOT_FOUND")
        void removeItem_notFound_throws() {
            when(auth.getName()).thenReturn("buyer1");
            when(auth.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(prRepository.findById(10L)).thenReturn(Optional.of(draftPr));
            when(itemRepository.findByIdAndPurchaseRequestId(99L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> prService.removeItem(10L, 99L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_ITEM_NOT_FOUND));
        }
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("search")
    class Search {

        @Test @DisplayName("search returns paginated results")
        void search_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(prRepository.search(null, null, null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(draftPr)));

            var page = prService.search(null, null, null, null, pageable);

            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).title()).isEqualTo("Office Supplies");
        }
    }
}
