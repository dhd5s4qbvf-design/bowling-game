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
        validateGameNotOver();
        validateFrameConstraints(pins);
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
        return new GameState(new ArrayList<>(rolls), frames, total, isGameOver());
    }

    public boolean isGameOver() {
        List<FrameResult> frames = calculateFrames(rolls);
        return frames.size() == FRAME_COUNT && frames.get(FRAME_COUNT - 1).complete();
    }

    private void validatePinCount(int pins) {
        if (pins < 0 || pins > MAX_PINS) {
            throw new IllegalArgumentException(
                    String.format("Invalid pin count: %d. Must be between 0 and %d.", pins, MAX_PINS));
        }
    }

    private void validateGameNotOver() {
        if (isGameOver()) {
            throw new IllegalStateException("Cannot roll: game is already complete.");
        }
    }

    private void validateFrameConstraints(int pins) {
        List<FrameResult> frames = calculateFrames(rolls);
        if (frames.isEmpty() || frames.get(frames.size() - 1).complete()) {
            return; // First roll or starting new frame
        }

        FrameResult currentFrame = frames.get(frames.size() - 1);
        List<Integer> frameRolls = currentFrame.rolls();

        if (currentFrame.frameNumber() < FRAME_COUNT) {
            validateRegularFrame(frameRolls, pins);
        } else {
            validateTenthFrame(frameRolls, pins);
        }
    }

    /**
     * Validates pin count for frames 1-9.
     * Rule: Second roll in a frame cannot exceed remaining pins (unless first roll was a strike).
     */
    private void validateRegularFrame(List<Integer> frameRolls, int pins) {
        // First roll or strike - no validation needed
        if (frameRolls.isEmpty() || frameRolls.size() >= 2 || frameRolls.get(0) == MAX_PINS) {
            return;
        }

        // Second roll validation
        int firstRoll = frameRolls.get(0);
        int remainingPins = MAX_PINS - firstRoll;

        if (pins > remainingPins) {
            throw new IllegalArgumentException(
                    String.format("Invalid roll: %d pins exceeds %d remaining pins in frame.",
                            pins, remainingPins));
        }
    }

    /**
     * Validates pin count for 10th frame.
     * Rules:
     * - Roll 1: Any value 0-10
     * - Roll 2: If roll 1 was strike, fresh pins (0-10). Otherwise, cannot exceed remaining.
     * - Roll 3: If roll 1 was strike and roll 2 was not, cannot exceed remaining from roll 2.
     *           If roll 1+2 was spare, fresh pins (0-10).
     */
    private void validateTenthFrame(List<Integer> frameRolls, int pins) {
        int rollCount = frameRolls.size();

        // Roll 2 validation
        if (rollCount == 1) {
            validateTenthFrameRoll2(frameRolls.get(0), pins);
            return;
        }

        // Roll 3 validation
        if (rollCount == 2) {
            validateTenthFrameRoll3(frameRolls.get(0), frameRolls.get(1), pins);
        }
    }

    private void validateTenthFrameRoll2(int firstRoll, int pins) {
        // After a strike, pins are reset - any value 0-10 is valid
        if (firstRoll == MAX_PINS) {
            return;
        }

        // No strike - must not exceed remaining pins
        int remainingPins = MAX_PINS - firstRoll;
        if (pins > remainingPins) {
            throw new IllegalArgumentException(
                    String.format("Invalid 10th frame roll 2: %d pins exceeds %d remaining pins.",
                            pins, remainingPins));
        }
    }

    private void validateTenthFrameRoll3(int firstRoll, int secondRoll, int pins) {
        boolean firstWasStrike = firstRoll == MAX_PINS;
        boolean secondWasStrike = secondRoll == MAX_PINS;
        boolean wasSpare = firstRoll + secondRoll == MAX_PINS;

        // After a spare, pins are reset - any value 0-10 is valid
        if (wasSpare) {
            return;
        }

        // After strike + strike, pins were reset for roll 3 - any value valid
        if (firstWasStrike && secondWasStrike) {
            return;
        }

        // After strike + non-strike, roll 3 cannot exceed remaining from roll 2
        if (firstWasStrike && !secondWasStrike) {
            int remainingPins = MAX_PINS - secondRoll;
            if (pins > remainingPins) {
                throw new IllegalArgumentException(
                        String.format("Invalid 10th frame roll 3: %d pins exceeds %d remaining pins.",
                                pins, remainingPins));
            }
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
                data = processRegularFrame(allRolls, rollIndex, frameNumber);
            } else {
                data = processTenthFrame(allRolls, rollIndex, frameNumber);
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

    private FrameResultData processRegularFrame(List<Integer> allRolls, int rollIndex, int frameNumber) {
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

    private FrameResultData processTenthFrame(List<Integer> allRolls, int rollIndex, int frameNumber) {
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
