package com.cputhome.listing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* flat create body like the dashboard form, rooms ride along */
public record CreateListingRequest(
    @NotBlank(message = "title is required")
        @Size(max = 150, message = "title is too long")
        String title,
    @NotBlank(message = "description is required")
        @Size(max = 4000, message = "description is too long")
        String description,
    @NotBlank(message = "location is required")
        @Size(max = 200, message = "location is too long")
        String location,
    @NotBlank(message = "campus is required") String campus,
    @Size(max = 300, message = "address is too long") String address,
    Boolean onCampus,
    Boolean nsfas,
    @Size(max = 20, message = "gender is too long") String gender,
    List<@Size(max = 60, message = "amenity is too long") String> amenities,
    List<@Size(max = 500, message = "image url is too long") String> imageUrls,
    String houseRules,
    String shuttle,
    BigDecimal utilities,
    @NotNull(message = "at least one room is required")
        @Size(min = 1, message = "at least one room is required")
        List<@Valid RoomSpec> rooms) {

  /* one bookable unit inside the property form */
  public record RoomSpec(
      @NotBlank(message = "room type is required") String roomType,
      @NotNull(message = "rent is required")
          @DecimalMin(value = "0.01", message = "rent must be positive")
          BigDecimal monthlyRent,
      BigDecimal deposit,
      Integer beds,
      Boolean available,
      LocalDate availableDate,
      Boolean emergency) {}
}
