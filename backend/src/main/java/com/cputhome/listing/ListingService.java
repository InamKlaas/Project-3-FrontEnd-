package com.cputhome.listing;

import com.cputhome.common.BadRequestException;
import com.cputhome.common.ForbiddenException;
import com.cputhome.common.NotFoundException;
import com.cputhome.common.PageResponse;
import com.cputhome.security.UserPrincipal;
import com.cputhome.user.LandlordProfile;
import com.cputhome.user.LandlordProfileRepository;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* property + room workflows, controllers only translate in and out */
@Service
public class ListingService {

  private final AccommodationRepository accommodations;
  private final RoomListingRepository rooms;
  private final UserRepository users;
  private final LandlordProfileRepository landlords;
  private final SharedAccommodationRepository shared;
  private final PrivateRoomRepository privates;
  private final EntireUnitRepository units;
  private final ListingSearchRepository search;
  private final ListingCardMapper cards;

  public ListingService(
      AccommodationRepository accommodations,
      RoomListingRepository rooms,
      UserRepository users,
      LandlordProfileRepository landlords,
      SharedAccommodationRepository shared,
      PrivateRoomRepository privates,
      EntireUnitRepository units,
      ListingSearchRepository search,
      ListingCardMapper cards) {
    this.accommodations = accommodations;
    this.rooms = rooms;
    this.users = users;
    this.landlords = landlords;
    this.shared = shared;
    this.privates = privates;
    this.units = units;
    this.search = search;
    this.cards = cards;
  }

  @Transactional
  public ListingCardDto create(UserPrincipal principal, CreateListingRequest request) {
    User owner =
        users.findById(principal.id()).orElseThrow(() -> new NotFoundException("user not found"));
    if (owner.getRole() != UserRole.LANDLORD) {
      throw new ForbiddenException("FORBIDDEN", "only landlords may list accommodation");
    }
    LandlordProfile profile =
        landlords.findByUserId(owner.getId()).orElseThrow(() -> new ForbiddenException("FORBIDDEN", "landlord profile missing"));
    if (!profile.isVerified()) {
      /* pending or rejected providers never reach the public shelf */
      throw new ForbiddenException("LANDLORD_NOT_VERIFIED", "verification is required before listing");
    }

    Accommodation accommodation =
        new Accommodation(
            owner,
            request.title().trim(),
            request.description().trim(),
            request.location().trim(),
            request.campus().trim());
    accommodation.setAddress(blankToNull(request.address()));
    accommodation.setOnCampus(Boolean.TRUE.equals(request.onCampus()));
    accommodation.setNsfasClaim(Boolean.TRUE.equals(request.nsfas()));
    accommodation.setGender(request.gender() == null ? "Any" : request.gender().trim());
    accommodation.setAmenities(request.amenities() == null ? List.of() : request.amenities());
    accommodation.setImageUrls(request.imageUrls() == null ? List.of() : request.imageUrls());
    accommodation.setHouseRules(blankToNull(request.houseRules()));
    accommodation.setShuttle(blankToNull(request.shuttle()));
    accommodation.setUtilities(request.utilities() == null ? BigDecimal.ZERO : request.utilities());
    /* drafted, never public — admin approval comes later */
    accommodation.setPublished(false);
    accommodation.setApprovalStatus("pending");
    Accommodation saved = accommodations.save(accommodation);

    boolean first = true;
    for (CreateListingRequest.RoomSpec spec : request.rooms()) {
      RoomListing room =
          new RoomListing(saved, roomTypeOf(spec.roomType()), spec.monthlyRent());
      room.setDeposit(spec.deposit() == null ? BigDecimal.ZERO : spec.deposit());
      room.setBeds(spec.beds() == null ? 1 : spec.beds());
      room.setAvailable(spec.available() == null || spec.available());
      room.setAvailableDate(spec.availableDate());
      room.setEmergency(Boolean.TRUE.equals(spec.emergency()));
      saved.getRooms().add(room);
      if (first) {
        attachSubtype(saved, room.getRoomType());
        first = false;
      }
    }
    Accommodation stored = accommodations.save(saved);
    return cards.card(stored);
  }

