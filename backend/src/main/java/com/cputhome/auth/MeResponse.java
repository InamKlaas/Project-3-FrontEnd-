package com.cputhome.auth;

import com.cputhome.user.UserRole;

/* safe session view, lowercase role like the mock, never a hash */
public record MeResponse(
    Long id,
    String fullName,
    String email,
    UserRole role,
    String status,
    String studentNumber,
    String campus) {}
