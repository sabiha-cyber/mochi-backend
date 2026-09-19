package com.mochi.mochibackend.flashcard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Request payload for {@code POST /api/flashcard-decks/{id}/chat}. */
@Getter
@Setter
public class FlashcardChatRequest {

    @NotBlank(message = "Question can't be empty")
    @Size(max = 800, message = "Question can't exceed 800 characters")
    private String question;

    /**
     * Prior turns in this conversation, oldest first. Optional — the
     * frontend sends whatever it currently has in the panel; older
     * turns beyond what's useful are trimmed server-side in
     * FlashcardChatService, not here.
     */
    @Valid
    private List<FlashcardChatTurn> history = new ArrayList<>();
}