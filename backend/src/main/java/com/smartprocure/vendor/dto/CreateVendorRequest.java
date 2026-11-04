package com.smartprocure.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for registering a new vendor.
 */
public record CreateVendorRequest(

        @NotBlank(message = "Vendor name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255)
        String email,

        @Size(max = 10, message = "Phone must not exceed 10 characters")
        String phone,

        String address,

        @Size(max = 50, message = "Tax ID must not exceed 50 characters")
        String taxId,

        String description,

        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website
) {}
