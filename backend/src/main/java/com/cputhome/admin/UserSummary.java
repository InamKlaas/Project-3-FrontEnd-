package com.cputhome.admin;

import com.cputhome.user.User;

/* account row after an admin flip, enough to repaint the desk */
public record UserSummary(
    Long id,
    String email,
    String fullName,
    String role,
    String status,
    boolean enabled) {

  public static UserSummary from(User user) {
    return new UserSummary(
        user.getId(),
        user.getEmail(),
        user.getFullName(),
        user.getRole().wire(),
        user.getStatus().wire(),
        user.isEnabled());
  }
}
