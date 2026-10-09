package com.cputhome.listing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, pairing rules live in the listing service */
public interface RoomListingRepository extends JpaRepository<RoomListing, Long> {

  List<RoomListing> findByAccommodationIdOrderByMonthlyRentAsc(Long accommodationId);

  void deleteByAccommodationId(Long accommodationId);
}
