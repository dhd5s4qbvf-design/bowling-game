package com.example.bowling.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Immutable snapshot of a bowling game's current state.
 *
 * <p>This class provides a read-only view of the game including:
 * <ul>
 *   <li>All rolls made so far</li>
 *   <li>Calculated frame results with scores</li>
 *   <li>Current total score (sum of completed frames)</li>
 *   <li>Game completion status</li>
 * </ul>
 *
 * <p>Instances are created by {@link com.example.bowling.service.BowlingGame#getState()}
 * and represent the state at the moment of creation. Subsequent rolls will not affect
 * this instance.
 *
 * <p><b>Immutability Guarantee:</b> This record is truly immutable. All list fields
 * are defensively copied to prevent external mutation. Java records provide:
 * <ul>
 *   <li>Automatic final fields</li>
 *   <li>Automatic getters (accessors)</li>
 *   <li>Automatic equals/hashCode/toString</li>
 *   <li>No setters (mutation impossible)</li>
 * </ul>
 *
 * @param rolls      immutable list of all rolls made in the game
 * @param frames     immutable list of calculated frame results
 * @param totalScore the sum of all completed frame scores
 * @param gameOver   whether the game has been completed
 * @author Bowling Game Kata
 * @version 2.0 - Converted to immutable record
 */
@Schema(description = "Current state of a bowling game including rolls, frames, and score")
public record GameState(
    @Schema(description = "Array of all rolls in the game", example = "[10, 7, 3, 9, 0]")
    List<Integer> rolls,

    @Schema(description = "Array of frame results with individual scores")
    List<FrameResult> frames,

    @Schema(description = "Total score so far", example = "150", minimum = "0", maximum = "300")
    int totalScore,

    @Schema(description = "Whether the game has ended", example = "false")
    boolean gameOver
) {
    /**
     * Compact constructor with defensive copying to ensure immutability.
     * Creates immutable copies of mutable collections passed in.
     */
    public GameState {
        // Defensive copy: prevent external mutation of lists
        rolls = List.copyOf(rolls);
        frames = List.copyOf(frames);
    }
}
