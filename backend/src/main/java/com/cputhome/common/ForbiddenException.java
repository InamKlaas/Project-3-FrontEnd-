package com.cputhome.common;

public class ForbiddenException extends ApiException {

  public ForbiddenException(String code, String message) {
    super(code, 403, message);
  }

  public ForbiddenException(String message) {
    this("FORBIDDEN", message);
  }
}
