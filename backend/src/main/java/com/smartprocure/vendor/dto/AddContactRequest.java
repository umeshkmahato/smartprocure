package com.smartprocure.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for adding a contact person to a vendor.
 */
public record AddContactRequest(

        @NotBlank(message = "Contact name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Email(message = "Email must be valid")
        @Size(max = 255)
        String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 100, message = "Position must not exceed 100 characters")
        String position,

        boolean primary
) {}
