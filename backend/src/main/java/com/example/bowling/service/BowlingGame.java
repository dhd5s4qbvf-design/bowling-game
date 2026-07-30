package com.example.bowling.service;

import com.example.bowling.model.FrameResult;
import com.example.bowling.model.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateful single-player bowling game following standard ten-pin rules.
 * Maintains a minimal roll sequence and derives frames/scores on demand.
 */
public class BowlingGame {

    private static final int FRAME_COUNT = 10;
    private static final int MAX_PINS = 10;

    private final List<Integer> rolls = new ArrayList<>();

    /**
     * Records a single roll. Validates pin count, game state, and frame constraints.
     *
     * @throws IllegalArgumentException if pins is invalid or violates frame constraints
     * @throws IllegalStateException if game is already complete
     */
    public void roll(int pins) {
        validatePinCount(pins);

        List<FrameResult> frames = calculateFrames(rolls);
        validateGameNotOver(frames);
        validateFrameConstraints(frames, pins);

        rolls.add(pins);
    }

    /**
     * Returns current game state with all frames and scores.
     * Incomplete frames (waiting for bonus rolls) will have null scores.
     */
    public GameState getState() {
        List<FrameResult> frames = calculateFrames(rolls);
        int total = frames.stream()
                .filter(FrameResult::complete)
                .mapToInt(FrameResult::score)
                .sum();
        boolean gameOver = isGameOver(frames);
        return new GameState(new ArrayList<>(rolls), frames, total, gameOver,
                computeMaxPinsForNextRoll(frames, gameOver));
    }

    /**
     * Returns the maximum pin count that would be accepted for the next roll,
     * given standard ten-pin frame constraints. Used both to validate incoming
     * rolls and to let clients render legal inputs without re-implementing
     * these rules themselves.
     */
    public int getMaxPinsForNextRoll() {
        List<FrameResult> frames = calculateFrames(rolls);
        return computeMaxPinsForNextRoll(frames, isGameOver(frames));
    }

    private int computeMaxPinsForNextRoll(List<FrameResult> frames, boolean gameOver) {
        if (gameOver) {
            return 0;
        }

        if (frames.isEmpty() || frames.get(frames.size() - 1).complete()) {
            return MAX_PINS; // First roll or starting a fresh frame
        }

        FrameResult currentFrame = frames.get(frames.size() - 1);
        List<Integer> frameRolls = currentFrame.rolls();

        return currentFrame.frameNumber() < FRAME_COUNT
                ? maxPinsRegularFrame(frameRolls)
                : maxPinsTenthFrame(frameRolls);
    }

    private int maxPinsRegularFrame(List<Integer> frameRolls) {
        // First roll, strike, or spare awaiting its bonus roll - full rack available
        if (frameRolls.isEmpty() || frameRolls.size() >= 2 || frameRolls.get(0) == MAX_PINS) {
            return MAX_PINS;
        }
        // Second roll - limited to what's left standing
        return clampToRack(MAX_PINS - frameRolls.get(0));
    }

    /**
     * Computes the max legal pins for the 10th frame given its rolls so far.
     * Self-contained: correct for any frameRolls it's handed, not just the
     * ones the current caller happens to pass (an already-open/complete frame
     * or a fully-rolled one both correctly yield 0, no third roll is legal).
     * Package-private so this invariant can be unit tested directly, since
     * the public API (getMaxPinsForNextRoll/getState) never hands it such
     * input - isGameOver() already returns 0 before reaching it.
     */
    int maxPinsTenthFrame(List<Integer> frameRolls) {
        if (frameRolls.isEmpty()) {
            return MAX_PINS;
        }

        if (frameRolls.size() == 1) {
            int first = frameRolls.get(0);
            return first == MAX_PINS ? MAX_PINS : clampToRack(MAX_PINS - first);
        }

        if (frameRolls.size() >= 3) {
            return 0; // all three rolls already made - nothing more is legal
        }

        int first = frameRolls.get(0);
        int second = frameRolls.get(1);
        boolean strike = first == MAX_PINS;
        boolean twoStrikes = strike && second == MAX_PINS;
        boolean spare = !strike && first + second == MAX_PINS;

        if (twoStrikes || spare) {
            return MAX_PINS; // fresh pins for roll 3
        }
        if (strike) {
            return clampToRack(MAX_PINS - second); // strike then non-strike: limited by roll 2
        }
        return 0; // open frame (no strike/spare) is already complete - no roll 3 legal
    }

    /**
     * Keeps a pin-count calculation within the legal 0-10 range. Every reachable
     * branch above already stays in range; this exists so the function is
     * obviously total rather than relying on that being proven by inspection.
     */
    private int clampToRack(int pins) {
        return Math.max(0, Math.min(MAX_PINS, pins));
    }

    public boolean isGameOver() {
        return isGameOver(calculateFrames(rolls));
    }

    private boolean isGameOver(List<FrameResult> frames) {
        return frames.size() == FRAME_COUNT && frames.get(FRAME_COUNT - 1).complete();
    }

