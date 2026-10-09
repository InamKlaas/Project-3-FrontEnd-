package com.cputhome.seed;

import com.cputhome.listing.Accommodation;
import com.cputhome.listing.AccommodationRepository;
import com.cputhome.listing.EntireUnit;
import com.cputhome.listing.EntireUnitRepository;
import com.cputhome.listing.PrivateRoom;
import com.cputhome.listing.PrivateRoomRepository;
import com.cputhome.listing.RoomListing;
import com.cputhome.listing.RoomListingRepository;
import com.cputhome.listing.SharedAccommodation;
import com.cputhome.listing.SharedAccommodationRepository;
import com.cputhome.message.Message;
import com.cputhome.message.MessageRepository;
import com.cputhome.user.LandlordProfile;
import com.cputhome.user.LandlordProfileRepository;
import com.cputhome.user.StudentProfile;
import com.cputhome.user.StudentProfileRepository;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* deterministic demo world, every row synthetic and labelled sample.
 * reruns change nothing: users keyed by email, properties by title. */
@Service
public class SeedService {

  /* dev-only credentials, documented in backend README, never production */
  public static final String SEED_PASSWORD = "SeedDemo123!";

  private final UserRepository users;
  private final StudentProfileRepository students;
  private final LandlordProfileRepository landlords;
  private final AccommodationRepository accommodations;
  private final RoomListingRepository rooms;
  private final SharedAccommodationRepository shared;
  private final PrivateRoomRepository privates;
  private final EntireUnitRepository units;
  private final MessageRepository messages;
  private final PasswordEncoder passwords;

  public SeedService(
      UserRepository users,
      StudentProfileRepository students,
      LandlordProfileRepository landlords,
      AccommodationRepository accommodations,
      RoomListingRepository rooms,
      SharedAccommodationRepository shared,
      PrivateRoomRepository privates,
      EntireUnitRepository units,
      MessageRepository messages,
      PasswordEncoder passwords) {
    this.users = users;
    this.students = students;
    this.landlords = landlords;
    this.accommodations = accommodations;
    this.rooms = rooms;
    this.shared = shared;
    this.privates = privates;
    this.units = units;
    this.messages = messages;
    this.passwords = passwords;
  }