  @Transactional(readOnly = true)
  public PageResponse<ListingCardDto> browse(ListingFilter filter, Pageable pageable) {
    if (filter.minPrice() != null
        && filter.maxPrice() != null
        && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
      throw new BadRequestException("INVALID_RANGE", "minimum rent cannot exceed maximum rent");
    }
    Page<Accommodation> page = search.searchPublic(filter, pageable);
    return new PageResponse<>(
        page.getContent().stream().map(cards::card).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }

  @Transactional(readOnly = true)
  public ListingCardDto detail(Long id, UserPrincipal viewer) {
    Accommodation accommodation = byId(id);
    boolean owner = viewer != null && accommodation.getOwner().getId().equals(viewer.id());
    boolean admin = viewer != null && viewer.role() == UserRole.ADMIN;
    if (!isPublic(accommodation) && !owner && !admin) {
      throw new NotFoundException("listing not found");
    }
    ListingCardDto card = cards.card(accommodation);
    if (viewer != null) {
      return card;
    }
    /* guests get a limited preview, no exact address */
    return new ListingCardDto(
        card.id(), card.owner(), card.ownerName(), card.title(), card.price(), card.rent(),
        card.location(), card.campus(), card.type(), card.available(), card.availableDate(),
        card.emergency(), card.status(), card.onCampus(), card.nsfas(), card.gender(),
        card.amenities(), card.beds(), card.image(), card.gallery(), card.deposit(),
        card.utilities(), card.houseRules(), card.shuttle(), preview(card.desc()), null,
        card.sample(), card.createdAt());
  }

  @Transactional(readOnly = true)
  public List<ListingCardDto> mine(UserPrincipal principal) {
    return accommodations.findByOwnerIdOrderByCreatedAtDesc(principal.id()).stream()
        .map(cards::card)
        .toList();
  }

  @Transactional
  public ListingCardDto update(Long id, UserPrincipal principal, UpdateListingRequest request) {
    Accommodation accommodation = owned(id, principal);
    if (request.title() != null && !request.title().isBlank()) {
      accommodation.setTitle(request.title().trim());
    }
    if (request.description() != null && !request.description().isBlank()) {
      accommodation.setDescription(request.description().trim());
    }
    if (request.location() != null && !request.location().isBlank()) {
      accommodation.setLocation(request.location().trim());
    }
    if (request.campus() != null && !request.campus().isBlank()) {
      accommodation.setCampus(request.campus().trim());
    }
    if (request.address() != null) {
      accommodation.setAddress(blankToNull(request.address()));
    }
    if (request.onCampus() != null) {
      accommodation.setOnCampus(request.onCampus());
    }
    if (request.nsfas() != null) {
      accommodation.setNsfasClaim(request.nsfas());
    }
    if (request.gender() != null && !request.gender().isBlank()) {
      accommodation.setGender(request.gender().trim());
    }
    if (request.amenities() != null) {
      accommodation.setAmenities(request.amenities());
    }
    if (request.imageUrls() != null) {
      accommodation.setImageUrls(request.imageUrls());
    }
    if (request.houseRules() != null) {
      accommodation.setHouseRules(blankToNull(request.houseRules()));
    }
    if (request.shuttle() != null) {
      accommodation.setShuttle(blankToNull(request.shuttle()));
    }
    if (request.utilities() != null) {
      accommodation.setUtilities(request.utilities());
    }
    if (request.active() != null) {
      accommodation.setActive(request.active());
    }
    boolean material = false;
    if (request.price() != null) {
      /* price moves land on the cheapest room, the card follows */
      RoomListing cheapest =
          rooms.findByAccommodationIdOrderByMonthlyRentAsc(id).stream()
              .findFirst()
              .orElseThrow(() -> new BadRequestException("NO_ROOMS", "listing has no rooms to reprice"));
      cheapest.setMonthlyRent(request.price());
      rooms.save(cheapest);
      material = true;
    }
    if (request.availableDate() != null) {
      for (RoomListing room : rooms.findByAccommodationIdOrderByMonthlyRentAsc(id)) {
        room.setAvailableDate(request.availableDate());
      }
      material = true;
    }
    if (material && "approved".equals(accommodation.getApprovalStatus())) {
      /* material edits after approval go back through review */
      accommodation.setPublished(false);
      accommodation.setApprovalStatus("pending");
      accommodation.setRejectionReason(null);
    }
    return cards.card(accommodations.save(accommodation));
  }

  @Transactional
  public void deactivate(Long id, UserPrincipal principal) {
    Accommodation accommodation = owned(id, principal);
    accommodation.setActive(false);
    accommodations.save(accommodation);
  }

  private Accommodation owned(Long id, UserPrincipal principal) {
    Accommodation accommodation = byId(id);
    boolean owner = accommodation.getOwner().getId().equals(principal.id());
    if (!owner && principal.role() != UserRole.ADMIN) {
      throw new ForbiddenException("FORBIDDEN", "only the owning landlord may change this listing");
    }
    return accommodation;
  }

  private Accommodation byId(Long id) {
    return accommodations.findById(id).orElseThrow(() -> new NotFoundException("listing not found"));
  }

  private static boolean isPublic(Accommodation accommodation) {
    return accommodation.isPublished()
        && accommodation.isActive()
        && "approved".equals(accommodation.getApprovalStatus());
  }

  private static String preview(String description) {
    if (description == null) {
      return "";
    }
    String trimmed = description.trim();
    return trimmed.length() <= 90 ? trimmed : trimmed.substring(0, 90) + "…";
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private RoomListing.RoomType roomTypeOf(String ui) {
    if (ui == null) {
      throw new BadRequestException("VALIDATION_ERROR", "room type is required");
    }
    return switch (ui.trim().toLowerCase(java.util.Locale.ROOT)) {
      case "single" -> RoomListing.RoomType.PRIVATE_ROOM;
      case "sharing" -> RoomListing.RoomType.SHARED_ACCOMMODATION;
      case "bachelor" -> RoomListing.RoomType.ENTIRE_UNIT;
      default -> throw new BadRequestException("VALIDATION_ERROR", "unknown room type: " + ui);
    };
  }

  /* one subtype row follows the first room's kind, the rest stay plain rooms */
  private void attachSubtype(Accommodation accommodation, RoomListing.RoomType type) {
    switch (type) {
      case SHARED_ACCOMMODATION ->
          shared.save(new SharedAccommodation(accommodation, 2));
      case PRIVATE_ROOM -> privates.save(new PrivateRoom(accommodation));
      case ENTIRE_UNIT -> units.save(new EntireUnit(accommodation, 1, 1));
    }
  }
}
