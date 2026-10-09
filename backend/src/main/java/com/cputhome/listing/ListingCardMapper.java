package com.cputhome.listing;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/* normalized property + rooms in, flat UI card out. one card per property.
 * the shown room is the cheapest available one, otherwise the cheapest
 * overall — the card price is always a starting rent, never an average. */
@Component
public class ListingCardMapper {

  public ListingCardDto card(Accommodation accommodation) {
    return card(accommodation, null);
  }

  public ListingCardDto card(Accommodation accommodation, ListingFilter filter) {
    List<RoomListing> rooms = accommodation.getRooms();
    List<RoomListing> eligible = rooms.stream()
        .filter(room -> filter == null ? room.isAvailable() : filter.matchesRoom(room)).toList();
    RoomListing shown =
        eligible.stream()
            .min(Comparator.comparing(RoomListing::getMonthlyRent))
            .or(() -> filter == null ? rooms.stream().min(Comparator.comparing(RoomListing::getMonthlyRent))
                : java.util.Optional.empty())
            .orElse(null);

    BigDecimal price = shown == null ? BigDecimal.ZERO : shown.getMonthlyRent();
    List<String> images = accommodation.getImageUrls();
    return new ListingCardDto(
        accommodation.getId(),
        accommodation.getOwner().getEmail(),
        accommodation.getOwner().getFullName(),
        accommodation.getTitle(),
        price,
        price,
        accommodation.getLocation(),
        accommodation.getCampus(),
        shown == null ? "Single" : shown.getRoomType().ui(),
        shown != null && shown.isAvailable(),
        shown == null ? null : shown.getAvailableDate(),
        eligible.stream().anyMatch(RoomListing::isEmergency),
        accommodation.getApprovalStatus(),
        accommodation.isOnCampus(),
        accommodation.isNsfasClaim(),
        accommodation.getGender(),
        List.copyOf(accommodation.getAmenities()),
        shown == null ? 0 : shown.getBeds(),
        images.isEmpty() ? null : images.get(0),
        List.copyOf(images),
        shown == null || shown.getDeposit() == null ? BigDecimal.ZERO : shown.getDeposit(),
        accommodation.getUtilities() == null ? BigDecimal.ZERO : accommodation.getUtilities(),
        accommodation.getHouseRules(),
        accommodation.getShuttle(),
        accommodation.getDescription(),
        accommodation.getAddress(),
        accommodation.isSample(),
        accommodation.getCreatedAt(),
        accommodation.isActive(),
        accommodation.isPublished(),
        accommodation.getRejectionReason());
  }
}
