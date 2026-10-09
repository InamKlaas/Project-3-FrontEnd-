package com.cputhome.message;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* persistence only, thread rules live in the message service */
public interface MessageRepository extends JpaRepository<Message, Long> {

  List<Message> findByListingIdAndStudentIdOrderByCreatedAtAsc(Long listingId, Long studentId);

  List<Message> findByStudentIdOrderByCreatedAtDesc(Long studentId);

  /* newest first, the service keeps the first row per thread */
  @Query(
      "select m from Message m where m.listing.owner.id = :ownerId order by m.createdAt desc")
  List<Message> findByListingOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
}
