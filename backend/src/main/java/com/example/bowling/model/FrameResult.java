package com.example.bowling.model;

import java.util.List;

/**
 * Immutable view of a single frame in a bowling game.
 *
 * <p>This record represents a completely immutable snapshot of a frame's state.
 * The score and runningTotal remain {@code null} as long as the frame cannot
 * yet be fully scored (e.g., a strike waiting for its bonus rolls).
 *
 * <p><b>Immutability Guarantee:</b> This record is truly immutable:
 * <ul>
 *   <li>All fields are final (enforced by record)</li>
 *   <li>Lists are defensively copied in the compact constructor</li>
 *   <li>No setters exist (records are immutable by design)</li>
 *   <li>Thread-safe and safe to share across boundaries</li>
 * </ul>
 *
 * @param frameNumber  the frame number (1-10)
 * @param rolls        immutable list of pin counts for each roll in this frame
 * @param strike       whether this frame is a strike
 * @param spare        whether this frame is a spare
 * @param complete     whether this frame is complete (all rolls made, bonus rolls available)
 * @param score        the score for this frame alone (null if not yet calculable)
 * @param runningTotal the cumulative score up to and including this frame (null if not yet calculable)
 * @author Bowling Game Kata
 * @version 2.0 - Converted to immutable record
 */
public record FrameResult(
    int frameNumber,
    List<Integer> rolls,
    boolean strike,
    boolean spare,
    boolean complete,
    Integer score,
    Integer runningTotal
) {
    /**
     * Compact constructor with defensive copying to ensure immutability.
     */
    public FrameResult {
        // Defensive copy: prevent external mutation
        rolls = List.copyOf(rolls);
    }

    /**
     * Builder for creating FrameResult instances with a fluent API.
     * Necessary because records don't support traditional builder patterns.
     */
    public static class Builder {
        private int frameNumber;
        private List<Integer> rolls = List.of();
        private boolean strike = false;
        private boolean spare = false;
        private boolean complete = false;
        private Integer score = null;
        private Integer runningTotal = null;

        public Builder frameNumber(int frameNumber) {
            this.frameNumber = frameNumber;
            return this;
        }

        public Builder rolls(List<Integer> rolls) {
            this.rolls = rolls;
            return this;
        }

        public Builder addRoll(int pins) {
            // Create new list with added roll (immutable approach)
            var newRolls = new java.util.ArrayList<>(this.rolls);
            newRolls.add(pins);
            this.rolls = List.copyOf(newRolls);
            return this;
        }

        public Builder strike(boolean strike) {
            this.strike = strike;
            return this;
        }

        public Builder spare(boolean spare) {
            this.spare = spare;
            return this;
        }

        public Builder complete(boolean complete) {
            this.complete = complete;
            return this;
        }

        public Builder score(Integer score) {
            this.score = score;
            return this;
        }

        public Builder runningTotal(Integer runningTotal) {
            this.runningTotal = runningTotal;
            return this;
        }

        public FrameResult build() {
            return new FrameResult(frameNumber, rolls, strike, spare, complete, score, runningTotal);
        }
    }

    /**
     * Creates a new builder for constructing FrameResult instances.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
