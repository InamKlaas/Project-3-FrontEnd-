package com.cputhome.admin;

import com.cputhome.listing.ListingCardDto;
import com.cputhome.common.PageResponse;
import com.cputhome.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/* backend-side role checks on every door, no frontend claim trusted */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final AdminService service;

  public AdminController(AdminService service) {
    this.service = service;
  }

  @GetMapping("/users")
  public PageResponse<UserSummary> users(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.users(PageRequest.of(Math.max(0, page), PageResponse.capSize(size), Sort.by("id")));
  }

  @GetMapping("/providers")
  public List<ProviderResponse> providers(@RequestParam(required = false) String status) {
    return service.providers(status);
  }

  @PatchMapping("/providers/{id}/verification")
  public ProviderResponse verify(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long id, @Valid @RequestBody VerifyProviderRequest request) {
    return service.verifyProvider(id, request, principal);
  }

  @PatchMapping("/providers/{id}/accreditation")
  public ProviderResponse accredit(
      @PathVariable Long id, @Valid @RequestBody AccreditationRequest request) {
    return service.setAccreditation(id, Boolean.TRUE.equals(request.accredited()));
  }

  @PostMapping("/users/{id}/disable")
  public UserSummary disable(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    return service.setUserEnabled(id, false, principal);
  }

  @PostMapping("/users/{id}/enable")
  public UserSummary enable(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    return service.setUserEnabled(id, true, principal);
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> removeUser(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    service.removeUser(id, principal);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/listings")
  public List<ListingCardDto> listings(@RequestParam(required = false) String status) {
    return service.listings(status);
  }

  @PostMapping("/listings/{id}/approve")
  public ListingCardDto approve(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    return service.approveListing(id, principal);
  }

  @PostMapping("/listings/{id}/reject")
  public ListingCardDto reject(@AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long id, @Valid @RequestBody(required = false) RejectListingRequest request) {
    return service.rejectListing(id, request == null ? null : request.reason(), principal);
  }

  @PostMapping("/listings/{id}/unpublish")
  public ResponseEntity<Void> unpublish(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    service.unpublishListing(id, principal);
    return ResponseEntity.noContent().build();
  }
}
