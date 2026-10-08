package com.cputhome.listing;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/* public shelf query, everything filters in the database.
 * a property qualifies through rooms that themselves match the
 * room-level knobs — never through a room that fails them. */
@Repository
public class ListingSearchRepository {

  private final jakarta.persistence.EntityManager entities;

  public ListingSearchRepository(jakarta.persistence.EntityManager entities) {
    this.entities = entities;
  }

  public Page<Accommodation> searchPublic(ListingFilter filter, Pageable pageable) {
    CriteriaBuilder cb = entities.getCriteriaBuilder();

    CriteriaQuery<Accommodation> query = cb.createQuery(Accommodation.class);
    Root<Accommodation> root = query.from(Accommodation.class);
    Predicate visibility = visibility(cb, root);
    Predicate roomMatch = roomsMatching(cb, query, root, filter);
    List<Predicate> predicates = propertyPredicates(cb, root, filter);
    predicates.add(visibility);
    predicates.add(roomMatch);
    query.where(predicates.toArray(new Predicate[0]));
    query.orderBy(order(filter.sort(), cb, query, root));
    query.distinct(true);

    List<Accommodation> content =
        entities.createQuery(query).setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize()).getResultList();

    CriteriaQuery<Long> count = cb.createQuery(Long.class);
    Root<Accommodation> countRoot = count.from(Accommodation.class);
    List<Predicate> countPredicates = propertyPredicates(cb, countRoot, filter);
    countPredicates.add(visibility(cb, countRoot));
    countPredicates.add(roomsMatching(cb, count, countRoot, filter));
    count.select(cb.countDistinct(countRoot)).where(countPredicates.toArray(new Predicate[0]));
    long total = entities.createQuery(count).getSingleResult();

    return new PageImpl<>(content, pageable, total);
  }

  /* published by admin, active by landlord, approved — always, no exceptions */
  private Predicate visibility(CriteriaBuilder cb, Root<Accommodation> root) {
    return cb.and(
        cb.isTrue(root.get("published")),
        cb.isTrue(root.get("active")),
        cb.equal(root.get("approvalStatus"), "approved"));
  }

  private List<Predicate> propertyPredicates(
      CriteriaBuilder cb, Root<Accommodation> root, ListingFilter filter) {
    List<Predicate> predicates = new ArrayList<>();
    if (filter.search() != null && !filter.search().isBlank()) {
      String like = "%" + filter.search().trim().toLowerCase(java.util.Locale.ROOT) + "%";
      predicates.add(
          cb.or(
              cb.like(cb.lower(root.get("title")), like),
              cb.like(cb.lower(root.get("location")), like)));
    }
    if (filter.campus() != null && !filter.campus().isBlank()) {
      predicates.add(cb.equal(root.get("campus"), filter.campus().trim()));
    }
    if (filter.onCampus() != null) {
      predicates.add(cb.equal(root.get("onCampus"), filter.onCampus()));
    }
    if (filter.nsfas() != null) {
      predicates.add(cb.equal(root.get("nsfasClaim"), filter.nsfas()));
    }
    if (filter.gender() != null && !filter.gender().isBlank() && !"Any".equalsIgnoreCase(filter.gender().trim())) {
      predicates.add(cb.equal(root.get("gender"), filter.gender().trim()));
    }
    if (filter.amenity() != null && !filter.amenity().isBlank()) {
      var amenities = root.joinList("amenities", JoinType.INNER);
      predicates.add(cb.equal(amenities, filter.amenity().trim()));
    }
    return predicates;
  }

  /* at least one room passes every room-level knob at once */
  private Predicate roomsMatching(
      CriteriaBuilder cb, CriteriaQuery<?> query, Root<Accommodation> root, ListingFilter filter) {
    Subquery<Long> exists = query.subquery(Long.class);
    var room = exists.from(RoomListing.class);
    List<Predicate> roomPredicates = new ArrayList<>();
    roomPredicates.add(cb.equal(room.get("accommodation").get("id"), root.get("id")));
    roomPredicates.add(cb.isTrue(room.get("available")));
    if (filter.type() != null && !filter.type().isBlank()) {
      roomPredicates.add(cb.equal(room.get("roomType").as(String.class), uiToRoomType(filter.type().trim())));
    }
    if (filter.minPrice() != null) {
      roomPredicates.add(cb.greaterThanOrEqualTo(room.get("monthlyRent"), filter.minPrice()));
    }
    if (filter.maxPrice() != null) {
      roomPredicates.add(cb.lessThanOrEqualTo(room.get("monthlyRent"), filter.maxPrice()));
    }
    if (filter.availableBy() != null) {
      roomPredicates.add(
          cb.or(
              cb.isNull(room.get("availableDate")),
              cb.lessThanOrEqualTo(room.get("availableDate"), filter.availableBy())));
    }
    if (Boolean.TRUE.equals(filter.emergency())) {
      /* the flag alone never qualifies, availability rides along */
      roomPredicates.add(cb.isTrue(room.get("emergency")));
    }
    exists.select(cb.literal(1L)).where(roomPredicates.toArray(new Predicate[0]));
    return cb.exists(exists);
  }

  /* newest, cheapest, priciest, or emergency-first — deterministic ties by id */
  private List<jakarta.persistence.criteria.Order> order(
      String sort, CriteriaBuilder cb, CriteriaQuery<Accommodation> query, Root<Accommodation> root) {
    String mode = sort == null ? "priority" : sort.trim().toLowerCase(java.util.Locale.ROOT);
    return switch (mode) {
      case "price-low" ->
          List.of(cb.asc(minRent(cb, query, root)), cb.desc(root.get("id")));
      case "price-high" ->
          List.of(cb.desc(minRent(cb, query, root)), cb.desc(root.get("id")));
      case "newest" -> List.of(cb.desc(root.get("id")));
      default ->
          List.of(
              cb.desc(
                  cb.selectCase()
                      .when(cb.exists(emergencyRoom(cb, query, root)), 1)
                      .otherwise(0)),
              cb.desc(root.get("id")));
    };
  }

  /* starting rent = cheapest room, the same number the card shows */
  private jakarta.persistence.criteria.Expression<BigDecimal> minRent(
      CriteriaBuilder cb, CriteriaQuery<Accommodation> query, Root<Accommodation> root) {
    Subquery<BigDecimal> min = query.subquery(BigDecimal.class);
    var room = min.from(RoomListing.class);
    min.select(cb.min(room.get("monthlyRent")))
        .where(cb.equal(room.get("accommodation").get("id"), root.get("id")));
    return min;
  }

  private Subquery<Long> emergencyRoom(
      CriteriaBuilder cb, CriteriaQuery<Accommodation> query, Root<Accommodation> root) {
    Subquery<Long> exists = query.subquery(Long.class);
    var room = exists.from(RoomListing.class);
    exists
        .select(cb.literal(1L))
        .where(
            cb.equal(room.get("accommodation").get("id"), root.get("id")),
            cb.isTrue(room.get("available")),
            cb.isTrue(room.get("emergency")));
    return exists;
  }

  /* UI Single/Sharing/Bachelor to normalized room kinds */
  private String uiToRoomType(String ui) {
    return switch (ui.trim().toLowerCase(java.util.Locale.ROOT)) {
      case "single" -> RoomListing.RoomType.PRIVATE_ROOM.name();
      case "sharing" -> RoomListing.RoomType.SHARED_ACCOMMODATION.name();
      case "bachelor" -> RoomListing.RoomType.ENTIRE_UNIT.name();
      default -> ui.trim().toUpperCase(java.util.Locale.ROOT);
    };
  }
}
