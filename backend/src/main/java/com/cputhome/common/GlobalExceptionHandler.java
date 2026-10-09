package com.cputhome.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/* bean validation on @Valid dtos -> VALIDATION_ERROR with per-field map */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleBeanValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fields =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    e -> e.getField(),
                    e -> e.getDefaultMessage() == null ? "invalid value" : e.getDefaultMessage(),
                    (first, second) -> first));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "one or more fields are invalid", fields, request);
  }

  /* malformed json or wrong enum values still read as validation, not a 500 */
  @ExceptionHandler({
    ConstraintViolationException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "malformed request body", null, request);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleDomain(ApiException ex, HttpServletRequest request) {
    return error(HttpStatus.valueOf(ex.getStatus()), ex.getCode(), ex.getMessage(), null, request);
  }

  /* method-security denials surface here, past the filter handlers */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleDenied(
      org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
    return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "you may not perform this action", null, request);
  }

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> handleUnauthenticated(
      org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
    return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "authentication required", null, request);
  }

  /* multipart size failures use the same API error shape */
  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleLargeUpload(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "FILE_SIZE", "documents must not exceed 5 MB", null, request);
  }

  /* generic server error does not expose internal exception details */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "something went wrong", null, request);
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status, String code, String message, Map<String, String> fields, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(new ApiErrorResponse(Instant.now(), status.value(), code, message, fields, request.getRequestURI()));
  }
}
