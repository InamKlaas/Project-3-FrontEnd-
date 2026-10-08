package com.cputhome.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/* admin verdict on a landlord, approved or rejected, nothing in between */
public record VerifyProviderRequest(
    @NotBlank(message = "status is required")
        @Pattern(regexp = "VERIFIED|REJECTED", message = "status must be VERIFIED or REJECTED")
        String status) {}
