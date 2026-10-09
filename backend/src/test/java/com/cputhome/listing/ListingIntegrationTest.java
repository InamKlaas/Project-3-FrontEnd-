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
@org.springframework.test.context.jdbc.Sql("/clean.sql")
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
  @Autowired com.cputhome.admin.ModerationDecisionRepository decisions;

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
        "description", "decent place near campus. This synthetic description is deliberately longer than ninety characters to check the server-side guest preview.",
        "address", "Synthetic test-only address",
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
                .content(mapper.writeValueAsString(Map.of("title", "Owned House", "status", "approved",
                    "owner", "s4fresh@test.co.za", "role", "ADMIN", "published", true))))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 400));
    mvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isOk()).andExpect(jsonPath("$.owner").value("s4owner@test.co.za"))
        .andExpect(jsonPath("$.status").value("pending"));
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
    mvc.perform(patch("/api/listings/" + id).header("Authorization", "Bearer " + landlordToken)
        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
        .andExpect(status().isOk());
    assertThat(publicCount("")).isEqualTo(1);
    mvc.perform(post("/api/admin/listings/" + id + "/unpublish").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
    assertThat(publicCount("")).isEqualTo(0);
    assertThat(accommodations.findById(id).orElseThrow().isActive()).isTrue();
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
  void availabilityToggleHidesAndReturns() throws Exception {
    JsonNode created = create(landlordToken, "Toggle House", "Single", "3000");
    long id = created.get("id").asLong();
    approve(id);
    assertThat(publicCount("")).isEqualTo(1);

    mvc.perform(
            patch("/api/listings/" + id)
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("available", false))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false));
    assertThat(publicCount("")).isEqualTo(0);

    mvc.perform(
            patch("/api/listings/" + id)
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("available", true))))
        .andExpect(status().isOk());
    assertThat(publicCount("")).isEqualTo(1);
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
    mvc.perform(patch("/api/listings/" + sosId).header("Authorization", "Bearer " + landlordToken)
        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}"))
        .andExpect(status().isOk());
    assertThat(publicCount("?emergency=true")).isZero();
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
    assertThat(guestView.get("owner").isNull()).isTrue();
    assertThat(guestView.get("desc").asText().length()).isLessThanOrEqualTo(91);
    mvc.perform(get("/api/listings")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].address").isEmpty());

    MvcResult authed =
        mvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(mapper.readTree(authed.getResponse().getContentAsString()).get("desc").asText())
        .contains("decent place");
    assertThat(mapper.readTree(authed.getResponse().getContentAsString()).get("address").asText())
        .isEqualTo("Synthetic test-only address");
  }

  @Test
  void multiRoomSearchProjectsAndSortsTheMatchingAvailableRoom() throws Exception {
    var body = new java.util.HashMap<>(listingBody("Mixed House", "Single", "3000"));
    body.put("rooms", List.of(
        Map.of("roomType", "Single", "monthlyRent", 500, "available", false, "emergency", true),
        Map.of("roomType", "Sharing", "monthlyRent", 1900),
        Map.of("roomType", "Single", "monthlyRent", 2900, "emergency", true)));
    var result = mvc.perform(post("/api/listings").header("Authorization", "Bearer " + landlordToken)
        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
        .andExpect(status().isCreated()).andReturn();
    long mixedId = mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    approve(mixedId);
    JsonNode other = create(landlordToken, "Other House", "Single", "2600");
    approve(other.get("id").asLong());
    mvc.perform(get("/api/listings?type=Single&emergency=true&minPrice=2000&maxPrice=3000"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].price").value(2900))
        .andExpect(jsonPath("$.content[0].emergency").value(true));
    mvc.perform(get("/api/listings?type=Single&sort=price-low"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].title").value("Other House"))
        .andExpect(jsonPath("$.content[1].price").value(2900));
    assertThat(publicCount("?type=Sharing&minPrice=2800")).isZero();
  }

  @Test
  void adminDecisionsRecordActorTimeAndReason() throws Exception {
    long id = create(landlordToken, "Audited House", "Single", "3000").get("id").asLong();
    approve(id);
    mvc.perform(post("/api/admin/listings/" + id + "/reject").header("Authorization", "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Synthetic review reason\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.rejectionReason").value("Synthetic review reason"));
    var trail = decisions.findBySubjectTypeAndSubjectIdOrderByIdAsc("listing", id);
    assertThat(trail).hasSize(2);
    assertThat(trail.get(0).getActor()).isEqualTo("s4admin@test.co.za");
    assertThat(trail.get(0).getCreatedAt()).isNotNull();
    assertThat(trail.get(1).getReason()).isEqualTo("Synthetic review reason");
  }
}
