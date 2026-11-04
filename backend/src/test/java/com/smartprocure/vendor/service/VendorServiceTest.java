package com.smartprocure.vendor.service;

import com.smartprocure.common.exception.BusinessException;
import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import com.smartprocure.vendor.dto.AddContactRequest;
import com.smartprocure.vendor.dto.AddRatingRequest;
import com.smartprocure.vendor.dto.CreateVendorRequest;
import com.smartprocure.vendor.dto.UpdateVendorRequest;
import com.smartprocure.vendor.dto.VendorContactDto;
import com.smartprocure.vendor.dto.VendorDto;
import com.smartprocure.vendor.dto.VendorRatingDto;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.entity.VendorContact;
import com.smartprocure.vendor.entity.VendorRating;
import com.smartprocure.vendor.entity.VendorStatus;
import com.smartprocure.vendor.repository.VendorContactRepository;
import com.smartprocure.vendor.repository.VendorRatingRepository;
import com.smartprocure.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorService")
class VendorServiceTest {

    @Mock private VendorRepository        vendorRepository;
    @Mock private VendorContactRepository contactRepository;
    @Mock private VendorRatingRepository  ratingRepository;
    @Mock private UserRepository          userRepository;
    @Mock private Authentication          authentication;

    @InjectMocks private VendorService vendorService;

    private Vendor pendingVendor;
    private Vendor approvedVendor;
    private User   rater;

    @BeforeEach
    void setUp() {
        pendingVendor = new Vendor();
        pendingVendor.setId(1L);
        pendingVendor.setName("Acme Corp");
        pendingVendor.setEmail("acme@example.com");
        pendingVendor.setStatus(VendorStatus.PENDING);
        pendingVendor.setActive(true);
        pendingVendor.setRatingScore(BigDecimal.ZERO);

        approvedVendor = new Vendor();
        approvedVendor.setId(2L);
        approvedVendor.setName("Beta Ltd");
        approvedVendor.setEmail("beta@example.com");
        approvedVendor.setStatus(VendorStatus.APPROVED);
        approvedVendor.setActive(true);
        approvedVendor.setRatingScore(BigDecimal.ZERO);

        rater = new User();
        rater.setId(10L);
        rater.setUsername("buyer1");
        rater.setFirstName("John");
        rater.setLastName("Buyer");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("create")
    class Create {

        @Test @DisplayName("valid request → vendor saved and returned as DTO")
        void create_valid_returnsSavedDto() {
            var req = new CreateVendorRequest(
                    "Acme Corp", "acme@example.com", "+1234567890",
                    "123 Main St", "TAX-001", "Great vendor", "https://acme.com");

            when(vendorRepository.existsByEmail(req.email())).thenReturn(false);
            when(vendorRepository.existsByTaxId(req.taxId())).thenReturn(false);
            when(vendorRepository.save(any())).thenAnswer(inv -> {
                Vendor v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });

            VendorDto dto = vendorService.create(req);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Acme Corp");
            assertThat(dto.status()).isEqualTo(VendorStatus.PENDING);
        }

        @Test @DisplayName("duplicate email → VENDOR_ALREADY_EXISTS")
        void create_duplicateEmail_throws() {
            var req = new CreateVendorRequest(
                    "Acme Corp", "acme@example.com", null, null, null, null, null);
            when(vendorRepository.existsByEmail("acme@example.com")).thenReturn(true);

            assertThatThrownBy(() -> vendorService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_ALREADY_EXISTS));
        }

        @Test @DisplayName("duplicate taxId → VENDOR_ALREADY_EXISTS")
        void create_duplicateTaxId_throws() {
            var req = new CreateVendorRequest(
                    "New Corp", "new@example.com", null, null, "DUPE-TAX", null, null);
            when(vendorRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(vendorRepository.existsByTaxId("DUPE-TAX")).thenReturn(true);

            assertThatThrownBy(() -> vendorService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_ALREADY_EXISTS));
        }
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("getById")
    class GetById {

        @Test @DisplayName("existing id → returns DTO")
        void getById_found_returnsDto() {
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));

            VendorDto dto = vendorService.getById(1L);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Acme Corp");
        }

        @Test @DisplayName("missing id → VENDOR_NOT_FOUND")
        void getById_notFound_throws() {
            when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vendorService.getById(99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_FOUND));
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("update")
    class Update {

        @Test @DisplayName("partial update → only non-null fields changed")
        void update_partialFields_onlyChangesProvided() {
            var req = new UpdateVendorRequest("New Name", null, null, null, null, null, null);
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(vendorRepository.existsByEmailAndIdNot(anyString(), anyLong())).thenReturn(false);
            when(vendorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VendorDto dto = vendorService.update(1L, req);

            assertThat(dto.name()).isEqualTo("New Name");
            assertThat(dto.email()).isEqualTo("acme@example.com"); // unchanged
        }

        @Test @DisplayName("email clash with another vendor → VENDOR_ALREADY_EXISTS")
        void update_emailClash_throws() {
            var req = new UpdateVendorRequest(null, "taken@example.com", null, null, null, null, null);
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(vendorRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> vendorService.update(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_ALREADY_EXISTS));
        }
    }

    // ── approve / reject / suspend ────────────────────────────────────────────

    @Nested @DisplayName("approval workflow")
    class ApprovalWorkflow {

