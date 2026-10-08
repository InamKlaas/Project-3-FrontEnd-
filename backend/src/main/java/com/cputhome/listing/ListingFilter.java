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
    String sort) {}
