package com.smartprocure.approval.service;

import com.smartprocure.approval.dto.ActionApprovalRequest;
import com.smartprocure.approval.dto.ActionApprovalRequest.ApprovalAction;
import com.smartprocure.approval.dto.ApprovalRequestDto;
import com.smartprocure.approval.dto.CreateDelegationRequest;
import com.smartprocure.approval.entity.*;
import com.smartprocure.approval.repository.*;
import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.purchaserequest.entity.PurchaseRequest;
import com.smartprocure.purchaserequest.entity.PurchaseRequestStatus;
import com.smartprocure.purchaserequest.repository.PurchaseRequestRepository;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
@DisplayName("ApprovalWorkflowService")
class ApprovalWorkflowServiceTest {

    @Mock private ApprovalRequestRepository    approvalRequestRepo;
    @Mock private ApprovalThresholdRepository  thresholdRepo;
    @Mock private ApprovalDelegationRepository delegationRepo;
    @Mock private PurchaseRequestRepository    prRepo;
    @Mock private UserRepository               userRepo;
    @Mock private Authentication               auth;

    @InjectMocks private ApprovalWorkflowService service;

    private User            manager;
    private PurchaseRequest submittedPr;
    private ApprovalRequest pendingStep;
    private ApprovalThreshold threshold;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setUsername("manager1");
        manager.setFirstName("Alice");
        manager.setLastName("Manager");

        submittedPr = new PurchaseRequest();
        submittedPr.setId(10L);
        submittedPr.setPrNumber("PR-20250101-00001");
        submittedPr.setTitle("Office Supplies");
        submittedPr.setStatus(PurchaseRequestStatus.SUBMITTED);
        submittedPr.setTotalAmount(new BigDecimal("500.00"));
        submittedPr.setItems(new ArrayList<>());

        threshold = ApprovalThreshold.builder()
                .id(1L).level(1)
                .minAmount(BigDecimal.ZERO)
                .maxAmount(new BigDecimal("999.99"))
                .roleRequired("ROLE_MANAGER")
                .description("Manager approval")
                .active(true)
                .build();

