package com.cputhome.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/* the public shelf only ever shows approved, active, available rooms */
@DataJpaTest
@ActiveProfiles("test")
class AccommodationRepositoryTest {

  @Autowired AccommodationRepository accommodations;
  @Autowired RoomListingRepository rooms;
  @Autowired UserRepository users;

  private User owner;

  @BeforeEach
  void seed() {
    owner = users.save(new User("Owner", "owner@cput.ac.za", "hash", UserRole.LANDLORD));
  }

  private Accommodation property(boolean published, boolean active, String approval) {
    Accommodation accommodation =
        new Accommodation(owner, "Residence", "decent place", "Bellville", "Bellville");
    accommodation.setPublished(published);
    accommodation.setActive(active);
    accommodation.setApprovalStatus(approval);
    return accommodations.save(accommodation);
  }

  private void room(Accommodation accommodation, boolean available) {
    RoomListing room =
        new RoomListing(accommodation, RoomListing.RoomType.PRIVATE_ROOM, BigDecimal.valueOf(3000));
    room.setAvailable(available);
    accommodation.getRooms().add(room);
    rooms.save(room);
  }

  private List<Long> publicIds() {
    Page<Accommodation> page = accommodations.findPublic(PageRequest.of(0, 20));
    return page.getContent().stream().map(Accommodation::getId).toList();
  }

  @Test
  void approvedActiveAvailableAppears() {
    Accommodation good = property(true, true, "approved");
    room(good, true);

    assertThat(publicIds()).contains(good.getId());
  }

  @Test
  void pendingNeverLeaks() {
    Accommodation pending = property(false, true, "pending");
    room(pending, true);

    assertThat(publicIds()).doesNotContain(pending.getId());
  }

  @Test
  void unpublishedNeverLeaks() {
    Accommodation hidden = property(false, true, "approved");
    room(hidden, true);

    assertThat(publicIds()).doesNotContain(hidden.getId());
  }

  @Test
  void deactivatedNeverLeaks() {
    Accommodation off = property(true, false, "approved");
    room(off, true);

    assertThat(publicIds()).doesNotContain(off.getId());
  }

  @Test
  void rejectedNeverLeaks() {
    Accommodation rejected = property(false, true, "rejected");
    room(rejected, true);

    assertThat(publicIds()).doesNotContain(rejected.getId());
  }

  @Test
  void fullyBookedNeverLeaks() {
    Accommodation full = property(true, true, "approved");
    room(full, false);

    assertThat(publicIds()).doesNotContain(full.getId());
  }

  @Test
  void ownerFeedSeesEverything() {
    Accommodation pending = property(false, true, "pending");
    room(pending, true);

    assertThat(accommodations.findByOwnerIdOrderByCreatedAtDesc(owner.getId()))
        .extracting(Accommodation::getId)
        .contains(pending.getId());
  }
}
