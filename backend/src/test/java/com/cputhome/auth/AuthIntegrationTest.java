package com.cputhome.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cputhome.user.UserRepository;
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

/* identity flows against h2, ownership always comes from the jwt */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired com.cputhome.user.StudentProfileRepository students;
  @Autowired com.cputhome.user.LandlordProfileRepository landlords;
  @Autowired PasswordEncoder passwords;

  @BeforeEach
  void clean() {
    landlords.deleteAll();
    students.deleteAll();
    users.deleteAll();
  }

  private Map<String, Object> registration(
      String fullName, String email, String role, String studentNumber) {
    Map<String, Object> req =
        new java.util.HashMap<>(
            Map.of(
                "fullName", fullName,
                "email", email,
                "password", "password123",
                "role", role,
                "campus", "Bellville"));
    if (studentNumber != null) {
      req.put("studentNumber", studentNumber);
    }
    return req;
  }

  private JsonNode registerOk(String email, String studentNumber, String role) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(registration("Test User", email, role, studentNumber))))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  @Test
  void studentRegistrationSucceedsWithProfile() throws Exception {
    JsonNode body = registerOk("220000001@mycput.ac.za", "220000001", "STUDENT");

    assertThat(body.get("token").asText()).isNotBlank();
    assertThat(body.get("user").get("email").asText()).isEqualTo("220000001@mycput.ac.za");
    assertThat(body.get("user").get("role").asText()).isEqualTo("student");
    assertThat(body.get("user").get("studentNumber").asText()).isEqualTo("220000001");
    assertThat(passwords.matches("password123",
        users.findByEmail("220000001@mycput.ac.za").orElseThrow().getPasswordHash())).isTrue();
  }

  @Test
  void landlordRegistrationStartsPendingVerification() throws Exception {
    JsonNode body = registerOk("owner@cput.ac.za", null, "LANDLORD");

    assertThat(body.get("user").get("role").asText()).isEqualTo("landlord");
    assertThat(body.get("user").get("status").asText()).isEqualTo("pending-verification");
  }

  @Test
  void nonMycputStudentIsRejected() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registration("Test User", "student@gmail.com", "STUDENT", "220000002"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_STUDENT_EMAIL"));
  }

  @Test
  void publicAdminRegistrationIsRejected() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registration("Sneaky", "sneaky@cput.ac.za", "ADMIN", null))))
        .andExpect(status().isForbidden());
  }

  @Test
  void duplicateEmailAndNumberConflict() throws Exception {
    registerOk("220000003@mycput.ac.za", "220000003", "STUDENT");

    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registration("Clone", "220000003@mycput.ac.za", "STUDENT", "220000004"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registration("Clone", "220000009@mycput.ac.za", "STUDENT", "220000003"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STUDENT_NUMBER_ALREADY_EXISTS"));
  }

  @Test
  void forgedRoleIsIgnored() throws Exception {
    /* role=admin in the body never mints an admin */
    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registration("Sneaky", "sneaky2@cput.ac.za", "admin", null))))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginByEmailAndNumber() throws Exception {
    registerOk("220000005@mycput.ac.za", "220000005", "STUDENT");

    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("identifier", "220000005@mycput.ac.za", "password", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("student"));

    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("identifier", "220000005", "password", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.studentNumber").value("220000005"));
  }

  @Test
  void wrongPasswordIs401() throws Exception {
    registerOk("220000006@mycput.ac.za", "220000006", "STUDENT");

    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("identifier", "220000006@mycput.ac.za", "password", "nope123"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void meNeedsAToken() throws Exception {
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

    JsonNode body = registerOk("220000007@mycput.ac.za", "220000007", "STUDENT");
    mvc.perform(
            get("/api/auth/me").header("Authorization", "Bearer " + body.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("220000007@mycput.ac.za"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void logoutIs204() throws Exception {
    JsonNode body = registerOk("220000008@mycput.ac.za", "220000008", "STUDENT");

    mvc.perform(
            post("/api/auth/logout").header("Authorization", "Bearer " + body.get("token").asText()))
        .andExpect(status().isNoContent());
  }
}
