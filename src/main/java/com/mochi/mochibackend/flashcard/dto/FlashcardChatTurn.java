package com.mochi.mochibackend.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One prior turn in a "Ask Mochi about this deck" conversation, as
 * replayed by the client for context on each new question. Nothing
 * about deck chat is persisted server-side (see FlashcardChatService's
 * javadoc), so the frontend is the source of truth for history — this
 * is just that history's wire shape.
 */
public record FlashcardChatTurn(
        @NotBlank
        @Pattern(regexp = "user|assistant", message = "role must be 'user' or 'assistant'")
        String role,

        @NotBlank
        @Size(max = 2000, message = "A single message can't exceed 2000 characters")
        String content
) {
}