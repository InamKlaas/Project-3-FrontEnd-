package com.cputhome.user;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/* lowercase on the wire (student, landlord, admin), enum in java */
public enum UserRole {
  STUDENT,
  LANDLORD,
  ADMIN;

  @JsonValue
  public String wire() {
    return name().toLowerCase(Locale.ROOT);
  }
}
