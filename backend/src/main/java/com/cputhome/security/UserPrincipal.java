package com.cputhome.security;

import com.cputhome.user.UserRole;

/* who the jwt says we are, services never touch raw tokens */
public record UserPrincipal(Long id, String email, UserRole role) {}