        pendingStep = ApprovalRequest.builder()
                .id(100L)
                .purchaseRequest(submittedPr)
                .threshold(threshold)
                .level(1)
                .status(ApprovalStatus.PENDING)
                .build();
    }

    // ── initiateWorkflow ──────────────────────────────────────────────────────

    @Nested @DisplayName("initiateWorkflow")
    class InitiateWorkflow {

        @Test @DisplayName("SUBMITTED PR with matching threshold → steps created, PR moves to UNDER_REVIEW")
        void initiate_submittedPr_createsSteps() {
            when(prRepo.findById(10L)).thenReturn(Optional.of(submittedPr));
            when(thresholdRepo.findApplicableThresholds(new BigDecimal("500.00")))
                    .thenReturn(List.of(threshold));
            when(approvalRequestRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(prRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ApprovalRequestDto> steps = service.initiateWorkflow(10L);

            assertThat(steps).hasSize(1);
            assertThat(submittedPr.getStatus()).isEqualTo(PurchaseRequestStatus.UNDER_REVIEW);
            verify(prRepo).save(submittedPr);
        }

        @Test @DisplayName("No matching threshold → PR auto-approved")
        void initiate_noThreshold_autoApproves() {
            when(prRepo.findById(10L)).thenReturn(Optional.of(submittedPr));
            when(thresholdRepo.findApplicableThresholds(any())).thenReturn(List.of());
            when(prRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ApprovalRequestDto> steps = service.initiateWorkflow(10L);

            assertThat(steps).isEmpty();
            assertThat(submittedPr.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
        }

        @Test @DisplayName("Non-SUBMITTED PR → PR_INVALID_TRANSITION")
        void initiate_nonSubmitted_throws() {
            submittedPr.setStatus(PurchaseRequestStatus.DRAFT);
            when(prRepo.findById(10L)).thenReturn(Optional.of(submittedPr));

            assertThatThrownBy(() -> service.initiateWorkflow(10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PR_INVALID_TRANSITION));
        }
    }

    // ── action ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("action")
    class Action {

        @Test @DisplayName("APPROVE last step → PR moves to APPROVED")
        void action_approveLastStep_prApproved() {
            when(approvalRequestRepo.findById(100L)).thenReturn(Optional.of(pendingStep));
            when(auth.getName()).thenReturn("manager1");
            when(userRepo.findByUsername("manager1")).thenReturn(Optional.of(manager));
            when(approvalRequestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRequestRepo.countApprovedSteps(10L)).thenReturn(1L);
            when(approvalRequestRepo.countTotalSteps(10L)).thenReturn(1L);
            when(prRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var req = new ActionApprovalRequest(ApprovalAction.APPROVED, "Looks good");
            ApprovalRequestDto dto = service.action(100L, req, auth);

            assertThat(dto.status()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(submittedPr.getStatus()).isEqualTo(PurchaseRequestStatus.APPROVED);
        }

        @Test @DisplayName("REJECT step → PR moves to REJECTED, remaining steps SKIPPED")
        void action_rejectStep_prRejected() {
            when(approvalRequestRepo.findById(100L)).thenReturn(Optional.of(pendingStep));
            when(auth.getName()).thenReturn("manager1");
            when(userRepo.findByUsername("manager1")).thenReturn(Optional.of(manager));
            when(approvalRequestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(approvalRequestRepo.findByPurchaseRequestIdAndStatus(10L, ApprovalStatus.PENDING))
                    .thenReturn(List.of());
            when(approvalRequestRepo.saveAll(anyList())).thenReturn(List.of());
            when(prRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var req = new ActionApprovalRequest(ApprovalAction.REJECTED, "Over budget");
            ApprovalRequestDto dto = service.action(100L, req, auth);

            assertThat(dto.status()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(submittedPr.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED);
            assertThat(submittedPr.getRejectionReason()).isEqualTo("Over budget");
        }

        @Test @DisplayName("Already actioned step → APPROVAL_ALREADY_ACTIONED")
        void action_alreadyActioned_throws() {
            pendingStep.setStatus(ApprovalStatus.APPROVED);
            when(approvalRequestRepo.findById(100L)).thenReturn(Optional.of(pendingStep));

            assertThatThrownBy(() -> service.action(100L,
                    new ActionApprovalRequest(ApprovalAction.APPROVED, null), auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.APPROVAL_ALREADY_ACTIONED));
        }

        @Test @DisplayName("Step not found → APPROVAL_NOT_FOUND")
        void action_notFound_throws() {
            when(approvalRequestRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.action(999L,
                    new ActionApprovalRequest(ApprovalAction.APPROVED, null), auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.APPROVAL_NOT_FOUND));
        }
    }

    // ── delegation ────────────────────────────────────────────────────────────

    @Nested @DisplayName("delegation")
    class Delegation {

        @Test @DisplayName("valid delegation → saved and returned")
        void createDelegation_valid_saved() {
            User delegate = new User();
            delegate.setId(2L);
            delegate.setUsername("delegate1");
            delegate.setFirstName("Bob");
            delegate.setLastName("Delegate");

            when(auth.getName()).thenReturn("manager1");
            when(userRepo.findByUsername("manager1")).thenReturn(Optional.of(manager));
            when(userRepo.findById(2L)).thenReturn(Optional.of(delegate));
            when(delegationRepo.hasOverlappingDelegation(anyLong(), any(), any())).thenReturn(false);
            when(delegationRepo.save(any())).thenAnswer(inv -> {
                ApprovalDelegation d = inv.getArgument(0);
                d.setId(50L);
                return d;
            });

            var req = new CreateDelegationRequest(2L,
                    LocalDate.now(), LocalDate.now().plusDays(7), "On leave");
            var dto = service.createDelegation(req, auth);

            assertThat(dto.id()).isEqualTo(50L);
            assertThat(dto.active()).isTrue();
        }

        @Test @DisplayName("self-delegation → APPROVAL_SELF_DELEGATION")
        void createDelegation_selfDelegate_throws() {
            when(auth.getName()).thenReturn("manager1");
            when(userRepo.findByUsername("manager1")).thenReturn(Optional.of(manager));
            when(userRepo.findById(1L)).thenReturn(Optional.of(manager));

            var req = new CreateDelegationRequest(1L,
                    LocalDate.now(), LocalDate.now().plusDays(1), null);

            assertThatThrownBy(() -> service.createDelegation(req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.APPROVAL_SELF_DELEGATION));
        }

        @Test @DisplayName("overlapping delegation → APPROVAL_DELEGATION_OVERLAP")
        void createDelegation_overlap_throws() {
            User delegate = new User();
            delegate.setId(2L);
            delegate.setUsername("delegate1");

            when(auth.getName()).thenReturn("manager1");
            when(userRepo.findByUsername("manager1")).thenReturn(Optional.of(manager));
            when(userRepo.findById(2L)).thenReturn(Optional.of(delegate));
            when(delegationRepo.hasOverlappingDelegation(anyLong(), any(), any())).thenReturn(true);

            var req = new CreateDelegationRequest(2L,
                    LocalDate.now(), LocalDate.now().plusDays(5), null);

            assertThatThrownBy(() -> service.createDelegation(req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.APPROVAL_DELEGATION_OVERLAP));
        }

        @Test @DisplayName("revoke own delegation → marked inactive")
        void revokeDelegation_own_deactivates() {
            ApprovalDelegation d = ApprovalDelegation.builder()
                    .id(50L).delegator(manager).active(true)
                    .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(5))
                    .build();

            when(delegationRepo.findById(50L)).thenReturn(Optional.of(d));
            when(auth.getName()).thenReturn("manager1");
            when(delegationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var dto = service.revokeDelegation(50L, auth);

            assertThat(dto.active()).isFalse();
        }
    }

    // ── thresholds ────────────────────────────────────────────────────────────

    @Nested @DisplayName("thresholds")
    class Thresholds {

        @Test @DisplayName("getThresholds → returns active thresholds ordered by level")
        void getThresholds_returnsActive() {
            when(thresholdRepo.findByActiveTrueOrderByLevelAsc()).thenReturn(List.of(threshold));
            var list = service.getThresholds();
            assertThat(list).hasSize(1);
            assertThat(list.get(0).level()).isEqualTo(1);
        }
    }
}