        @Test @DisplayName("approve PENDING vendor → status becomes APPROVED")
        void approve_pending_setsApproved() {
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(vendorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VendorDto dto = vendorService.approve(1L);

            assertThat(dto.status()).isEqualTo(VendorStatus.APPROVED);
        }

        @Test @DisplayName("approve already-APPROVED vendor → VENDOR_ALREADY_APPROVED")
        void approve_alreadyApproved_throws() {
            when(vendorRepository.findById(2L)).thenReturn(Optional.of(approvedVendor));

            assertThatThrownBy(() -> vendorService.approve(2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_ALREADY_APPROVED));
        }

        @Test @DisplayName("reject PENDING vendor → status becomes REJECTED")
        void reject_pending_setsRejected() {
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(vendorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VendorDto dto = vendorService.reject(1L);

            assertThat(dto.status()).isEqualTo(VendorStatus.REJECTED);
        }

        @Test @DisplayName("reject non-PENDING vendor → BusinessException")
        void reject_nonPending_throws() {
            when(vendorRepository.findById(2L)).thenReturn(Optional.of(approvedVendor));

            assertThatThrownBy(() -> vendorService.reject(2L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test @DisplayName("suspend APPROVED vendor → status becomes SUSPENDED")
        void suspend_approved_setsSuspended() {
            when(vendorRepository.findById(2L)).thenReturn(Optional.of(approvedVendor));
            when(vendorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VendorDto dto = vendorService.suspend(2L);

            assertThat(dto.status()).isEqualTo(VendorStatus.SUSPENDED);
        }

        @Test @DisplayName("suspend non-APPROVED vendor → VENDOR_NOT_APPROVED")
        void suspend_nonApproved_throws() {
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));

            assertThatThrownBy(() -> vendorService.suspend(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_APPROVED));
        }
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Nested @DisplayName("deactivate")
    class Deactivate {

        @Test @DisplayName("existing vendor → marked inactive")
        void deactivate_existing_setsInactive() {
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(vendorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            vendorService.deactivate(1L);

            assertThat(pendingVendor.isActive()).isFalse();
            assertThat(pendingVendor.getStatus()).isEqualTo(VendorStatus.INACTIVE);
        }
    }

    // ── contacts ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("contacts")
    class Contacts {

        @Test @DisplayName("addContact: primary=true clears previous primary first")
        void addContact_primary_clearsPrevious() {
            var req = new AddContactRequest("Jane", "jane@acme.com", null, "Manager", true);
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(contactRepository.save(any())).thenAnswer(inv -> {
                VendorContact c = inv.getArgument(0);
                c.setId(5L);
                return c;
            });

            VendorContactDto dto = vendorService.addContact(1L, req);

            verify(contactRepository).clearPrimaryForVendor(1L);
            assertThat(dto.name()).isEqualTo("Jane");
            assertThat(dto.primary()).isTrue();
        }

        @Test @DisplayName("addContact: primary=false does NOT clear other primaries")
        void addContact_nonPrimary_doesNotClear() {
            var req = new AddContactRequest("Bob", "bob@acme.com", null, "Sales", false);
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(contactRepository.save(any())).thenAnswer(inv -> {
                VendorContact c = inv.getArgument(0);
                c.setId(6L);
                return c;
            });

            vendorService.addContact(1L, req);

            verify(contactRepository, never()).clearPrimaryForVendor(anyLong());
        }

        @Test @DisplayName("removeContact: not found → VENDOR_CONTACT_NOT_FOUND")
        void removeContact_notFound_throws() {
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));
            when(contactRepository.findByIdAndVendorId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vendorService.removeContact(1L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_CONTACT_NOT_FOUND));
        }
    }

    // ── ratings ───────────────────────────────────────────────────────────────

    @Nested @DisplayName("ratings")
    class Ratings {

        @Test @DisplayName("addRating: approved vendor + authenticated user → rating saved and avg updated")
        void addRating_valid_savesRatingAndUpdatesAvg() {
            var req = new AddRatingRequest(4, "Good service");
            when(vendorRepository.findById(2L)).thenReturn(Optional.of(approvedVendor));
            when(authentication.getName()).thenReturn("buyer1");
            when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(rater));
            when(ratingRepository.save(any())).thenAnswer(inv -> {
                VendorRating r = inv.getArgument(0);
                r.setId(100L);
                return r;
            });
            when(ratingRepository.findAverageScoreByVendorId(2L)).thenReturn(Optional.of(4.0));
            when(vendorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VendorRatingDto dto = vendorService.addRating(2L, req, authentication);

            assertThat(dto.score()).isEqualTo(4);
            assertThat(dto.comment()).isEqualTo("Good service");
            assertThat(approvedVendor.getRatingScore()).isEqualByComparingTo("4.00");
        }

        @Test @DisplayName("addRating: non-approved vendor → VENDOR_NOT_APPROVED")
        void addRating_notApproved_throws() {
            var req = new AddRatingRequest(3, null);
            when(vendorRepository.findById(1L)).thenReturn(Optional.of(pendingVendor));

            assertThatThrownBy(() -> vendorService.addRating(1L, req, authentication))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.VENDOR_NOT_APPROVED));
        }

        @Test @DisplayName("getRatings: returns paginated DTO list")
        void getRatings_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            VendorRating rating = VendorRating.builder()
                    .id(1L).vendor(approvedVendor).ratedBy(rater).score(5).comment("Excellent").build();

            when(vendorRepository.findById(2L)).thenReturn(Optional.of(approvedVendor));
            when(ratingRepository.findByVendorId(2L, pageable))
                    .thenReturn(new PageImpl<>(List.of(rating)));

            PageResponse<VendorRatingDto> page = vendorService.getRatings(2L, pageable);

            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).score()).isEqualTo(5);
        }
    }
}
