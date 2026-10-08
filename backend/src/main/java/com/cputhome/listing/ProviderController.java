package com.cputhome.listing;

import com.cputhome.security.UserPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* landlord's own shelf, every status included — the public feed hides these */
@RestController
@RequestMapping("/api/providers/me")
public class ProviderController {

  private final ListingService service;

  public ProviderController(ListingService service) {
    this.service = service;
  }

  @GetMapping("/listings")
  public List<ListingCardDto> mine(@AuthenticationPrincipal UserPrincipal principal) {
    return service.mine(principal);
  }
}
