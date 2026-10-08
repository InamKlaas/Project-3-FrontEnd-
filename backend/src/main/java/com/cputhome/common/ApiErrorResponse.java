package com.cputhome.common;

import java.time.Instant;

/* every error wears the same shape, nothing internal leaks */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    java.util.Map<String, String> fieldErrors,
    String path) {}
