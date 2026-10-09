package com.cputhome.listing;

import java.math.BigDecimal;
import java.time.LocalDate;

/* public search knobs, mirrors the Home.jsx filter bar one to one */
public record ListingFilter(
    String search,
    String campus,
    String type,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    LocalDate availableBy,
    Boolean emergency,
    Boolean onCampus,
    Boolean nsfas,
    String gender,
    String amenity,
    String status,
    String sort) {
  /* Projection uses the same room-level rules as the database query. */
  public boolean matchesRoom(RoomListing room) {
    return room.isAvailable()
        && (type == null || type.isBlank() || room.getRoomType().ui().equalsIgnoreCase(type.trim())
            || room.getRoomType().name().equalsIgnoreCase(type.trim()))
        && (minPrice == null || room.getMonthlyRent().compareTo(minPrice) >= 0)
        && (maxPrice == null || room.getMonthlyRent().compareTo(maxPrice) <= 0)
        && (availableBy == null || room.getAvailableDate() == null || !room.getAvailableDate().isAfter(availableBy))
        && (!Boolean.TRUE.equals(emergency) || room.isEmergency());
  }
}
