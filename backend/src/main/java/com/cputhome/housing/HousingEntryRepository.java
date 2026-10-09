package com.cputhome.housing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HousingEntryRepository extends JpaRepository<HousingEntry, Long> {
  boolean existsByKindAndListingIdAndAuthorId(String kind, Long listingId, Long authorId);
  List<HousingEntry> findByKindOrderByCreatedAtDesc(String kind);
  List<HousingEntry> findByKindAndListingIdOrderByCreatedAtDesc(String kind, Long listingId);
}
