package com.cputhome.message;

/* inbox row, one per (listing, student) pair with activity */
public record ConversationResponse(Long listingId, String student, String title) {}