  @Transactional
  public SeedReport seed() {
    User admin = admin();
    User verified = landlord("verified-landlord@seed.local", "Verified Provider", true);
    landlord("pending-landlord@seed.local", "Pending Provider", false);
    User student = student("220001001@mycput.ac.za", "Seed Student", "220001001", "Bellville");
    User buddy = student("220001002@mycput.ac.za", "Seed Buddy", "220001002", "Mowbray");

    int before = (int) accommodations.count();
    property(verified, "Sample House Bellville", "Bellville", "Bellville", false, true,
        List.of("WiFi", "Security"), room(RoomListing.RoomType.PRIVATE_ROOM, 2600, true, "2026-10-01", false),
        room(RoomListing.RoomType.PRIVATE_ROOM, 2900, true, "2026-10-01", true),
        true, true, "approved", null);
    property(verified, "City Edge Rooms", "Cape Town / District Six", "Cape Town / District Six", false, true,
        List.of("WiFi", "Laundry", "Security"), room(RoomListing.RoomType.SHARED_ACCOMMODATION, 1800, true, "2026-10-01", false),
        true, true, "approved", null);
    property(verified, "Mowbray Family House", "Mowbray", "Mowbray", false, false,
        List.of("WiFi"), room(RoomListing.RoomType.ENTIRE_UNIT, 5500, true, "2026-11-01", false),
        true, true, "approved", null);
    property(verified, "House Bliss Wellington", "Wellington", "Wellington", true, true,
        List.of("WiFi", "Security", "Shuttle"), room(RoomListing.RoomType.PRIVATE_ROOM, 2400, true, "2026-10-01", true),
        true, true, "approved", null);
    property(verified, "King Edward Parow", "Parow", "Parow", false, false,
        List.of("Security"), room(RoomListing.RoomType.SHARED_ACCOMMODATION, 1500, true, "2026-10-15", false),
        true, true, "approved", null);
    property(verified, "Aden Street Athlone", "Athlone", "Athlone", false, false,
        List.of("WiFi"), room(RoomListing.RoomType.PRIVATE_ROOM, 2100, true, "2026-10-01", false),
        true, true, "approved", null);
    property(verified, "Draft House Bellville", "Bellville", "Bellville", false, false,
        List.of("WiFi"), room(RoomListing.RoomType.PRIVATE_ROOM, 2700, true, "2026-10-01", false),
        false, true, "pending", null);
    property(verified, "Draft Flat Town", "Cape Town / District Six", "Cape Town / District Six", false, false,
        List.of("WiFi"), room(RoomListing.RoomType.ENTIRE_UNIT, 6000, true, "2026-11-01", false),
        false, true, "pending", null);
    property(verified, "Rejected Rooms Parow", "Parow", "Parow", false, false,
        List.of("WiFi"), room(RoomListing.RoomType.SHARED_ACCOMMODATION, 1200, true, "2026-10-01", false),
        false, true, "rejected", "photos do not match the building");
    property(verified, "Sleepy House Bellville", "Bellville", "Bellville", false, false,
        List.of("WiFi", "Security"), room(RoomListing.RoomType.PRIVATE_ROOM, 2300, true, "2026-10-01", false),
        true, false, "approved", null);
    property(verified, "Full House Mowbray", "Mowbray", "Mowbray", false, false,
        List.of("WiFi"), room(RoomListing.RoomType.PRIVATE_ROOM, 2500, false, "2027-01-01", false),
        true, true, "approved", null);
    /* second room on the first property exercises multi-room cards */
    Accommodation first =
        accommodations.findAll().stream()
            .filter(a -> a.getTitle().equals("Sample House Bellville"))
            .findFirst()
            .orElseThrow();
    if (rooms.findByAccommodationIdOrderByMonthlyRentAsc(first.getId()).size() < 2) {
      RoomListing extra =
          new RoomListing(first, RoomListing.RoomType.SHARED_ACCOMMODATION, BigDecimal.valueOf(1900));
      extra.setAvailable(true);
      extra.setAvailableDate(LocalDate.parse("2026-10-01"));
      first.getRooms().add(extra);
      rooms.save(extra);
    }

    thread(student, verified);
    return new SeedReport(
        users.count(), accommodations.count(), (int) accommodations.count() - before,
        messages.count());
  }

  private User admin() {
    return users
        .findByEmail("admin@seed.local")
        .orElseGet(
            () -> {
              User admin =
                  new User("Seed Admin", "admin@seed.local", passwords.encode(SEED_PASSWORD), UserRole.ADMIN);
              admin.setStatus(User.UserStatus.VERIFIED);
              return users.save(admin);
            });
  }

  private User landlord(String email, String name, boolean verified) {
    User user =
        users
            .findByEmail(email)
            .orElseGet(
                () -> {
                  User created = new User(name, email, passwords.encode(SEED_PASSWORD), UserRole.LANDLORD);
                  created.setStatus(
                      verified ? User.UserStatus.VERIFIED : User.UserStatus.PENDING_VERIFICATION);
                  return users.save(created);
                });
    LandlordProfile profile =
        landlords.findByUserId(user.getId()).orElseGet(() -> landlords.save(new LandlordProfile(user)));
    profile.setVerificationStatus(
        verified
            ? LandlordProfile.VerificationStatus.VERIFIED
            : LandlordProfile.VerificationStatus.PENDING);
    landlords.save(profile);
    return user;
  }

