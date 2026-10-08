package com.cputhome.common;

public class NotFoundException extends ApiException {

  public NotFoundException(String message) {
    super("RESOURCE_NOT_FOUND", 404, message);
  }
}
