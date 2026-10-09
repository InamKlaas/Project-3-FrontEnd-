package com.cputhome.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cputhome.listing.AccommodationRepository;
import com.cputhome.listing.RoomListingRepository;
import com.cputhome.listing.SharedAccommodationRepository;
import com.cputhome.listing.PrivateRoomRepository;
import com.cputhome.listing.EntireUnitRepository;
import com.cputhome.user.LandlordProfileRepository;
import com.cputhome.user.StudentProfileRepository;
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

/* threads stay between exactly one student and one listing owner */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessagingIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired StudentProfileRepository students;
  @Autowired LandlordProfileRepository landlords;
  @Autowired AccommodationRepository accommodations;
  @Autowired RoomListingRepository rooms;
  @Autowired SharedAccommodationRepository shared;
  @Autowired PrivateRoomRepository privates;
  @Autowired EntireUnitRepository units;
  @Autowired MessageRepository messages;
  @Autowired PasswordEncoder passwords;

  private String adminToken;
  private String landlordToken;
  private String studentToken;
  private String studentEmail = "220000020@mycput.ac.za";
  private String strangerToken;
  private long listingId;

  @BeforeEach
  void seed() throws Exception {
    messages.deleteAll();
    shared.deleteAll();
    privates.deleteAll();
    units.deleteAll();
    rooms.deleteAll();
    accommodations.deleteAll();
    landlords.deleteAll();
    students.deleteAll();
    users.deleteAll();
    users.save(new User("Admin", "msgadmin@test.co.za", passwords.encode("password123"), UserRole.ADMIN));
    adminToken = login("msgadmin@test.co.za");
    landlordToken = register("msgowner@test.co.za", "LANDLORD", null);
    verifyLandlord("msgowner@test.co.za");
    studentToken = register(studentEmail, "STUDENT", "220000020");
    strangerToken = register("220000021@mycput.ac.za", "STUDENT", "220000021");
    listingId = createListing(landlordToken, "Message House");
    approve(listingId);
  }

  private String register(String email, String role, String studentNumber) throws Exception {
    Map<String, Object> req =
        new java.util.HashMap<>(
            Map.of("fullName", "Msg User", "email", email, "password", "password123", "role", role));
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

  private long createListing(String token, String title) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "title", title,
                                "description", "decent place for messages",
                                "location", "Bellville",
                                "campus", "Bellville",
                                "rooms",
                                    List.of(
                                        Map.of(
                                            "roomType", "Single",
                                            "monthlyRent", new BigDecimal("3000"),
                                            "beds", 1))))))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private void approve(long id) throws Exception {
    mvc.perform(post("/api/admin/listings/" + id + "/approve").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  private void send(String token, String text, String studentEmail) throws Exception {
    Map<String, Object> body = new java.util.HashMap<>(Map.of("text", text));
    if (studentEmail != null) {
      body.put("studentEmail", studentEmail);
    }
    mvc.perform(
            post("/api/listings/" + listingId + "/messages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
        .andExpect(status().isCreated());
  }

  private JsonNode thread(String token, String student) throws Exception {
    MvcResult result =
        mvc.perform(
                get("/api/listings/" + listingId + "/messages?studentId=" + student)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  @Test
  void studentAndLandlordTalk() throws Exception {
    send(studentToken, "is this still available?", null);
    send(landlordToken, "yes, viewing friday?", studentEmail);

    JsonNode asStudent = thread(studentToken, studentEmail);
    assertThat(asStudent.size()).isEqualTo(2);
    assertThat(asStudent.get(0).get("text").asText()).isEqualTo("is this still available?");
    assertThat(asStudent.get(1).get("from").asText()).isEqualTo("msgowner@test.co.za");

    JsonNode asLandlord = thread(landlordToken, studentEmail);
    assertThat(asLandlord.size()).isEqualTo(2);

    /* inbox summaries on both sides */
    MvcResult inbox =
        mvc.perform(get("/api/conversations").header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(mapper.readTree(inbox.getResponse().getContentAsString()).size()).isEqualTo(1);

    MvcResult landlordInbox =
        mvc.perform(get("/api/conversations").header("Authorization", "Bearer " + landlordToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(mapper.readTree(landlordInbox.getResponse().getContentAsString()).size()).isEqualTo(1);
  }

  @Test
  void strangersAndGuestsAreKeptOut() throws Exception {
    send(studentToken, "hello?", null);

    /* another student reads nothing here */
    mvc.perform(
            get("/api/listings/" + listingId + "/messages?studentId=" + studentEmail)
                .header("Authorization", "Bearer " + strangerToken))
        .andExpect(status().isNotFound());

    /* a forged studentEmail in the body is ignored — the jwt decides */
    Map<String, Object> forged =
        new java.util.HashMap<>(Map.of("text", "i am the student", "studentEmail", studentEmail));
    MvcResult opened_ =
        mvc.perform(
                post("/api/listings/" + listingId + "/messages")
                    .header("Authorization", "Bearer " + strangerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(forged)))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode opened = mapper.readTree(opened_.getResponse().getContentAsString());
    assertThat(opened.get("student").asText()).isEqualTo("220000021@mycput.ac.za");
    assertThat(opened.get("from").asText()).isEqualTo("220000021@mycput.ac.za");

    /* guests get 401 everywhere here */
    mvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
  }

  @Test
  void landlordNeedsAStudentToReply() throws Exception {
    mvc.perform(
            post("/api/listings/" + listingId + "/messages")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("text", "hello?"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void blankMessagesRejected() throws Exception {
    mvc.perform(
            post("/api/listings/" + listingId + "/messages")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("text", "   "))))
        .andExpect(status().isBadRequest());
  }
}
