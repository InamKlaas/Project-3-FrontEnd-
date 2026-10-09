package com.cputhome.message;

import java.time.Instant;

/* one chat line, safe to show to either participant */
public record MessageResponse(
    Long id,
    Long listingId,
    String student,
    String from,
    String text,
    Instant createdAt) {

  public static MessageResponse from(Message message) {
    return new MessageResponse(
        message.getId(),
        message.getListing().getId(),
        message.getStudent().getEmail(),
        message.getSender().getEmail(),
        message.getText(),
        message.getCreatedAt());
  }
}
