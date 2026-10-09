package com.cputhome.housing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cputhome.listing.*;
import com.cputhome.seed.SeedService;
import com.cputhome.security.JwtService;
import com.cputhome.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.media.upload-dir=./uploads-test/housing-integration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/clean.sql")
class HousingWorkflowIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired AccommodationRepository listings;
  @Autowired RoomListingRepository rooms;
  @Autowired HousingEntryRepository entries;
  @Autowired PasswordEncoder passwords;
  @Autowired JwtService jwt;
  @Autowired SeedService seed;
  String student, stranger, owner, admin;
  Long listingId;
  final List<Path> files = new ArrayList<>();
  @BeforeEach void setup() {
    student = account("Student", "student@mycput.ac.za", UserRole.STUDENT);
    stranger = account("Other Student", "other@mycput.ac.za", UserRole.STUDENT);
    owner = account("Provider", "owner@test.local", UserRole.LANDLORD);
    admin = account("Admin", "admin@test.local", UserRole.ADMIN);
    Accommodation home = new Accommodation(users.findByEmail("owner@test.local").orElseThrow(), "Integration Residence", "Test residence", "Cape Town", "Cape Town / District Six");
    home.setPublished(true); home.setApprovalStatus("approved"); listings.save(home);
    rooms.save(new RoomListing(home, RoomListing.RoomType.PRIVATE_ROOM, BigDecimal.valueOf(3200)));
    listingId = home.getId();
  }
  String account(String name, String email, UserRole role) {
    User user = new User(name, email, passwords.encode("Integration123!"), role);
    user.setStatus(User.UserStatus.VERIFIED); return jwt.generate(users.save(user));
  }
  String body(Object value) throws Exception { return json.writeValueAsString(value); }
  Map<String, Object> application() { return Map.of("note", "I need a room near campus", "moveIn", LocalDate.now().plusDays(7).toString()); }
  Long apply() throws Exception {
    var response = mvc.perform(post("/api/listings/{id}/applications", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(body(application())))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("submitted")).andReturn();
    return json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
  }
  @AfterEach void cleanupFiles() throws Exception { for (Path file : files) Files.deleteIfExists(file); }

  @Test void applicationPersistsAndProviderDecisionReachesStudent() throws Exception {
    Long id = apply();
    assertThat(entries.count()).isEqualTo(1);
    mvc.perform(get("/api/applications").header("Authorization", "Bearer " + owner)).andExpect(jsonPath("$[0].id").value(id));
    mvc.perform(post("/api/applications/{id}/status", id).header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "accepted"))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted"));
    mvc.perform(get("/api/applications").header("Authorization", "Bearer " + student)).andExpect(jsonPath("$[0].status").value("accepted"));
  }
  @Test void duplicateApplicationIsRejectedAndWithdrawnCannotBeAccepted() throws Exception {
    Long id = apply();
    mvc.perform(post("/api/listings/{id}/applications", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(body(application())))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/applications/{id}/status", id).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "withdrawn"))))
        .andExpect(status().isOk());
    mvc.perform(post("/api/applications/{id}/status", id).header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "accepted"))))
        .andExpect(status().isConflict());
  }
  @Test void applicationAndDocumentAccessIsScopedToParticipants() throws Exception {
    Long id = apply();
    mvc.perform(get("/api/applications").header("Authorization", "Bearer " + stranger)).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(post("/api/applications/{id}/status", id).header("Authorization", "Bearer " + stranger).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "accepted"))))
        .andExpect(status().isForbidden());
    mvc.perform(multipart("/api/applications/{id}/documents", id).file(new MockMultipartFile("file", "proof.pdf", "application/pdf", "%PDF-1.4 proof".getBytes())).header("Authorization", "Bearer " + stranger))
        .andExpect(status().isForbidden());
  }
  @Test void documentUploadAndAuthenticatedDownloadPreserveBytes() throws Exception {
    Long id = apply(); byte[] bytes = "%PDF-1.4\nIntegration supporting document".getBytes();
    var result = mvc.perform(multipart("/api/applications/{id}/documents", id).file(new MockMultipartFile("file", "proof.pdf", "application/pdf", bytes)).header("Authorization", "Bearer " + student))
        .andExpect(status().isOk()).andExpect(jsonPath("$.documents[0].name").value("proof.pdf")).andReturn();
    String url = json.readTree(result.getResponse().getContentAsString()).at("/documents/0/url").asText();
    files.add(Path.of("uploads-test/housing-integration/applications", url.substring(url.lastIndexOf('/') + 1)));
    mvc.perform(get("/api" + url).header("Authorization", "Bearer " + owner)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
    mvc.perform(get("/api" + url).header("Authorization", "Bearer " + stranger)).andExpect(status().isForbidden());
    mvc.perform(get("/api" + url)).andExpect(status().isUnauthorized());
  }
  @Test void invalidUploadsAndOversizedDocumentsAreRejected() throws Exception {
    Long id = apply();
    mvc.perform(multipart("/api/applications/{id}/documents", id).file(new MockMultipartFile("file", "fake.pdf", "application/pdf", "<script>alert(1)</script>".getBytes())).header("Authorization", "Bearer " + student))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FILE_TYPE"));
    mvc.perform(multipart("/api/applications/{id}/documents", id).file(new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1])).header("Authorization", "Bearer " + student))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FILE_SIZE"));
  }
  @Test void concernPersistsAndOnlyAdminCanResolveIt() throws Exception {
    var result = mvc.perform(post("/api/listings/{id}/reports", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("note", "Incorrect advertised room information"))))
        .andExpect(status().isCreated()).andReturn();
    long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(get("/api/reports").header("Authorization", "Bearer " + stranger)).andExpect(jsonPath("$.length()").value(0));
    mvc.perform(post("/api/reports/{id}/status", id).header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "resolved")))).andExpect(status().isForbidden());
    mvc.perform(post("/api/reports/{id}/status", id).header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "resolved")))).andExpect(status().isOk());
    mvc.perform(get("/api/reports").header("Authorization", "Bearer " + student)).andExpect(jsonPath("$[0].status").value("resolved"));
  }
  @Test void reviewsArePersistentPublicAndUniquePerStudent() throws Exception {
    String review = body(Map.of("note", "Good study environment", "rating", 4));
    mvc.perform(post("/api/listings/{id}/reviews", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(review)).andExpect(status().isCreated());
    mvc.perform(get("/api/listings/{id}/reviews", listingId)).andExpect(status().isOk()).andExpect(jsonPath("$[0].rating").value(4)).andExpect(jsonPath("$[0].name").value("Student")).andExpect(jsonPath("$[0].email").doesNotExist());
    mvc.perform(post("/api/listings/{id}/reviews", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(review)).andExpect(status().isConflict());
  }
  @Test void validatesRatingsDatesAndStudentOnlySubmission() throws Exception {
    mvc.perform(post("/api/listings/{id}/reviews", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("note", "Bad rating", "rating", 6)))).andExpect(status().isBadRequest());
    mvc.perform(post("/api/listings/{id}/applications", listingId).header("Authorization", "Bearer " + student).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("note", "Need a room", "moveIn", LocalDate.now().minusDays(1).toString())))).andExpect(status().isBadRequest());
    mvc.perform(post("/api/listings/{id}/applications", listingId).header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON).content(body(application()))).andExpect(status().isForbidden());
    mvc.perform(post("/api/listings/{id}/reports", listingId).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("note", "A concern")))).andExpect(status().isUnauthorized());
  }
  @Test void officialResidenceSeedContainsThreePhotosAndIsRepeatable() {
    seed.seed(); long count = listings.count(); seed.seed(); assertThat(listings.count()).isEqualTo(count);
    var actual = listings.findAll().stream().filter(row -> List.of("St Peters Residence", "Hanover Residence", "Thibault Square - Vogue House", "Ruskin House").contains(row.getTitle())).toList();
    assertThat(actual).hasSize(4);
    for (var row : actual) { assertThat(row.getImageUrls()).hasSize(3); assertThat(row.getImageUrls()).allMatch(url -> url.startsWith("https://files.kuula.io/")); }
  }
}
