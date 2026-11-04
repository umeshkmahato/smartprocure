package com.smartprocure.vendor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartprocure.vendor.entity.VendorContact;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorContactDto(
        Long          id,
        Long          vendorId,
        String        name,
        String        email,
        String        phone,
        String        position,
        boolean       primary,
        LocalDateTime createdAt
) {
    public static VendorContactDto fromEntity(VendorContact c) {
        return new VendorContactDto(
                c.getId(),
                c.getVendor().getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getPosition(),
                c.isPrimary(),
                c.getCreatedAt()
        );
    }
}
