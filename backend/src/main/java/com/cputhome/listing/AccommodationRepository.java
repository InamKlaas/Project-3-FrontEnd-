package com.cputhome.listing;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/* persistence only, visibility rules live in the listing service.
 * UUIDs identify people and conversations; accommodations keep the
 * numeric ids the frontend already coerces. */
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

  List<Accommodation> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

  /* public shelf: published by admin, active by landlord, approved,
   * with at least one available room. nothing else ever leaks out. */
  @Query(
      "select distinct a from Accommodation a join a.rooms r"
          + " where a.published = true and a.active = true"
          + " and a.approvalStatus = 'approved' and r.available = true")
  Page<Accommodation> findPublic(Pageable pageable);
}
