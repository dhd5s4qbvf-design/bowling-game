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
 * @author Bowling Game Kata
 * @version 1.0
 */
@Schema(description = "Current state of a bowling game including rolls, frames, and score")
public class GameState {

    private final List<Integer> rolls;
    private final List<FrameResult> frames;
    private final int totalScore;
    private final boolean gameOver;

    /**
     * Creates a new game state snapshot.
     *
     * @param rolls      the sequence of all rolls made in the game
     * @param frames     the calculated frame results
     * @param totalScore the sum of all completed frame scores
     * @param gameOver   whether the game has been completed
     */
    public GameState(List<Integer> rolls, List<FrameResult> frames, int totalScore, boolean gameOver) {
        this.rolls = rolls;
        this.frames = frames;
        this.totalScore = totalScore;
        this.gameOver = gameOver;
    }

    /**
     * Returns the sequence of all rolls made in this game.
     *
     * @return an immutable list of pin counts for each roll
     */
    @Schema(description = "Array of all rolls in the game", example = "[10, 7, 3, 9, 0]")
    public List<Integer> getRolls() {
        return rolls;
    }

    /**
     * Returns the calculated frame results.
     *
     * <p>Each frame contains its rolls, score (if calculable), and completion status.
     * Frames waiting for bonus rolls will have {@code null} scores.
     *
     * @return an immutable list of frame results
     */
    @Schema(description = "Array of frame results with individual scores")
    public List<FrameResult> getFrames() {
        return frames;
    }

    /**
     * Returns the current total score.
     *
     * <p>This is the sum of all frames that have been completely scored.
     * Incomplete frames (e.g., strikes waiting for bonus rolls) are not included.
     *
     * @return the total score of all completed frames
     */
    @Schema(description = "Total score so far", example = "150", minimum = "0", maximum = "300")
    public int getTotalScore() {
        return totalScore;
    }

    /**
     * Indicates whether the game has been completed.
     *
     * <p>A game is complete when all 10 frames have been played, including
     * any bonus rolls in the 10th frame.
     *
     * @return {@code true} if the game is over, {@code false} otherwise
     */
    @Schema(description = "Whether the game has ended", example = "false")
    public boolean isGameOver() {
        return gameOver;
    }
}
