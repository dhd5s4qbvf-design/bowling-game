package com.example.bowling.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view of a single frame, used for the API response.
 * score / runningTotal stay {@code null} as long as the frame cannot
 * yet be fully scored (e.g. a strike waiting for its bonus rolls).
 */
public class FrameResult {

    private final int frameNumber;
    private final List<Integer> rolls = new ArrayList<>();
    private boolean strike;
    private boolean spare;
    private boolean complete;
    private Integer score;
    private Integer runningTotal;

    public FrameResult(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public List<Integer> getRolls() {
        return rolls;
    }

    public boolean isStrike() {
        return strike;
    }

    public void setStrike(boolean strike) {
        this.strike = strike;
    }

    public boolean isSpare() {
        return spare;
    }

    public void setSpare(boolean spare) {
        this.spare = spare;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRunningTotal() {
        return runningTotal;
    }

    public void setRunningTotal(Integer runningTotal) {
        this.runningTotal = runningTotal;
    }
}
