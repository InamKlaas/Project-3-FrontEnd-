package com.cputhome.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* chat line body. sender always comes from the jwt, never from here.
 * landlords replying name the student side; students leave it empty. */
public record SendMessageRequest(
    @NotBlank(message = "message text is required")
        @Size(max = 2000, message = "message is too long")
        String text,
    @Size(max = 160, message = "student email is too long") String studentEmail) {}
