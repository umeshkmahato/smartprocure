package com.smartprocure.vendor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.entity.VendorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Public representation of a {@link Vendor} — safe to serialize in API responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorDto(
        Long          id,
        String        name,
        String        email,
        String        phone,
        String        address,
        String        taxId,
        VendorStatus  status,
        BigDecimal    ratingScore,
        boolean       active,
        String        description,
        String        website,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String        createdBy,
        String        updatedBy
) {
    public static VendorDto fromEntity(Vendor v) {
        return new VendorDto(
                v.getId(),
                v.getName(),
                v.getEmail(),
                v.getPhone(),
                v.getAddress(),
                v.getTaxId(),
                v.getStatus(),
                v.getRatingScore(),
                v.isActive(),
                v.getDescription(),
                v.getWebsite(),
                v.getCreatedAt(),
                v.getUpdatedAt(),
                v.getCreatedBy(),
                v.getUpdatedBy()
        );
    }
}
