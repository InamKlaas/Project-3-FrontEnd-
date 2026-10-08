package com.cputhome.auth;

import jakarta.validation.constraints.NotBlank;

/* identifier is email or student number, the service sorts it out */
public record LoginRequest(
    @NotBlank(message = "email or student number is required") String identifier,
    @NotBlank(message = "password is required") String password) {}
