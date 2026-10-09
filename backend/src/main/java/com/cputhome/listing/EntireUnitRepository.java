package com.cputhome.listing;

import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, subtype rows are written at creation */
public interface EntireUnitRepository extends JpaRepository<EntireUnit, Long> {

  void deleteByAccommodationId(Long accommodationId);
}
