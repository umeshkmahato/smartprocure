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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Core business logic for Vendor lifecycle management.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD operations for vendors, contacts and ratings</li>
 *   <li>Approval / rejection / suspension workflow</li>
 *   <li>Automatic rolling average rating score maintenance</li>
 *   <li>Duplicate guard (email + taxId uniqueness)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository        vendorRepository;
    private final VendorContactRepository contactRepository;
    private final VendorRatingRepository  ratingRepository;
    private final UserRepository          userRepository;

    // ── Vendor CRUD ───────────────────────────────────────────────────────────

    /**
     * Register a new vendor. Status defaults to {@code PENDING}.
     */
    @Transactional
    public VendorDto create(CreateVendorRequest req) {
        if (vendorRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.VENDOR_ALREADY_EXISTS,
                    "A vendor with email '" + req.email() + "' already exists");
        }
        if (req.taxId() != null && vendorRepository.existsByTaxId(req.taxId())) {
            throw new BusinessException(ErrorCode.VENDOR_ALREADY_EXISTS,
                    "A vendor with tax ID '" + req.taxId() + "' already exists");
        }

        Vendor vendor = new Vendor();
        vendor.setName(req.name());
        vendor.setEmail(req.email());
        vendor.setPhone(req.phone());
        vendor.setAddress(req.address());
        vendor.setTaxId(req.taxId());
        vendor.setDescription(req.description());
        vendor.setWebsite(req.website());

        Vendor saved = vendorRepository.save(vendor);
        log.info("Vendor created: id={}, name={}", saved.getId(), saved.getName());
        return VendorDto.fromEntity(saved);
    }

    /**
     * Fetch a single vendor by ID.
     */
    public VendorDto getById(Long id) {
        return VendorDto.fromEntity(findVendorOrThrow(id));
    }

    /**
     * Paginated, filterable search across all vendors.
     */
    public PageResponse<VendorDto> search(VendorStatus status,
                                          Boolean      active,
                                          String       search,
                                          Pageable     pageable) {
        return PageResponse.from(
                vendorRepository.search(status, active, search, pageable)
                                .map(VendorDto::fromEntity));
    }

    /**
     * Partial update — only non-null fields in the request are applied.
     */
    @Transactional
    public VendorDto update(Long id, UpdateVendorRequest req) {
        Vendor vendor = findVendorOrThrow(id);

        if (req.email() != null && vendorRepository.existsByEmailAndIdNot(req.email(), id)) {
            throw new BusinessException(ErrorCode.VENDOR_ALREADY_EXISTS,
                    "Email '" + req.email() + "' is already used by another vendor");
        }
        if (req.taxId() != null && vendorRepository.existsByTaxIdAndIdNot(req.taxId(), id)) {
            throw new BusinessException(ErrorCode.VENDOR_ALREADY_EXISTS,
                    "Tax ID '" + req.taxId() + "' is already used by another vendor");
        }

        if (req.name()        != null) vendor.setName(req.name());
        if (req.email()       != null) vendor.setEmail(req.email());
        if (req.phone()       != null) vendor.setPhone(req.phone());
        if (req.address()     != null) vendor.setAddress(req.address());
        if (req.taxId()       != null) vendor.setTaxId(req.taxId());
        if (req.description() != null) vendor.setDescription(req.description());
        if (req.website()     != null) vendor.setWebsite(req.website());

        log.info("Vendor updated: id={}", id);
        return VendorDto.fromEntity(vendorRepository.save(vendor));
    }

    /**
     * Soft-delete: marks vendor as inactive rather than physically removing.
     */
    @Transactional
    public void deactivate(Long id) {
        Vendor vendor = findVendorOrThrow(id);
        vendor.setActive(false);
        vendor.setStatus(VendorStatus.INACTIVE);
        vendorRepository.save(vendor);
        log.info("Vendor deactivated: id={}", id);
    }

    // ── Approval Workflow ────────────────────────────────────────────────────

    /**
     * Approve a pending vendor.
     */
    @Transactional
    public VendorDto approve(Long id) {
        Vendor vendor = findVendorOrThrow(id);
        if (vendor.getStatus() == VendorStatus.APPROVED) {
            throw new BusinessException(ErrorCode.VENDOR_ALREADY_APPROVED);
        }
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setActive(true);
        log.info("Vendor approved: id={}", id);
        return VendorDto.fromEntity(vendorRepository.save(vendor));
    }

    /**
     * Reject a pending vendor.
     */
    @Transactional
    public VendorDto reject(Long id) {
        Vendor vendor = findVendorOrThrow(id);
        if (vendor.getStatus() != VendorStatus.PENDING) {
            throw new BusinessException(ErrorCode.VENDOR_NOT_FOUND,
                    "Only PENDING vendors can be rejected");
        }
        vendor.setStatus(VendorStatus.REJECTED);
        log.info("Vendor rejected: id={}", id);
        return VendorDto.fromEntity(vendorRepository.save(vendor));
    }

    /**
     * Suspend an approved vendor.
     */
    @Transactional
    public VendorDto suspend(Long id) {
        Vendor vendor = findVendorOrThrow(id);
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new BusinessException(ErrorCode.VENDOR_NOT_APPROVED,
                    "Only APPROVED vendors can be suspended");
        }
        vendor.setStatus(VendorStatus.SUSPENDED);
        log.info("Vendor suspended: id={}", id);
        return VendorDto.fromEntity(vendorRepository.save(vendor));
    }

    // ── Contacts ─────────────────────────────────────────────────────────────

    public List<VendorContactDto> getContacts(Long vendorId) {
        findVendorOrThrow(vendorId);
        return contactRepository.findByVendorId(vendorId)
                                .stream()
                                .map(VendorContactDto::fromEntity)
                                .toList();
    }

    @Transactional
    public VendorContactDto addContact(Long vendorId, AddContactRequest req) {
        Vendor vendor = findVendorOrThrow(vendorId);

        // If this contact is primary, demote all existing primary contacts first
        if (req.primary()) {
            contactRepository.clearPrimaryForVendor(vendorId);
        }

        VendorContact contact = VendorContact.builder()
                .vendor(vendor)
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .position(req.position())
                .primary(req.primary())
                .build();

        VendorContact saved = contactRepository.save(contact);
        log.info("Contact added: vendorId={}, contactId={}", vendorId, saved.getId());
        return VendorContactDto.fromEntity(saved);
    }

    @Transactional
    public void removeContact(Long vendorId, Long contactId) {
        findVendorOrThrow(vendorId);
        VendorContact contact = contactRepository.findByIdAndVendorId(contactId, vendorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VENDOR_CONTACT_NOT_FOUND));
        contactRepository.delete(contact);
        log.info("Contact removed: vendorId={}, contactId={}", vendorId, contactId);
    }

    // ── Ratings ───────────────────────────────────────────────────────────────

    public PageResponse<VendorRatingDto> getRatings(Long vendorId, Pageable pageable) {
        findVendorOrThrow(vendorId);
        return PageResponse.from(
                ratingRepository.findByVendorId(vendorId, pageable)
                                .map(VendorRatingDto::fromEntity));
    }

    @Transactional
    public VendorRatingDto addRating(Long vendorId, AddRatingRequest req, Authentication auth) {
        Vendor vendor = findVendorOrThrow(vendorId);

        if (!vendor.isApproved()) {
            throw new BusinessException(ErrorCode.VENDOR_NOT_APPROVED,
                    "Ratings can only be added to APPROVED vendors");
        }

        User rater = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VendorRating rating = VendorRating.builder()
                .vendor(vendor)
                .ratedBy(rater)
                .score(req.score())
                .comment(req.comment())
                .build();

        ratingRepository.save(rating);

        // Recalculate rolling average and persist on the vendor record
        double avg = ratingRepository.findAverageScoreByVendorId(vendorId).orElse((double) req.score());
        vendor.setRatingScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        vendorRepository.save(vendor);

        log.info("Rating added: vendorId={}, score={}, rater={}", vendorId, req.score(), rater.getUsername());
        return VendorRatingDto.fromEntity(rating);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Vendor findVendorOrThrow(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VENDOR_NOT_FOUND,
                        "Vendor with id " + id + " not found"));
    }
}
