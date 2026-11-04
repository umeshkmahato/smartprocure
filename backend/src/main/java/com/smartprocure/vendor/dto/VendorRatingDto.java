package com.smartprocure.vendor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.vendor.entity.VendorRating;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorRatingDto(
        Long          id,
        Long          vendorId,
        Long          ratedById,
        String        ratedByName,
        Integer       score,
        String        comment,
        LocalDateTime createdAt
) {
    public static VendorRatingDto fromEntity(VendorRating r) {
        return new VendorRatingDto(
                r.getId(),
                r.getVendor().getId(),
                r.getRatedBy() != null ? r.getRatedBy().getId()       : null,
                r.getRatedBy() != null ? r.getRatedBy().getFullName() : null,
                r.getScore(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
