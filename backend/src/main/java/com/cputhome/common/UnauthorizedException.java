package com.cputhome.common;

public class UnauthorizedException extends ApiException {

  public UnauthorizedException(String code, String message) {
    super(code, 401, message);
  }
}
