package com.cputhome.auth;

/* login/register answer, token plus who it belongs to */
public record AuthResponse(String token, MeResponse user) {}
