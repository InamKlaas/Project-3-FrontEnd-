package com.cputhome.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cputhome.user.LandlordProfileRepository;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/* the whole letting journey: draft stays hidden, approval opens the door */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListingIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired LandlordProfileRepository landlords;
  @Autowired com.cputhome.user.StudentProfileRepository students;
  @Autowired AccommodationRepository accommodations;
  @Autowired RoomListingRepository rooms;
  @Autowired SharedAccommodationRepository shared;
  @Autowired PrivateRoomRepository privates;
  @Autowired EntireUnitRepository units;
  @Autowired PasswordEncoder passwords;

  private String adminToken;
  private String landlordToken;
  private String freshToken;
  private String studentToken;

  @BeforeEach
  void seed() throws Exception {
    shared.deleteAll();
    privates.deleteAll();
    units.deleteAll();
    rooms.deleteAll();
    accommodations.deleteAll();
    landlords.deleteAll();
    students.deleteAll();
    users.deleteAll();
    users.save(new User("Admin", "s4admin@test.co.za", passwords.encode("password123"), UserRole.ADMIN));
    adminToken = login("s4admin@test.co.za");
    landlordToken = register("s4owner@test.co.za", "LANDLORD", null);
    verifyLandlord("s4owner@test.co.za");
    freshToken = register("s4fresh@test.co.za", "LANDLORD", null);
    studentToken = register("220000011@mycput.ac.za", "STUDENT", "220000011");
  }

  private String register(String email, String role, String studentNumber) throws Exception {
    Map<String, Object> req =
        new java.util.HashMap<>(
            Map.of("fullName", "Stage Four", "email", email, "password", "password123", "role", role));
    if (studentNumber != null) {
      req.put("studentNumber", studentNumber);
    }
    MvcResult result =
        mvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of("identifier", email, "password", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private void verifyLandlord(String email) throws Exception {
    Long userId = users.findByEmail(email).orElseThrow().getId();
    mvc.perform(
            patch("/api/admin/providers/" + userId + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", "VERIFIED"))))
        .andExpect(status().isOk());
  }

  private Map<String, Object> listingBody(String title, String type, String price) {
    return Map.of(
        "title", title,
        "description", "decent place near campus",
        "location", "Bellville",
        "campus", "Bellville",
        "rooms", List.of(Map.of("roomType", type, "monthlyRent", new BigDecimal(price), "beds", 1)));
  }

  private JsonNode create(String token, String title, String type, String price) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(listingBody(title, type, price))))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private void approve(Long id) throws Exception {
    mvc.perform(post("/api/admin/listings/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  private int publicCount(String query) throws Exception {
    MvcResult result =
        mvc.perform(get("/api/listings" + query)).andExpect(status().isOk()).andReturn();
    return mapper.readTree(result.getResponse().getContentAsString()).get("totalElements").asInt();
  }

  @Test
  void unverifiedLandlordCannotCreate() throws Exception {
    mvc.perform(
            post("/api/listings")
                .header("Authorization", "Bearer " + freshToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(listingBody("Fresh Place", "Single", "3000"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("LANDLORD_NOT_VERIFIED"));
  }

  @Test
  void pendingDraftInvisibleUntilApproval() throws Exception {
    JsonNode created = create(landlordToken, "Hidden House", "Single", "3000");
    long id = created.get("id").asLong();
    assertThat(created.get("status").asText()).isEqualTo("pending");

    /* strangers and students see nothing */
    assertThat(publicCount("")).isEqualTo(0);

    /* owner previews their own draft, admin sees everything */
    mvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isOk());
    approve(id);

    assertThat(publicCount("")).isEqualTo(1);
  }

  @Test
  void strangerCannotForgeOwnership() throws Exception {
    JsonNode created = create(landlordToken, "Owned House", "Single", "3000");
    long id = created.get("id").asLong();

    /* another landlord rewriting the price gets refused, not applied */
    mvc.perform(
            patch("/api/listings/" + id)
                .header("Authorization", "Bearer " + freshToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("title", "Stolen House"))))
        .andExpect(status().isForbidden());

    /* forged approved status in a patch is not a thing — allowlist only */
    mvc.perform(
            patch("/api/listings/" + id)
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("title", "Owned House"))))
        .andExpect(status().isOk());
    assertThat(publicCount("")).isEqualTo(0);
  }

  @Test
  void deactivationAndUnpublishHideIndependently() throws Exception {
    JsonNode created = create(landlordToken, "Busy House", "Single", "3000");
    long id = created.get("id").asLong();
    approve(id);
    assertThat(publicCount("")).isEqualTo(1);

    /* landlord off switch hides it, admin switch is independent */
    mvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isNoContent());
    assertThat(publicCount("")).isEqualTo(0);
  }

  @Test
  void materialEditReturnsToReview() throws Exception {
    JsonNode created = create(landlordToken, "Priced House", "Single", "3000");
    long id = created.get("id").asLong();
    approve(id);
    assertThat(publicCount("")).isEqualTo(1);

    mvc.perform(
            patch("/api/listings/" + id)
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("price", new BigDecimal("9999")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("pending"));
    assertThat(publicCount("")).isEqualTo(0);
  }

  @Test
  void searchFiltersAndSorts() throws Exception {
    JsonNode cheap = create(landlordToken, "Cheap Room Bellville", "Sharing", "2000");
    JsonNode pricey = create(landlordToken, "Pricey Room Bellville", "Bachelor", "6000");
    approve(cheap.get("id").asLong());
    approve(pricey.get("id").asLong());

    assertThat(publicCount("?search=cheap")).isEqualTo(1);
    assertThat(publicCount("?type=Sharing")).isEqualTo(1);
    assertThat(publicCount("?minPrice=5000")).isEqualTo(1);
    assertThat(publicCount("?maxPrice=2500")).isEqualTo(1);
    assertThat(publicCount("?campus=Nopeville")).isEqualTo(0);

    MvcResult sorted =
        mvc.perform(get("/api/listings?sort=price-low&size=10")).andExpect(status().isOk()).andReturn();
    JsonNode content = mapper.readTree(sorted.getResponse().getContentAsString()).get("content");
    assertThat(content.get(0).get("price").asDouble()).isEqualTo(2000.0);
    assertThat(content.get(1).get("price").asDouble()).isEqualTo(6000.0);
  }

  @Test
  void invalidRangeIs400() throws Exception {
    mvc.perform(get("/api/listings?minPrice=6000&maxPrice=2000"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_RANGE"));
  }

  @Test
  void emergencyNeedsRealAvailability() throws Exception {
    JsonNode created = create(landlordToken, "Urgent Room", "Single", "2500");
    long id = created.get("id").asLong();
    approve(id);

    /* flag alone is not enough until the room says emergency */
    assertThat(publicCount("?emergency=true")).isEqualTo(0);

    /* a genuinely available emergency room shows up */
    MvcResult sos =
        mvc.perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "title", "SOS Room",
                                "description", "available right now",
                                "location", "Bellville",
                                "campus", "Bellville",
                                "rooms",
                                    List.of(
                                        Map.of(
                                            "roomType", "Single",
                                            "monthlyRent", new BigDecimal("2500"),
                                            "emergency", true))))))
            .andExpect(status().isCreated())
            .andReturn();
    long sosId = mapper.readTree(sos.getResponse().getContentAsString()).get("id").asLong();
    approve(sosId);
    assertThat(publicCount("?emergency=true")).isEqualTo(1);
  }

  @Test
  void guestPreviewHidesAddress() throws Exception {
    JsonNode created = create(landlordToken, "Preview House", "Single", "3000");
    long id = created.get("id").asLong();
    approve(id);

    MvcResult guest =
        mvc.perform(get("/api/listings/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode guestView = mapper.readTree(guest.getResponse().getContentAsString());
    assertThat(guestView.get("address").isNull()).isTrue();

    MvcResult authed =
        mvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(mapper.readTree(authed.getResponse().getContentAsString()).get("desc").asText())
        .contains("decent place");
  }
}
