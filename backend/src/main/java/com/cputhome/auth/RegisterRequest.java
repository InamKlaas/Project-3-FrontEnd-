package com.cputhome.auth;

import com.cputhome.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* registration body mirrors the React signup form field for field */
public record RegisterRequest(
    @NotBlank(message = "full name is required")
        @Size(max = 120, message = "full name is too long")
        String fullName,
    @NotBlank(message = "email is required") @Email(message = "email is invalid") String email,
    @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be at least 6 characters")
        String password,
    @NotBlank(message = "role is required") String role,
    @Size(max = 30, message = "student number is too long") String studentNumber,
    @Size(max = 80, message = "campus is too long") String campus,
    @Size(max = 10, message = "year of study is too long") String year,
    @Size(max = 30, message = "funding type is too long") String funding) {

  public UserRole parsedRole() {
    try {
      return UserRole.valueOf(role.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
