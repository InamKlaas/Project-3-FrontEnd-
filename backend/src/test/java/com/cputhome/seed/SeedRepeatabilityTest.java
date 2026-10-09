package com.cputhome.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.cputhome.listing.AccommodationRepository;
import com.cputhome.listing.RoomListingRepository;
import com.cputhome.message.MessageRepository;
import com.cputhome.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/* seed twice, count once — demo data never duplicates itself */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.jdbc.Sql("/clean.sql")
class SeedRepeatabilityTest {

  @Autowired SeedService seed;
  @Autowired UserRepository users;
  @Autowired AccommodationRepository accommodations;
  @Autowired RoomListingRepository rooms;
  @Autowired MessageRepository messages;
  @Autowired com.cputhome.user.LandlordProfileRepository landlords;

  @Test
  void seedIsIdempotent() {
    SeedService.SeedReport first = seed.seed();
    var user = users.findByEmail("pending-landlord@seed.local").orElseThrow();
    var profile = landlords.findByUserId(user.getId()).orElseThrow();
    profile.setVerificationStatus(com.cputhome.user.LandlordProfile.VerificationStatus.REJECTED);
    landlords.save(profile);
    SeedService.SeedReport second = seed.seed();

    assertThat(first.accommodations()).isEqualTo(11);
    assertThat(second.accommodations()).isEqualTo(11);
    assertThat(second.created()).isEqualTo(0);
    assertThat(messages.count()).isGreaterThanOrEqualTo(2);
    assertThat(rooms.count()).isGreaterThanOrEqualTo(11);
    assertThat(users.findByEmail("220001001@mycput.ac.za")).isPresent();
    assertThat(users.findByEmail("verified-landlord@seed.local")).isPresent();
    assertThat(landlords.findByUserId(user.getId()).orElseThrow().getVerificationStatus())
        .isEqualTo(com.cputhome.user.LandlordProfile.VerificationStatus.REJECTED);
  }
}
