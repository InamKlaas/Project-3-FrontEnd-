package com.cputhome.common;

public class BadRequestException extends ApiException {

  public BadRequestException(String code, String message) {
    super(code, 400, message);
  }

  public BadRequestException(String message) {
    this("VALIDATION_ERROR", message);
  }
}
