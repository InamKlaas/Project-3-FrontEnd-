package com.cputhome.listing;

import com.cputhome.common.PageResponse;
import com.cputhome.security.UserPrincipal;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/* thin listing doors, filters run in the database */
@RestController
@RequestMapping("/api/listings")
public class ListingController {

  private final ListingService service;

  public ListingController(ListingService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<ListingCardDto> browse(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String campus,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableBy,
      @RequestParam(required = false) Boolean emergency,
      @RequestParam(required = false) Boolean onCampus,
      @RequestParam(required = false) Boolean nsfas,
      @RequestParam(required = false) String gender,
      @RequestParam(required = false) String amenity,
      @RequestParam(required = false, defaultValue = "priority") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.capSize(size));
    return service.browse(
        new ListingFilter(
            search, campus, type, minPrice, maxPrice, availableBy, emergency, onCampus,
            nsfas, gender, amenity, null, sort),
        pageable);
  }

  @GetMapping("/{id}")
  public ListingCardDto detail(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    return service.detail(id, principal);
  }

  @PostMapping
  public ResponseEntity<ListingCardDto> create(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody CreateListingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal, request));
  }

  @PatchMapping("/{id}")
  public ListingCardDto update(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpdateListingRequest request) {
    return service.update(id, principal, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    service.deactivate(id, principal);
    return ResponseEntity.noContent().build();
  }
}
