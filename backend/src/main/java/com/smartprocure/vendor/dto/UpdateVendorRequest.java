package com.smartprocure.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating an existing vendor's details.
 * All fields are optional — only non-null values are applied.
 */
public record UpdateVendorRequest(

        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Email(message = "Email must be valid")
        @Size(max = 255)
        String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        String address,

        @Size(max = 50, message = "Tax ID must not exceed 50 characters")
        String taxId,

        String description,

        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website
) {}
