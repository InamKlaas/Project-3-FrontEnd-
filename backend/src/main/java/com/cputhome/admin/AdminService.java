package com.cputhome.admin;

import com.cputhome.common.ForbiddenException;
import com.cputhome.common.NotFoundException;
import com.cputhome.listing.Accommodation;
import com.cputhome.listing.AccommodationRepository;
import com.cputhome.listing.ListingCardDto;
import com.cputhome.listing.ListingCardMapper;
import com.cputhome.security.UserPrincipal;
import com.cputhome.user.LandlordProfile;
import com.cputhome.user.LandlordProfileRepository;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* human moderation desk, every door rechecks ADMIN — the UI gate is decoration */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminService {

  private final UserRepository users;
  private final LandlordProfileRepository landlords;
  private final AccommodationRepository accommodations;
  private final ListingCardMapper cards;

  public AdminService(
      UserRepository users,
      LandlordProfileRepository landlords,
      AccommodationRepository accommodations,
      ListingCardMapper cards) {
    this.users = users;
    this.landlords = landlords;
    this.accommodations = accommodations;
    this.cards = cards;
  }

  @Transactional(readOnly = true)
  public List<ProviderResponse> providers(String status) {
    return landlords.findAll().stream()
        .filter(profile -> status == null || profile.getVerificationStatus().name().equalsIgnoreCase(status))
        .map(profile -> ProviderResponse.from(profile.getUser(), profile))
        .toList();
  }

  @Transactional
  public ProviderResponse verifyProvider(Long userId, VerifyProviderRequest request) {
    User user = users.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
    if (user.getRole() != UserRole.LANDLORD) {
      throw new ForbiddenException("FORBIDDEN", "only landlords go through verification");
    }
    LandlordProfile profile =
        landlords.findByUserId(userId).orElseThrow(() -> new NotFoundException("landlord profile not found"));
    LandlordProfile.VerificationStatus next =
        LandlordProfile.VerificationStatus.valueOf(request.status());
    profile.setVerificationStatus(next);
    if (next == LandlordProfile.VerificationStatus.VERIFIED) {
      user.setStatus(User.UserStatus.VERIFIED);
    } else {
      user.setStatus(User.UserStatus.REJECTED);
    }
    users.save(user);
    return ProviderResponse.from(user, landlords.save(profile));
  }

  @Transactional
  public void setUserEnabled(Long userId, boolean enabled, UserPrincipal caller) {
    if (caller.id().equals(userId)) {
      /* admins cannot lock themselves out through this door */
      throw new ForbiddenException("FORBIDDEN", "you cannot change your own account state");
    }
    User user = users.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
    user.setEnabled(enabled);
    users.save(user);
  }

  @Transactional(readOnly = true)
  public List<ListingCardDto> pendingListings() {
    return accommodations.findAll().stream()
        .filter(row -> "pending".equals(row.getApprovalStatus()))
        .map(cards::card)
        .toList();
  }

  /* approval is one atomic flip, rejections keep their reason on the row */
  @Transactional
  public ListingCardDto approveListing(Long id) {
    Accommodation accommodation =
        accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    accommodation.setPublished(true);
    accommodation.setApprovalStatus("approved");
    accommodation.setRejectionReason(null);
    return cards.card(accommodations.save(accommodation));
  }

  @Transactional
  public ListingCardDto rejectListing(Long id, String reason) {
    Accommodation accommodation =
        accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    accommodation.setPublished(false);
    accommodation.setApprovalStatus("rejected");
    accommodation.setRejectionReason(reason == null || reason.isBlank() ? "no reason given" : reason.trim());
    return cards.card(accommodations.save(accommodation));
  }

  @Transactional
  public void unpublishListing(Long id) {
    Accommodation accommodation =
        accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    accommodation.setPublished(false);
    accommodations.save(accommodation);
  }
}
