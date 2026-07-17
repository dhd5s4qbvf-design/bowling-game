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
                .filter(FrameResult::isComplete)
                .mapToInt(FrameResult::getScore)
                .sum();
        return new GameState(new ArrayList<>(rolls), frames, total, isGameOver());
    }

    public boolean isGameOver() {
        List<FrameResult> frames = calculateFrames(rolls);
        return frames.size() == FRAME_COUNT && frames.get(FRAME_COUNT - 1).isComplete();
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
        if (frames.isEmpty() || frames.get(frames.size() - 1).isComplete()) {
            return; // First roll or starting new frame
        }

        FrameResult currentFrame = frames.get(frames.size() - 1);
        List<Integer> frameRolls = currentFrame.getRolls();

        if (currentFrame.getFrameNumber() < FRAME_COUNT) {
            validateRegularFrame(frameRolls, pins);
        } else {
            validateTenthFrame(frameRolls, pins);
        }
    }

    private void validateRegularFrame(List<Integer> frameRolls, int pins) {
        if (frameRolls.isEmpty()) return;

        // If frame already has 2 rolls, the next roll is for a new frame
        if (frameRolls.size() >= 2) return;

        int firstRoll = frameRolls.get(0);
        if (firstRoll == MAX_PINS) return; // Strike - next roll is for next frame

        if (firstRoll + pins > MAX_PINS) {
            throw new IllegalArgumentException(
                    String.format("Invalid roll: %d + %d exceeds %d pins for this frame.",
                            firstRoll, pins, MAX_PINS));
        }
    }

    private void validateTenthFrame(List<Integer> frameRolls, int pins) {
        int rollCount = frameRolls.size();

        if (rollCount == 1) {
            int firstRoll = frameRolls.get(0);
            if (firstRoll != MAX_PINS && firstRoll + pins > MAX_PINS) {
                throw new IllegalArgumentException(
                        String.format("Invalid 10th frame roll: %d + %d exceeds %d (strikes reset pins).",
                                firstRoll, pins, MAX_PINS));
            }
        } else if (rollCount == 2) {
            int firstRoll = frameRolls.get(0);
            int secondRoll = frameRolls.get(1);

            // After a strike, second roll was fresh pins. If second wasn't a strike, third roll can't exceed remaining
            if (firstRoll == MAX_PINS && secondRoll != MAX_PINS && secondRoll + pins > MAX_PINS) {
                throw new IllegalArgumentException(
                        String.format("Invalid 10th frame bonus: %d + %d exceeds %d (strikes reset pins).",
                                secondRoll, pins, MAX_PINS));
            }
            // After a spare (first two rolls), third roll gets fresh pins - any value 0-10 is valid
        }
    }

    /**
     * Processes rolls into frames and calculates scores.
     * Frames waiting for bonus rolls remain incomplete with null scores.
     */
    private List<FrameResult> calculateFrames(List<Integer> allRolls) {
        List<FrameResult> frames = new ArrayList<>();
        int rollIndex = 0;
        int runningTotal = 0;

        for (int frameNumber = 1; frameNumber <= FRAME_COUNT; frameNumber++) {
            if (rollIndex >= allRolls.size()) break;

            FrameResult frame = new FrameResult(frameNumber);

            if (frameNumber < FRAME_COUNT) {
                rollIndex = processRegularFrame(allRolls, rollIndex, frame);
            } else {
                rollIndex = processTenthFrame(allRolls, rollIndex, frame);
            }

            if (frame.isComplete()) {
                runningTotal += frame.getScore();
                frame.setRunningTotal(runningTotal);
            }
            frames.add(frame);
        }
        return frames;
    }

    private int processRegularFrame(List<Integer> allRolls, int rollIndex, FrameResult frame) {
        int firstRoll = allRolls.get(rollIndex);

        if (firstRoll == MAX_PINS) {
            // Strike
            frame.getRolls().add(firstRoll);
            frame.setStrike(true);
            if (rollIndex + 2 < allRolls.size()) {
                int bonus = allRolls.get(rollIndex + 1) + allRolls.get(rollIndex + 2);
                frame.setScore(MAX_PINS + bonus);
                frame.setComplete(true);
            }
            return rollIndex + 1;
        } else if (rollIndex + 1 < allRolls.size()) {
            // Two rolls available
            int secondRoll = allRolls.get(rollIndex + 1);
            frame.getRolls().add(firstRoll);
            frame.getRolls().add(secondRoll);

            if (firstRoll + secondRoll == MAX_PINS) {
                // Spare
                frame.setSpare(true);
                if (rollIndex + 2 < allRolls.size()) {
                    frame.setScore(MAX_PINS + allRolls.get(rollIndex + 2));
                    frame.setComplete(true);
                }
            } else {
                // Regular frame
                frame.setScore(firstRoll + secondRoll);
                frame.setComplete(true);
            }
            return rollIndex + 2;
        } else {
            // Only first roll available
            frame.getRolls().add(firstRoll);
            return rollIndex + 1;
        }
    }

    private int processTenthFrame(List<Integer> allRolls, int rollIndex, FrameResult frame) {
        // 10th frame: consume all remaining rolls (max 3)
        List<Integer> remaining = allRolls.subList(rollIndex, allRolls.size());
        frame.getRolls().addAll(remaining);

        if (remaining.size() >= 2) {
            boolean strike = remaining.get(0) == MAX_PINS;
            boolean spare = !strike && remaining.get(0) + remaining.get(1) == MAX_PINS;

            frame.setStrike(strike);
            frame.setSpare(spare);

            if (strike || spare) {
                // Need 3 rolls total
                if (remaining.size() == 3) {
                    frame.setScore(remaining.stream().mapToInt(Integer::intValue).sum());
                    frame.setComplete(true);
                }
            } else {
                // No strike/spare - only 2 rolls needed
                frame.setScore(remaining.get(0) + remaining.get(1));
                frame.setComplete(true);
            }
        }

        return allRolls.size();
    }
}
