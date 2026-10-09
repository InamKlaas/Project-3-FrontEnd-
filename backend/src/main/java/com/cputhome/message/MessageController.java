package com.cputhome.message;

import com.cputhome.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/* thin messaging doors, REST polling is enough for the POC */
@RestController
public class MessageController {

  private final MessageService service;

  public MessageController(MessageService service) {
    this.service = service;
  }

  @PostMapping("/api/listings/{id}/messages")
  public ResponseEntity<MessageResponse> send(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long id,
      @Valid @RequestBody SendMessageRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.send(id, principal, request));
  }

  @GetMapping("/api/listings/{id}/messages")
  public List<MessageResponse> thread(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long id,
      @RequestParam String studentId) {
    return service.thread(id, studentId, principal);
  }

  @GetMapping("/api/conversations")
  public List<ConversationResponse> inbox(@AuthenticationPrincipal UserPrincipal principal) {
    return service.inbox(principal);
  }
}
