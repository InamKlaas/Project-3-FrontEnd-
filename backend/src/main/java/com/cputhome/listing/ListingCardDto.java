package com.cputhome.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/* flat card shape the React UI already renders, projected from normalized rows */
public record ListingCardDto(
    Long id,
    String owner,
    String ownerName,
    String title,
    BigDecimal price,
    BigDecimal rent,
    String location,
    String campus,
    String type,
    boolean available,
    LocalDate availableDate,
    boolean emergency,
    String status,
    boolean onCampus,
    boolean nsfas,
    String gender,
    List<String> amenities,
    int beds,
    String image,
    List<String> gallery,
    BigDecimal deposit,
    BigDecimal utilities,
    String houseRules,
    String shuttle,
    String desc,
    String address,
    boolean sample,
    Instant createdAt) {}
