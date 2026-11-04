package com.smartprocure.vendor.controller;

import com.smartprocure.common.response.ApiResponse;
import com.smartprocure.common.response.PageResponse;
import com.smartprocure.vendor.dto.AddContactRequest;
import com.smartprocure.vendor.dto.AddRatingRequest;
import com.smartprocure.vendor.dto.CreateVendorRequest;
import com.smartprocure.vendor.dto.UpdateVendorRequest;
import com.smartprocure.vendor.dto.VendorContactDto;
import com.smartprocure.vendor.dto.VendorDto;
import com.smartprocure.vendor.dto.VendorRatingDto;
import com.smartprocure.vendor.entity.VendorStatus;
import com.smartprocure.vendor.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for Vendor lifecycle management.
 *
 * <pre>
 * POST   /api/v1/vendors                           → create vendor
 * GET    /api/v1/vendors/{id}                       → get vendor
 * GET    /api/v1/vendors                            → search vendors (paginated)
 * PUT    /api/v1/vendors/{id}                       → update vendor
 * DELETE /api/v1/vendors/{id}                       → deactivate vendor
 *
 * PATCH  /api/v1/vendors/{id}/approve               → approve vendor (ADMIN)
 * PATCH  /api/v1/vendors/{id}/reject                → reject vendor  (ADMIN)
 * PATCH  /api/v1/vendors/{id}/suspend               → suspend vendor (ADMIN)
 *
 * GET    /api/v1/vendors/{id}/contacts              → list contacts
 * POST   /api/v1/vendors/{id}/contacts              → add contact
 * DELETE /api/v1/vendors/{id}/contacts/{contactId}  → remove contact
 *
 * GET    /api/v1/vendors/{id}/ratings               → list ratings (paginated)
 * POST   /api/v1/vendors/{id}/ratings               → add rating
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    // ── Vendor CRUD ───────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<VendorDto>> create(
            @Valid @RequestBody CreateVendorRequest req) {

        VendorDto dto = vendorService.create(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Vendor registered successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VendorDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<VendorDto>>> search(
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) Boolean      active,
            @RequestParam(required = false) String       search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        return ResponseEntity.ok(
                ApiResponse.success(vendorService.search(status, active, search, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<VendorDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVendorRequest req) {

        return ResponseEntity.ok(
                ApiResponse.success(vendorService.update(id, req), "Vendor updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        vendorService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vendor deactivated successfully"));
    }

    // ── Approval Workflow ─────────────────────────────────────────────────────

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(vendorService.approve(id), "Vendor approved successfully"));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorDto>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(vendorService.reject(id), "Vendor rejected"));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorDto>> suspend(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(vendorService.suspend(id), "Vendor suspended"));
    }

    // ── Contacts ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/contacts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<VendorContactDto>>> getContacts(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success(vendorService.getContacts(id)));
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<VendorContactDto>> addContact(
            @PathVariable Long id,
            @Valid @RequestBody AddContactRequest req) {

        VendorContactDto dto = vendorService.addContact(id, req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Contact added successfully"));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> removeContact(
            @PathVariable Long id,
            @PathVariable Long contactId) {

        vendorService.removeContact(id, contactId);
        return ResponseEntity.ok(ApiResponse.success(null, "Contact removed successfully"));
    }

    // ── Ratings ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}/ratings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<VendorRatingDto>>> getRatings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(vendorService.getRatings(id, pageable)));
    }

    @PostMapping("/{id}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VendorRatingDto>> addRating(
            @PathVariable Long id,
            @Valid @RequestBody AddRatingRequest req,
            Authentication authentication) {

        VendorRatingDto dto = vendorService.addRating(id, req, authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Rating submitted successfully"));
    }
}
