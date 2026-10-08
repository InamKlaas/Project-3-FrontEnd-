package com.cputhome.common;

public class ConflictException extends ApiException {

  public ConflictException(String code, String message) {
    super(code, 409, message);
  }
}
