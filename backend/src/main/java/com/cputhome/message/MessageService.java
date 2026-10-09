package com.cputhome.message;

import com.cputhome.common.BadRequestException;
import com.cputhome.common.ForbiddenException;
import com.cputhome.common.NotFoundException;
import com.cputhome.listing.Accommodation;
import com.cputhome.listing.AccommodationRepository;
import com.cputhome.security.UserPrincipal;
import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import com.cputhome.user.UserRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* two-way threads between exactly one student and one listing owner */
@Service
public class MessageService {

  /* thread history stays readable, nobody scrolls further in the POC */
  private static final int HISTORY_CAP = 100;

  private final MessageRepository messages;
  private final AccommodationRepository accommodations;
  private final UserRepository users;

  public MessageService(
      MessageRepository messages,
      AccommodationRepository accommodations,
      UserRepository users) {
    this.messages = messages;
    this.accommodations = accommodations;
    this.users = users;
  }

  @Transactional
  public MessageResponse send(Long listingId, UserPrincipal principal, SendMessageRequest request) {
    Accommodation listing =
        accommodations.findById(listingId).orElseThrow(() -> new NotFoundException("listing not found"));
    User sender =
        users.findById(principal.id()).orElseThrow(() -> new NotFoundException("user not found"));

    User student;
    if (principal.role() == UserRole.STUDENT) {
      /* students open threads on listings they may see */
      if (!visibleTo(listing, principal)) {
        throw new NotFoundException("listing not found");
      }
      student = sender;
    } else if (principal.role() == UserRole.LANDLORD
        && listing.getOwner().getId().equals(principal.id())) {
      /* landlords reply inside threads on their own listings only */
      if (request.studentEmail() == null || request.studentEmail().isBlank()) {
        throw new BadRequestException("VALIDATION_ERROR", "studentEmail is required to reply");
      }
      student =
          users
              .findByEmail(request.studentEmail().trim().toLowerCase(java.util.Locale.ROOT))
              .orElseThrow(() -> new NotFoundException("student not found"));
      if (student.getRole() != UserRole.STUDENT) {
        throw new BadRequestException("VALIDATION_ERROR", "threads only run with students");
      }
    } else {
      throw new ForbiddenException("FORBIDDEN", "you may not message about this listing");
    }
    Message saved = messages.save(new Message(listing, student, sender, request.text().trim()));
    return MessageResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> thread(Long listingId, String studentEmail, UserPrincipal principal) {
    Accommodation listing =
        accommodations.findById(listingId).orElseThrow(() -> new NotFoundException("listing not found"));
    User student =
        users
            .findByEmail(studentEmail.trim().toLowerCase(java.util.Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("student not found"));
    requireParticipant(listing, student, principal);
    return messages.findByListingIdAndStudentIdOrderByCreatedAtAsc(listingId, student.getId()).stream()
        .limit(HISTORY_CAP)
        .map(MessageResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ConversationResponse> inbox(UserPrincipal principal) {
    Map<String, ConversationResponse> threads = new LinkedHashMap<>();
    if (principal.role() == UserRole.STUDENT) {
      for (Message message : messages.findByStudentIdOrderByCreatedAtDesc(principal.id())) {
        threads.putIfAbsent(
            message.getListing().getId() + "|" + principal.id(),
            new ConversationResponse(
                message.getListing().getId(),
                message.getStudent().getEmail(),
                message.getListing().getTitle()));
      }
      return new ArrayList<>(threads.values());
    }
    if (principal.role() == UserRole.LANDLORD) {
      for (Message message : messages.findByListingOwnerIdOrderByCreatedAtDesc(principal.id())) {
        threads.putIfAbsent(
            message.getListing().getId() + "|" + message.getStudent().getId(),
            new ConversationResponse(
                message.getListing().getId(),
                message.getStudent().getEmail(),
                message.getListing().getTitle()));
      }
      return new ArrayList<>(threads.values());
    }
    return List.of();
  }

  private void requireParticipant(Accommodation listing, User student, UserPrincipal principal) {
    boolean self = student.getId().equals(principal.id());
    boolean owner = listing.getOwner().getId().equals(principal.id());
    boolean admin = principal.role() == UserRole.ADMIN;
    if (!self && !owner && !admin) {
      throw new NotFoundException("conversation not found");
    }
  }

  private static boolean visibleTo(Accommodation accommodation, UserPrincipal viewer) {
    return (accommodation.isPublished()
            && accommodation.isActive()
            && "approved".equals(accommodation.getApprovalStatus()))
        || accommodation.getOwner().getId().equals(viewer.id())
        || viewer.role() == UserRole.ADMIN;
  }
}
