package com.cputhome.poc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/* the whole POC story in one test: register, list, approve, find, talk */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.jdbc.Sql("/clean.sql")
class PocJourneyTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder passwords;
  @Autowired com.cputhome.message.MessageRepository messages;
  @Autowired com.cputhome.listing.SharedAccommodationRepository shared;
  @Autowired com.cputhome.listing.PrivateRoomRepository privates;
  @Autowired com.cputhome.listing.EntireUnitRepository units;
  @Autowired com.cputhome.listing.RoomListingRepository rooms;
  @Autowired com.cputhome.listing.AccommodationRepository accommodations;
  @Autowired com.cputhome.user.LandlordProfileRepository landlords;
  @Autowired com.cputhome.user.StudentProfileRepository students;

  @BeforeEach
  void clean() {
    messages.deleteAll();
    shared.deleteAll();
    privates.deleteAll();
    units.deleteAll();
    rooms.deleteAll();
    accommodations.deleteAll();
    landlords.deleteAll();
    students.deleteAll();
    users.deleteAll();
  }

  private JsonNode register(String email, String role, String studentNumber) throws Exception {
    Map<String, Object> req =
        new java.util.HashMap<>(
            Map.of("fullName", "Journey User", "email", email, "password", "password123", "role", role));
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
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private String login(String email) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of("identifier", email, "password", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
    return body.get("token").asText();
  }

  @Test
  void registerCreateApproveFindAndMessage() throws Exception {
    /* admin straight from the repo, everyone else through the doors */
    users.save(new User("Admin", "pocadmin@test.co.za", passwords.encode("password123"), UserRole.ADMIN));
    String adminToken = login("pocadmin@test.co.za");
    String landlordToken = register("pocowner@test.co.za", "LANDLORD", null).get("token").asText();
    String studentToken = register("221000001@mycput.ac.za", "STUDENT", "221000001").get("token").asText();

    /* landlord verifies first, drafts stay hidden until approved */
    Long landlordId = users.findByEmail("pocowner@test.co.za").orElseThrow().getId();
    mvc.perform(
            patch("/api/admin/providers/" + landlordId + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", "VERIFIED"))))
        .andExpect(status().isOk());

    /* landlord drafts, still invisible */
    MvcResult created =
        mvc.perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "title", "POC House",
                                "description", "decent journey place",
                                "location", "Bellville",
                                "campus", "Bellville",
                                "rooms",
                                    List.of(
                                        Map.of(
                                            "roomType", "Single",
                                            "monthlyRent", new BigDecimal("2800"),
                                            "beds", 1))))))
            .andExpect(status().isCreated())
            .andReturn();
    long listingId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    MvcResult empty =
        mvc.perform(get("/api/listings?search=POC")).andExpect(status().isOk()).andReturn();
    assertThat(mapper.readTree(empty.getResponse().getContentAsString()).get("totalElements").asInt())
        .isEqualTo(0);

    /* admin approves, student finds */
    mvc.perform(post("/api/admin/listings/" + listingId + "/approve").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
    MvcResult found =
        mvc.perform(get("/api/listings?search=POC")).andExpect(status().isOk()).andReturn();
    JsonNode page = mapper.readTree(found.getResponse().getContentAsString());
    assertThat(page.get("totalElements").asInt()).isEqualTo(1);
    assertThat(page.get("content").get(0).get("price").asDouble()).isEqualTo(2800.0);

    /* student writes, landlord reads and answers */
    mvc.perform(
            post("/api/listings/" + listingId + "/messages")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("text", "is october open?"))))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/listings/" + listingId + "/messages")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        Map.of("text", "yes it is", "studentEmail", "221000001@mycput.ac.za"))))
        .andExpect(status().isCreated());
    MvcResult thread =
        mvc.perform(
                get("/api/listings/" + listingId + "/messages?studentId=221000001@mycput.ac.za")
                    .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(mapper.readTree(thread.getResponse().getContentAsString()).size()).isEqualTo(2);
  }
}
