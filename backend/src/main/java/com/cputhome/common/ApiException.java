package com.cputhome.common;

/* base for every domain failure, carries its own api code */
public abstract class ApiException extends RuntimeException {

  private final String code;
  private final int status;

  protected ApiException(String code, int status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public int getStatus() {
    return status;
  }
}
