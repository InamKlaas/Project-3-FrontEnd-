package com.cputhome.admin;

import com.cputhome.listing.ListingCardDto;
import com.cputhome.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  @GetMapping("/providers")
  public List<ProviderResponse> providers(@RequestParam(required = false) String status) {
    return service.providers(status);
  }

  @PatchMapping("/providers/{id}/verification")
  public ProviderResponse verify(
      @PathVariable Long id, @Valid @RequestBody VerifyProviderRequest request) {
    return service.verifyProvider(id, request);
  }

  @PostMapping("/users/{id}/disable")
  public ResponseEntity<Void> disable(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    service.setUserEnabled(id, false, principal);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/users/{id}/enable")
  public ResponseEntity<Void> enable(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    service.setUserEnabled(id, true, principal);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/listings")
  public List<ListingCardDto> pendingListings(@RequestParam(required = false) String status) {
    if (status != null && !"pending".equalsIgnoreCase(status)) {
      throw new com.cputhome.common.BadRequestException("VALIDATION_ERROR", "only status=pending is supported here");
    }
    return service.pendingListings();
  }

  @PostMapping("/listings/{id}/approve")
  public ListingCardDto approve(@PathVariable Long id) {
    return service.approveListing(id);
  }

  @PostMapping("/listings/{id}/reject")
  public ListingCardDto reject(@PathVariable Long id, @RequestBody(required = false) RejectListingRequest request) {
    return service.rejectListing(id, request == null ? null : request.reason());
  }
}
