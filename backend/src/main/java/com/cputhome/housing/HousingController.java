package com.cputhome.housing;

import com.cputhome.security.UserPrincipal;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class HousingController {
  private final HousingService service;
  public HousingController(HousingService service) { this.service = service; }
  public record Submission(String note, Integer rating, LocalDate moveIn) {}
  public record Decision(String status) {}
  @PostMapping("/listings/{id}/applications") @ResponseStatus(HttpStatus.CREATED)
  public HousingService.Entry apply(@PathVariable Long id, @RequestBody Submission body, @AuthenticationPrincipal UserPrincipal user) {
    return service.create("APPLICATION", id, user, body.note(), null, body.moveIn());
  }
  @GetMapping("/applications")
  public List<HousingService.Entry> applications(@AuthenticationPrincipal UserPrincipal user) { return service.inbox("APPLICATION", user); }
  @PostMapping("/applications/{id}/status")
  public HousingService.Entry decide(@PathVariable Long id, @RequestBody Decision body, @AuthenticationPrincipal UserPrincipal user) {
    return service.status(id, "APPLICATION", body.status(), user);
  }
  @PostMapping(value = "/applications/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public HousingService.Entry upload(@PathVariable Long id, @RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal user) throws IOException {
    return service.upload(id, file, user);
  }
  @GetMapping("/applications/{id}/documents/{filename}")
  public ResponseEntity<byte[]> download(@PathVariable Long id, @PathVariable String filename, @AuthenticationPrincipal UserPrincipal user) throws IOException {
    String type = filename.endsWith(".pdf") ? "application/pdf" : filename.endsWith(".png") ? "image/png" : "image/jpeg";
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document" + filename.substring(filename.lastIndexOf('.')) + "\"")
        .header(HttpHeaders.CACHE_CONTROL, "no-store").body(service.download(id, filename, user));
  }
  @PostMapping("/listings/{id}/reports") @ResponseStatus(HttpStatus.CREATED)
  public HousingService.Entry report(@PathVariable Long id, @RequestBody Submission body, @AuthenticationPrincipal UserPrincipal user) {
    return service.create("REPORT", id, user, body.note(), null, null);
  }
  @GetMapping("/reports")
  public List<HousingService.Entry> reports(@AuthenticationPrincipal UserPrincipal user) { return service.inbox("REPORT", user); }
  @PostMapping("/reports/{id}/status")
  public HousingService.Entry resolve(@PathVariable Long id, @RequestBody Decision body, @AuthenticationPrincipal UserPrincipal user) {
    return service.status(id, "REPORT", body.status(), user);
  }
  @PostMapping("/listings/{id}/reviews") @ResponseStatus(HttpStatus.CREATED)
  public HousingService.Entry review(@PathVariable Long id, @RequestBody Submission body, @AuthenticationPrincipal UserPrincipal user) {
    return service.create("REVIEW", id, user, body.note(), body.rating(), null);
  }
  @GetMapping("/listings/{id}/reviews")
  public List<HousingService.Review> reviews(@PathVariable Long id) { return service.reviews(id); }
}
