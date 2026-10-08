package com.cputhome.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
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

/* provider verification desk, admins only on every door */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminVerificationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired com.cputhome.user.StudentProfileRepository students;
  @Autowired com.cputhome.user.LandlordProfileRepository landlords;
  @Autowired PasswordEncoder passwords;

  private String adminToken;
  private String landlordToken;
  private Long landlordId;

  @BeforeEach
  void seed() throws Exception {
    landlords.deleteAll();
    students.deleteAll();
    users.deleteAll();
    users.save(new User("Admin", "admin@test.co.za", passwords.encode("password123"), UserRole.ADMIN));
    adminToken = login("admin@test.co.za");

    JsonNode landlord = register("owner@test.co.za", "LANDLORD", null);
    landlordToken = landlord.get("token").asText();
    landlordId = landlord.get("user").get("id").asLong();
    register("220000009@mycput.ac.za", "STUDENT", "220000009");
  }

  private JsonNode register(String email, String role, String studentNumber) throws Exception {
    Map<String, Object> req =
        new java.util.HashMap<>(
            Map.of("fullName", "Desk User", "email", email, "password", "password123", "role", role));
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
    return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  @Test
  void nonAdminsAreRefused() throws Exception {
    mvc.perform(get("/api/admin/providers").header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isForbidden());

    JsonNode student = register("220000010@mycput.ac.za", "STUDENT", "220000010");
    mvc.perform(
            get("/api/admin/providers")
                .header("Authorization", "Bearer " + student.get("token").asText()))
        .andExpect(status().isForbidden());
  }

  @Test
  void pendingQueueAndApproval() throws Exception {
    MvcResult queue =
        mvc.perform(get("/api/admin/providers?status=pending").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode rows = mapper.readTree(queue.getResponse().getContentAsString());
    assertThat(rows.size()).isEqualTo(1);
    assertThat(rows.get(0).get("email").asText()).isEqualTo("owner@test.co.za");
    assertThat(rows.get(0).get("verificationStatus").asText()).isEqualTo("pending");

    mvc.perform(
            patch("/api/admin/providers/" + landlordId + "/verification")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("status", "VERIFIED"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationStatus").value("verified"));

    MvcResult empty =
        mvc.perform(get("/api/admin/providers?status=pending").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(mapper.readTree(empty.getResponse().getContentAsString()).size()).isEqualTo(0);
  }

  @Test
  void disabledLandlordCannotLogin() throws Exception {
    mvc.perform(
            post("/api/admin/users/" + landlordId + "/disable")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("identifier", "owner@test.co.za", "password", "password123"))))
        .andExpect(status().isForbidden());

    mvc.perform(
            post("/api/admin/users/" + landlordId + "/enable")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());
    assertThat(login("owner@test.co.za")).isNotBlank();
  }
}
