package com.cputhome.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

/* error shape stays contractual without booting the server */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  void domainExceptionKeepsItsCode() {
    request.setRequestURI("/api/listings");

    ResponseEntity<ApiErrorResponse> response =
        handler.handleDomain(new NotFoundException("listing not found"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(response.getBody().path()).isEqualTo("/api/listings");
  }

  @Test
  void conflictMapsTo409() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleDomain(new ConflictException("DUPLICATE", "already there"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().code()).isEqualTo("DUPLICATE");
  }

  @Test
  void forbiddenMapsTo403() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleDomain(new ForbiddenException("nope"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void methodSecurityDenialMapsTo403() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleDenied(
            new org.springframework.security.access.AccessDeniedException("nope"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
  }

  @Test
  void unknownHidesInternals() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleUnknown(new IllegalStateException("db password=hunter2"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().message()).isEqualTo("something went wrong");
    assertThat(response.getBody().message()).doesNotContain("hunter2");
  }

  @Test
  void beanValidationShapesFieldErrors() throws Exception {
    Probe probe = new Probe();
    org.springframework.validation.BeanPropertyBindingResult binding =
        new org.springframework.validation.BeanPropertyBindingResult(probe, "probe");
    binding.rejectValue("email", "Email", "must be a valid email");
    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(
            new org.springframework.core.MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("beanValidationShapesFieldErrors"),
                -1),
            binding);

    ResponseEntity<ApiErrorResponse> response = handler.handleBeanValidation(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().fieldErrors()).containsEntry("email", "must be a valid email");
  }

  /* binding target for the shape test, never serialized */
  static class Probe {
    private String email;

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
