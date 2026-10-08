package com.cputhome.user;

import com.cputhome.common.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/* one account for every role, lowercase on the wire like the mock */
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(name = "email", nullable = false, unique = true, length = 160)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 120)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private UserStatus status = UserStatus.PENDING_EMAIL;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected User() {
    /* jpa only */
  }

  public User(String fullName, String email, String passwordHash, UserRole role) {
    this.fullName = fullName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.enabled = true;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public UserRole getRole() {
    return role;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /* account lifecycle, lowercase like the React mock */
  public enum UserStatus {
    PENDING_EMAIL("pending-email"),
    PENDING_VERIFICATION("pending-verification"),
    VERIFIED("verified"),
    SUSPENDED("suspended"),
    REJECTED("rejected");

    private final String wire;

    UserStatus(String wire) {
      this.wire = wire;
    }

    @JsonValue
    public String wire() {
      return wire;
    }
  }
}
