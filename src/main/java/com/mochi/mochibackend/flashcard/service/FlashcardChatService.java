package com.mochi.mochibackend.flashcard.service;

import com.mochi.mochibackend.flashcard.dto.FlashcardChatTurn;
import com.mochi.mochibackend.flashcard.dto.FlashcardDeckDetailResponse;
import com.mochi.mochibackend.flashcard.dto.FlashcardResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Powers the "Ask Mochi" panel that sits next to the flip-card view in
 * FlashcardViewer — free-form Q&amp;A about the topic a deck was
 * generated from.
 * <p>
 * Deliberately reuses {@link FlashcardGenerationClient} — the exact
 * same provider-agnostic AI client {@link FlashcardGenerationService}
 * calls to build decks in the first place — instead of adding a
 * second AI integration. Whatever is already configured via
 * {@code mochi.ai.huggingface.*} / {@code mochi.ai.openai.*} powers
 * this too, with the same error handling (missing key, rate limit,
 * unreachable provider) already built into that client, surfaced the
 * same way through {@code FlashcardGenerationException} ->
 * {@code GlobalExceptionHandler}.
 * <p>
 * The raw uploaded document is never persisted (see
 * {@code FlashcardDeckService#generate} — only the AI summary and the
 * generated cards are saved to keep the schema small), so there's no
 * "original text" to hand back to the model here. Instead the
 * "topic" context sent on every question is reconstructed from the
 * deck's title, its AI summary, and every card's front/back — between
 * them a solid stand-in for the source material for Q&amp;A purposes,
 * without a migration to store potentially large raw document blobs.
 * <p>
 * Nothing about the conversation itself is persisted either — the
 * frontend (useDeckChat) holds the message list in memory and replays
 * recent turns back on each question so the model has continuity
 * within a study session; a page refresh starts a fresh conversation.
 */
@Service
public class FlashcardChatService {

    /** Only the most recent turns are replayed back to the model — keeps prompts small and cheap no matter how long a study session runs. */
    private static final int MAX_HISTORY_TURNS = 8;
    private static final int MAX_HISTORY_TURN_CHARS = 600;

    private final FlashcardDeckService deckService;
    private final FlashcardGenerationClient client;

    public FlashcardChatService(FlashcardDeckService deckService, FlashcardGenerationClient client) {
        this.deckService = deckService;
        this.client = client;
    }

    /**
     * Answers one question about {@code deckId}'s topic on behalf of
     * {@code userId}. {@link FlashcardDeckService#getOwned} both
     * resolves the deck and enforces the same ownership check every
     * other deck endpoint uses — a stranger can't ask about someone
     * else's deck any more than they can open it.
     */
    public String askAboutDeck(String userId, Long deckId, String question, List<FlashcardChatTurn> history) {
        FlashcardDeckDetailResponse deck = deckService.getOwned(userId, deckId);

        String systemPrompt = """
                You are Mochi, a friendly, encouraging study assistant chatting next to a student's flashcard deck.
                Answer using ONLY the study material provided below (the deck's title, summary, and flashcards) plus
                the conversation so far. Explain things clearly and a little playfully, like a supportive study buddy.
                If the answer truly isn't in the material, say so honestly instead of inventing facts, and suggest
                what the student could look up next.
                Keep answers focused: a few short sentences, or a brief list for multi-part questions. Plain text
                only — no markdown headers, no code fences.
                """;

        String userPrompt = buildUserPrompt(deck, trimHistory(history), question);
        String raw = client.generate(systemPrompt, userPrompt);
        return raw == null ? "" : raw.strip();
    }

    private String buildUserPrompt(FlashcardDeckDetailResponse deck, List<FlashcardChatTurn> history, String question) {
        StringBuilder sb = new StringBuilder();

        sb.append("STUDY MATERIAL\n");
        sb.append("Title: ").append(deck.title()).append('\n');
        if (deck.aiSummary() != null && !deck.aiSummary().isBlank()) {
            sb.append("Summary: ").append(deck.aiSummary()).append('\n');
        }
        sb.append("Flashcards:\n");
        for (FlashcardResponse card : deck.cards()) {
            sb.append("- ").append(card.front()).append(" -> ").append(card.back()).append('\n');
        }

        if (!history.isEmpty()) {
            sb.append("\nCONVERSATION SO FAR\n");
            for (FlashcardChatTurn turn : history) {
                String speaker = "assistant".equals(turn.role()) ? "Mochi" : "Student";
                sb.append(speaker).append(": ").append(truncate(turn.content(), MAX_HISTORY_TURN_CHARS)).append('\n');
            }
        }

        sb.append("\nStudent's new question: ").append(question.strip());
        return sb.toString();
    }

    /** Keeps only the tail of the conversation — oldest turns are the least relevant to a follow-up question anyway. */
    private List<FlashcardChatTurn> trimHistory(List<FlashcardChatTurn> history) {
        if (history == null || history.isEmpty()) return List.of();
        int from = Math.max(0, history.size() - MAX_HISTORY_TURNS);
        return history.subList(from, history.size());
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        String stripped = text.strip();
        return stripped.length() <= max ? stripped : stripped.substring(0, max) + "…";
    }
}