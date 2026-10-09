package com.cputhome.message;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* persistence only, thread rules live in the message service */
public interface MessageRepository extends JpaRepository<Message, Long> {

  List<Message> findByListingIdAndStudentIdOrderByCreatedAtAsc(Long listingId, Long studentId);

  List<Message> findByStudentIdOrderByCreatedAtDesc(Long studentId);

  /* admin user removal wipes every line the user touched */
  @Query("delete from Message m where m.student.id = :id or m.sender.id = :id")
  @org.springframework.data.jpa.repository.Modifying
  void deleteByStudentOrSender(@Param("id") Long userId);

  @org.springframework.data.jpa.repository.Modifying
  @Query("delete from Message m where m.listing.id = :id")
  void deleteByListingId(@Param("id") Long listingId);

  /* newest first, the service keeps the first row per thread */
  @Query(
      "select m from Message m where m.listing.owner.id = :ownerId order by m.createdAt desc")
  List<Message> findByListingOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
}