  private User student(String email, String name, String number, String campus) {
    User user =
        users
            .findByEmail(email)
            .orElseGet(
                () -> {
                  User created = new User(name, email, passwords.encode(SEED_PASSWORD), UserRole.STUDENT);
                  created.setStatus(User.UserStatus.VERIFIED);
                  return users.save(created);
                });
    students
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              StudentProfile profile = new StudentProfile(user, number, campus);
              profile.setEmailVerified(true);
              return students.save(profile);
            });
    return user;
  }

  private record RoomSpec(
      RoomListing.RoomType type, int rent, boolean available, String date, boolean emergency) {}

  private RoomSpec room(
      RoomListing.RoomType type, int rent, boolean available, String date, boolean emergency) {
    return new RoomSpec(type, rent, available, date, emergency);
  }

  private void property(
      User owner,
      String title,
      String location,
      String campus,
      boolean onCampus,
      boolean nsfas,
      List<String> amenities,
      RoomSpec first,
      boolean published,
      boolean active,
      String approval,
      String rejectionReason) {
    property(owner, title, location, campus, onCampus, nsfas, amenities, first, null, published, active,
        approval, rejectionReason);
  }

  private void property(
      User owner,
      String title,
      String location,
      String campus,
      boolean onCampus,
      boolean nsfas,
      List<String> amenities,
      RoomSpec first,
      RoomSpec second,
      boolean published,
      boolean active,
      String approval,
      String rejectionReason) {
    if (accommodations.existsByTitle(title)) {
      return;
    }
    Accommodation accommodation = new Accommodation(owner, title,
        title + " is listed here as sample data for the CPUT Home student project. Verify all details with the provider and CPUT before making decisions.",
        location, campus);
    accommodation.setAddress("Sample area: " + location + ". Exact street address not supplied; verify with CPUT and the provider.");
    accommodation.setOnCampus(onCampus);
    accommodation.setNsfasClaim(nsfas);
    accommodation.setGender("Any");
    accommodation.setAmenities(amenities);
    accommodation.setImageUrls(
        List.of("/images/residence-bellville.svg", "/images/residence-room.svg"));
    accommodation.setHouseRules("No smoking indoors. Quiet hours after 22:00. Visitors must sign in.");
    accommodation.setShuttle("Confirm shuttle availability with the provider.");
    accommodation.setPublished(published);
    accommodation.setActive(active);
    accommodation.setApprovalStatus(approval);
    accommodation.setRejectionReason(rejectionReason);
    accommodation.setSample(true);
    accommodations.save(accommodation);

    addRoom(accommodation, first);
    if (second != null) {
      addRoom(accommodation, second);
    }
    attachSubtype(accommodation);
  }

  private void addRoom(Accommodation accommodation, RoomSpec spec) {
    RoomListing room =
        new RoomListing(accommodation, spec.type(), BigDecimal.valueOf(spec.rent()));
    room.setAvailable(spec.available());
    room.setAvailableDate(spec.date() == null ? null : LocalDate.parse(spec.date()));
    room.setEmergency(spec.emergency());
    accommodation.getRooms().add(room);
    rooms.save(room);
  }

  private void attachSubtype(Accommodation accommodation) {
    RoomListing.RoomType first =
        rooms.findByAccommodationIdOrderByMonthlyRentAsc(accommodation.getId()).stream()
            .findFirst()
            .map(RoomListing::getRoomType)
            .orElse(RoomListing.RoomType.PRIVATE_ROOM);
    switch (first) {
      case SHARED_ACCOMMODATION -> shared.save(new SharedAccommodation(accommodation, 2));
      case PRIVATE_ROOM -> privates.save(new PrivateRoom(accommodation));
      case ENTIRE_UNIT -> units.save(new EntireUnit(accommodation, 1, 1));
    }
  }

  private void thread(User student, User landlord) {
    Accommodation home =
        accommodations.findAll().stream()
            .filter(a -> a.getTitle().equals("Sample House Bellville"))
            .findFirst()
            .orElseThrow();
    if (!messages.findByListingIdAndStudentIdOrderByCreatedAtAsc(home.getId(), student.getId()).isEmpty()) {
      return;
    }
    messages.save(new Message(home, student, student, "is this room still available for october?"));
    messages.save(new Message(home, student, landlord, "yes, viewing friday afternoon works for us."));
  }

  public record SeedReport(long users, long accommodations, long created, long messages) {}
}
