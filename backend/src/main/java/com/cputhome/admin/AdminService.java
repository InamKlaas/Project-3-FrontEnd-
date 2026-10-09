package com.cputhome.admin;

import com.cputhome.common.ForbiddenException;
import com.cputhome.common.BadRequestException;
import com.cputhome.common.NotFoundException;
import com.cputhome.common.PageResponse;
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
import org.springframework.data.domain.Pageable;
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
  private final com.cputhome.message.MessageRepository messages;
  private final com.cputhome.listing.SharedAccommodationRepository shared;
  private final com.cputhome.listing.PrivateRoomRepository privates;
  private final com.cputhome.listing.EntireUnitRepository units;
  private final com.cputhome.user.StudentProfileRepository students;
  private final ModerationDecisionRepository decisions;

  public AdminService(
      UserRepository users,
      LandlordProfileRepository landlords,
      AccommodationRepository accommodations,
      ListingCardMapper cards,
      com.cputhome.message.MessageRepository messages,
      com.cputhome.listing.SharedAccommodationRepository shared,
      com.cputhome.listing.PrivateRoomRepository privates,
      com.cputhome.listing.EntireUnitRepository units,
      com.cputhome.user.StudentProfileRepository students,
      ModerationDecisionRepository decisions) {
    this.users = users;
    this.landlords = landlords;
    this.accommodations = accommodations;
    this.cards = cards;
    this.messages = messages;
    this.shared = shared;
    this.privates = privates;
    this.units = units;
    this.students = students;
    this.decisions = decisions;
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummary> users(Pageable pageable) {
    var page = users.findAll(pageable).map(UserSummary::from);
    return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
  }

  @Transactional(readOnly = true)
  public List<ProviderResponse> providers(String status) {
    return landlords.findAll().stream()
        .filter(profile -> status == null || profile.getVerificationStatus().name().equalsIgnoreCase(status))
        .map(profile -> ProviderResponse.from(profile.getUser(), profile))
        .toList();
  }

  @Transactional
  public ProviderResponse verifyProvider(Long userId, VerifyProviderRequest request, UserPrincipal caller) {
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
    decisions.save(new ModerationDecision(caller.email(), "provider", userId, next.name(), null));
    return ProviderResponse.from(user, landlords.save(profile));
  }

  /* accreditation is a claim flag, independent of verification state */
  @Transactional
  public ProviderResponse setAccreditation(Long userId, boolean accredited) {
    User user = users.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
    if (user.getRole() != UserRole.LANDLORD) {
      throw new ForbiddenException("FORBIDDEN", "only landlords carry accreditation");
    }
    LandlordProfile profile =
        landlords.findByUserId(userId).orElseThrow(() -> new NotFoundException("landlord profile not found"));
    profile.setAccreditation(accredited);
    return ProviderResponse.from(user, landlords.save(profile));
  }

  @Transactional
  public UserSummary setUserEnabled(Long userId, boolean enabled, UserPrincipal caller) {
    if (caller.id().equals(userId)) {
      /* admins cannot lock themselves out through this door */
      throw new ForbiddenException("FORBIDDEN", "you cannot change your own account state");
    }
    User user = users.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
    user.setEnabled(enabled);
    return UserSummary.from(users.save(user));
  }

  /* full removal: threads, listings with their rooms, profiles, then the row.
   * admins cannot remove themselves or fellow admins through this door. */
  @Transactional
  public void removeUser(Long userId, UserPrincipal caller) {
    if (caller.id().equals(userId)) {
      throw new ForbiddenException("FORBIDDEN", "you cannot remove your own account");
    }
    User user = users.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
    if (user.getRole() == UserRole.ADMIN) {
      throw new ForbiddenException("FORBIDDEN", "admin accounts stay");
    }
    messages.deleteByStudentOrSender(userId);
    for (com.cputhome.listing.Accommodation accommodation :
        accommodations.findByOwnerIdOrderByCreatedAtDesc(userId)) {
      messages.deleteByListingId(accommodation.getId());
      shared.deleteByAccommodationId(accommodation.getId());
      privates.deleteByAccommodationId(accommodation.getId());
      units.deleteByAccommodationId(accommodation.getId());
      accommodations.delete(accommodation);
    }
    students.findByUserId(userId).ifPresent(students::delete);
    landlords.findByUserId(userId).ifPresent(landlords::delete);
    users.delete(user);
  }

  @Transactional(readOnly = true)
  public List<ListingCardDto> listings(String status) {
    if (status != null && !List.of("pending", "approved", "rejected").contains(status.toLowerCase(java.util.Locale.ROOT))) {
      throw new BadRequestException("VALIDATION_ERROR", "unknown listing status");
    }
    return accommodations.findAll().stream()
        .filter(row -> status == null || status.equalsIgnoreCase(row.getApprovalStatus()))
        .map(cards::card)
        .toList();
  }

  /* approval is one atomic flip, rejections keep their reason on the row */
  @Transactional
  public ListingCardDto approveListing(Long id, UserPrincipal caller) {
    Accommodation accommodation =
        accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    if (!accommodation.getOwner().isEnabled()
        || !landlords.findByUserId(accommodation.getOwner().getId()).map(LandlordProfile::isVerified).orElse(false)) {
      throw new BadRequestException("LANDLORD_NOT_VERIFIED", "verify and enable the provider before approving");
    }
    accommodation.setPublished(true);
    accommodation.setApprovalStatus("approved");
    accommodation.setRejectionReason(null);
    decisions.save(new ModerationDecision(caller.email(), "listing", id, "APPROVED", null));
    return cards.card(accommodations.save(accommodation));
  }

  @Transactional
  public ListingCardDto rejectListing(Long id, String reason, UserPrincipal caller) {
    Accommodation accommodation =
        accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    accommodation.setPublished(false);
    accommodation.setApprovalStatus("rejected");
    accommodation.setRejectionReason(reason == null || reason.isBlank() ? "no reason given" : reason.trim());
    decisions.save(new ModerationDecision(caller.email(), "listing", id, "REJECTED", accommodation.getRejectionReason()));
    return cards.card(accommodations.save(accommodation));
  }

  @Transactional
  public void unpublishListing(Long id, UserPrincipal caller) {
    Accommodation accommodation =
        accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
    accommodation.setPublished(false);
    decisions.save(new ModerationDecision(caller.email(), "listing", id, "UNPUBLISHED", null));
    accommodations.save(accommodation);
  }
}
