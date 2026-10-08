package com.cputhome.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.cputhome.user.User;
import com.cputhome.user.UserRole;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/* one property, one card — cheapest available room sets the price */
class ListingCardMapperTest {

  private final ListingCardMapper mapper = new ListingCardMapper();

  private Accommodation property() {
    User owner = new User("Owner", "owner@cput.ac.za", "hash", UserRole.LANDLORD);
    Accommodation accommodation =
        new Accommodation(owner, "Residence", "decent place", "Bellville", "Bellville");
    accommodation.setPublished(true);
    accommodation.setApprovalStatus("approved");
    return accommodation;
  }

  private void room(Accommodation accommodation, int rent, boolean available, boolean emergency) {
    RoomListing room =
        new RoomListing(accommodation, RoomListing.RoomType.PRIVATE_ROOM, BigDecimal.valueOf(rent));
    room.setAvailable(available);
    room.setEmergency(emergency);
    accommodation.getRooms().add(room);
  }

  @Test
  void cheapestAvailableRoomSetsCard() {
    Accommodation accommodation = property();
    room(accommodation, 4000, true, false);
    room(accommodation, 3000, true, true);
    room(accommodation, 2000, false, false);

    ListingCardDto card = mapper.card(accommodation);

    assertThat(card.price()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    assertThat(card.rent()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    assertThat(card.available()).isTrue();
    assertThat(card.emergency()).isTrue();
    assertThat(card.type()).isEqualTo("Single");
    assertThat(card.owner()).isEqualTo("owner@cput.ac.za");
    assertThat(card.status()).isEqualTo("approved");
  }

  @Test
  void fullyBookedFallsBackToCheapest() {
    Accommodation accommodation = property();
    room(accommodation, 4000, false, false);
    room(accommodation, 3000, false, false);

    ListingCardDto card = mapper.card(accommodation);

    assertThat(card.price()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    assertThat(card.available()).isFalse();
  }

  @Test
  void roomlessPropertyStillMaps() {
    ListingCardDto card = mapper.card(property());

    assertThat(card.price()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(card.available()).isFalse();
    assertThat(card.beds()).isEqualTo(0);
  }
}
