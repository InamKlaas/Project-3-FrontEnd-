package com.cputhome.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* owner edits, every field optional — anything absent stays as it was */
public record UpdateListingRequest(
    @Size(max = 150, message = "title is too long") String title,
    @Size(max = 4000, message = "description is too long") String description,
    @Size(max = 200, message = "location is too long") String location,
    @Size(max = 80) String campus,
    @Size(max = 300, message = "address is too long") String address,
    Boolean onCampus,
    Boolean nsfas,
    @Size(max = 20, message = "gender is too long") String gender,
    List<@Size(max = 60, message = "amenity is too long") String> amenities,
    List<@Size(max = 500, message = "image url is too long") String> imageUrls,
    @Size(max = 2000) String houseRules,
    @Size(max = 500) String shuttle,
    @DecimalMin("0") BigDecimal utilities,
    Boolean active,
    Boolean available,
    @DecimalMin(value = "0.01", message = "rent must be positive") BigDecimal price,
    LocalDate availableDate) {}