    private void validatePinCount(int pins) {
        if (pins < 0 || pins > MAX_PINS) {
            throw new IllegalArgumentException(
                    String.format("Invalid pin count: %d. Must be between 0 and %d.", pins, MAX_PINS));
        }
    }

    private void validateGameNotOver(List<FrameResult> frames) {
        if (isGameOver(frames)) {
            throw new IllegalStateException("Cannot roll: game is already complete.");
        }
    }

    private void validateFrameConstraints(List<FrameResult> frames, int pins) {
        // isGameOver(frames) is re-derived rather than assumed false: this
        // method must stay correct even if called independently of
        // validateGameNotOver(frames), not just when roll() happens to call
        // them in sequence.
        int maxPins = computeMaxPinsForNextRoll(frames, isGameOver(frames));
        if (pins > maxPins) {
            throw new IllegalArgumentException(
                    String.format("Invalid roll: %d pins exceeds %d pins available for this roll.",
                            pins, maxPins));
        }
    }

    /**
     * Processes rolls into frames and calculates scores.
     * Frames waiting for bonus rolls remain incomplete with null scores.
     * Uses immutable FrameResult.Builder to construct frames.
     */
    private List<FrameResult> calculateFrames(List<Integer> allRolls) {
        List<FrameResult> frames = new ArrayList<>();
        int rollIndex = 0;
        int runningTotal = 0;

        for (int frameNumber = 1; frameNumber <= FRAME_COUNT; frameNumber++) {
            if (rollIndex >= allRolls.size()) break;

            FrameResultData data;
            if (frameNumber < FRAME_COUNT) {
                data = processRegularFrame(allRolls, rollIndex);
            } else {
                data = processTenthFrame(allRolls, rollIndex);
            }

            // Update running total if frame is complete
            Integer finalRunningTotal = null;
            if (data.complete && data.score != null) {
                runningTotal += data.score;
                finalRunningTotal = runningTotal;
            }

            // Build immutable FrameResult
            FrameResult frame = FrameResult.builder()
                .frameNumber(frameNumber)
                .rolls(data.rolls)
                .strike(data.strike)
                .spare(data.spare)
                .complete(data.complete)
                .score(data.score)
                .runningTotal(finalRunningTotal)
                .build();

            frames.add(frame);
            rollIndex = data.nextRollIndex;
        }
        return frames;
    }

    /**
     * Helper record to transfer frame calculation data.
     * Avoids mutation by returning all data at once.
     */
    private record FrameResultData(
        List<Integer> rolls,
        boolean strike,
        boolean spare,
        boolean complete,
        Integer score,
        int nextRollIndex
    ) {}

    private FrameResultData processRegularFrame(List<Integer> allRolls, int rollIndex) {
        int firstRoll = allRolls.get(rollIndex);
        List<Integer> rolls = new ArrayList<>();
        boolean strike = false;
        boolean spare = false;
        boolean complete = false;
        Integer score = null;
        int nextRollIndex;

        if (firstRoll == MAX_PINS) {
            // Strike
            rolls.add(firstRoll);
            strike = true;
            if (rollIndex + 2 < allRolls.size()) {
                int bonus = allRolls.get(rollIndex + 1) + allRolls.get(rollIndex + 2);
                score = MAX_PINS + bonus;
                complete = true;
            }
            nextRollIndex = rollIndex + 1;
        } else if (rollIndex + 1 < allRolls.size()) {
            // Two rolls available
            int secondRoll = allRolls.get(rollIndex + 1);
            rolls.add(firstRoll);
            rolls.add(secondRoll);

            if (firstRoll + secondRoll == MAX_PINS) {
                // Spare
                spare = true;
                if (rollIndex + 2 < allRolls.size()) {
                    score = MAX_PINS + allRolls.get(rollIndex + 2);
                    complete = true;
                }
            } else {
                // Regular frame
                score = firstRoll + secondRoll;
                complete = true;
            }
            nextRollIndex = rollIndex + 2;
        } else {
            // Only first roll available
            rolls.add(firstRoll);
            nextRollIndex = rollIndex + 1;
        }

        return new FrameResultData(rolls, strike, spare, complete, score, nextRollIndex);
    }

    private FrameResultData processTenthFrame(List<Integer> allRolls, int rollIndex) {
        // 10th frame: consume all remaining rolls (max 3)
        List<Integer> remaining = allRolls.subList(rollIndex, allRolls.size());
        List<Integer> rolls = new ArrayList<>(remaining);
        boolean strike = false;
        boolean spare = false;
        boolean complete = false;
        Integer score = null;

        if (remaining.size() >= 2) {
            strike = remaining.get(0) == MAX_PINS;
            spare = !strike && remaining.get(0) + remaining.get(1) == MAX_PINS;

            if (strike || spare) {
                // Need 3 rolls total
                if (remaining.size() == 3) {
                    score = remaining.stream().mapToInt(Integer::intValue).sum();
                    complete = true;
                }
            } else {
                // No strike/spare - only 2 rolls needed
                score = remaining.get(0) + remaining.get(1);
                complete = true;
            }
        }

        return new FrameResultData(rolls, strike, spare, complete, score, allRolls.size());
    }
}
