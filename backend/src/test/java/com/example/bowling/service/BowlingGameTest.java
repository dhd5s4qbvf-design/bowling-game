package com.example.bowling.service;

import com.example.bowling.model.FrameResult;
import com.example.bowling.model.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for {@link BowlingGame}.
 * Tests cover basic scenarios, strikes, spares, 10th frame rules, and validation.
 */
class BowlingGameTest {

    private void rollMany(BowlingGame game, int times, int pins) {
        for (int i = 0; i < times; i++) {
            game.roll(pins);
        }
    }

    // ========== Basic Scoring Tests ==========

    /**
     * Tests that a game with all gutter balls (0 pins) scores 0.
     */
    @Test
    void allGutterBallsScoreZero() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 20, 0);
        assertEquals(0, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    /**
     * Tests that knocking down 1 pin per roll for all 20 rolls scores 20.
     */
    @Test
    void allOnesScoreTwenty() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 20, 1);
        assertEquals(20, game.getState().totalScore());
    }

    // ========== Spare Tests ==========

    /**
     * Tests that a spare (10 pins in 2 rolls) is scored correctly with bonus.
     * Spare scores 10 + next roll (3) = 13, plus frame 2 score of 3 = total 16.
     */
    @Test
    void oneSpareIsScoredCorrectly() {
        BowlingGame game = new BowlingGame();
        game.roll(5);
        game.roll(5); // spare
        game.roll(3);
        rollMany(game, 17, 0);
        assertEquals(16, game.getState().totalScore());
    }

    /**
     * Tests that a game with all spares (5+5 per frame) scores 150.
     * Each frame: 10 + 5 (next roll) = 15, times 10 frames = 150.
     */
    @Test
    void allSpareGameScoresOneHundredFifty() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 20, 5);
        game.roll(5); // extra bonus roll for the 10th frame spare
        assertEquals(150, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    /**
     * Tests that an incomplete spare frame shows correct state (not complete, no score yet).
     */
    @Test
    void incompleteSpareFrameShowsCorrectState() {
        BowlingGame game = new BowlingGame();
        game.roll(4);
        game.roll(6); // spare, but no bonus yet
        GameState state = game.getState();
        assertEquals(1, state.frames().size());
        assertTrue(state.frames().get(0).spare());
        assertFalse(state.frames().get(0).complete());
        assertNull(state.frames().get(0).score());
        assertEquals(0, state.totalScore());
    }

    // ========== Strike Tests ==========

    /**
     * Tests that a strike (10 pins on first roll) is scored correctly with bonus.
     * Strike scores 10 + next 2 rolls (3 + 4) = 17, plus frame 2 score of 7 = total 24.
     */
    @Test
    void oneStrikeIsScoredCorrectly() {
        BowlingGame game = new BowlingGame();
        game.roll(10); // strike
        game.roll(3);
        game.roll(4);
        rollMany(game, 16, 0);
        assertEquals(24, game.getState().totalScore());
    }

    /**
     * Tests a perfect game (12 strikes) scores 300.
     */
    @Test
    void perfectGameScoresThreeHundred() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 12, 10);
        assertEquals(300, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    /**
     * Tests that consecutive strikes (turkey) calculate bonuses correctly.
     * Frame 1: 10 + 10 + 10 = 30, Frame 2: 10 + 10 + 5 = 25, Frame 3: 10 + 5 + 3 = 18.
     */
    @Test
    void multipleConsecutiveStrikesScoreCorrectly() {
        BowlingGame game = new BowlingGame();
        game.roll(10); // frame 1: strike
        game.roll(10); // frame 2: strike
        game.roll(10); // frame 3: strike
        game.roll(5);
        game.roll(3);
        rollMany(game, 12, 0);
        assertEquals(81, game.getState().totalScore());
    }

    /**
     * Tests alternating strikes and gutter balls.
     * Each strike gets 10 + 0 + 0 = 10, each gutter frame = 0. Total = 50.
     */
    @Test
    void alternatingStrikesAndGutters() {
        BowlingGame game = new BowlingGame();
        for (int i = 0; i < 5; i++) {
            game.roll(10); // strike
            game.roll(0);  // next frame starts with gutter
            game.roll(0);
        }
        assertEquals(50, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    /**
     * Tests that a partial strike frame (waiting for bonus rolls) shows incomplete state.
     */
    @Test
    void partialGameReportsIncompleteFramesWithoutScore() {
        BowlingGame game = new BowlingGame();
        game.roll(10); // strike, bonus not known yet
        GameState state = game.getState();
        assertFalse(state.frames().get(0).complete());
        assertNull(state.frames().get(0).score());
        assertFalse(state.gameOver());
    }

    // ========== Mixed Game Tests ==========

    /**
     * Tests a realistic game with mixed strikes, spares, and regular frames.
     */
    @Test
    void mixedGameWithStrikesAndSpares() {
        BowlingGame game = new BowlingGame();
        game.roll(10); // frame 1: strike -> 10 + 7 + 3 = 20
        game.roll(7);  // frame 2: spare
        game.roll(3);  //           10 + 10 = 20
        game.roll(10); // frame 3: strike -> 10 + 9 + 0 = 19
        game.roll(9);  // frame 4: 9
        game.roll(0);
        rollMany(game, 12, 0);
        assertEquals(68, game.getState().totalScore());
    }

    // ========== 10th Frame Tests ==========

    /**
     * Tests that the 10th frame ends after 2 rolls when no strike or spare is achieved.
     */
    @Test
    void tenthFrameWithNoSpareOrStrikeEndsAfterTwoRolls() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(3);
        game.roll(5); // no spare/strike, game should be over
        assertTrue(game.isGameOver());
        assertEquals(8, game.getState().totalScore());
    }

    /**
     * Tests 10th frame with strike followed by a spare (X 7 /).
     */
    @Test
    void tenthFrameStrikeThenSpare() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(10); // strike
        game.roll(7);  // second roll
        game.roll(3);  // spare on second+third
        assertEquals(20, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    /**
     * Tests 10th frame with strike followed by non-strike rolls (X 3 4).
     */
    @Test
    void tenthFrameStrikeFollowedByNonStrike() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(10); // strike
        game.roll(3);
        game.roll(4);
        assertEquals(17, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    // ========== Frame Interaction Tests ==========

    /**
     * Tests that a spare in the 9th frame uses the 10th frame's first roll as bonus.
     */
    @Test
    void spareInNinthFrameFollowedByStrikeInTenth() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 16, 0);
        game.roll(5);
        game.roll(5); // spare in 9th
        game.roll(10); // strike in 10th (also bonus for 9th)
        game.roll(10);
        game.roll(10);
        // 9th frame: 10 + 10 = 20
        // 10th frame: 10 + 10 + 10 = 30
        assertEquals(50, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    /**
     * Tests that a strike in the 9th frame uses the 10th frame's rolls as bonus.
     */
    @Test
    void ninthStrikeCalculatesCorrectlyWithTenthFrame() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 16, 0);
        game.roll(10); // 9th frame strike
        game.roll(5);  // 10th frame
        game.roll(3);
        // 9th frame: 10 + 5 + 3 = 18
        // 10th frame: 5 + 3 = 8
        assertEquals(26, game.getState().totalScore());
        assertTrue(game.isGameOver());
    }

    // ========== Validation Tests ==========

    /**
     * Tests that rolling more than 10 pins total in a single frame is rejected.
     */
    @Test
    void rollingMoreThanTenPinsInAFrameIsRejected() {
        BowlingGame game = new BowlingGame();
        game.roll(6);
        assertThrows(IllegalArgumentException.class, () -> game.roll(6));
    }

    /**
     * Tests that pin counts outside the valid range (0-10) are rejected.
     */
    @Test
    void pinCountOutOfRangeIsRejected() {
        BowlingGame game = new BowlingGame();
        assertThrows(IllegalArgumentException.class, () -> game.roll(11));
        assertThrows(IllegalArgumentException.class, () -> game.roll(-1));
    }

    /**
     * Tests that attempting to roll after the game is complete is rejected.
     */
    @Test
    void rollingAfterGameOverIsRejected() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 20, 0);
        assertThrows(IllegalStateException.class, () -> game.roll(0));
    }

    // ========== 10th Frame Validation Edge Cases ==========

    /**
     * Tests that 10th frame roll 3 after strike+non-strike cannot exceed remaining pins.
     * Example: 10th frame: X, 7, ? → max 3 pins on roll 3
     */
    @Test
    void tenthFrameRoll3AfterStrikeAndNonStrikeValidation() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(10); // strike - gets bonus rolls
        game.roll(7);  // second roll (fresh pins)
        // Third roll can only be 0-3 (remaining from second roll)
        assertThrows(IllegalArgumentException.class, () -> game.roll(4));
    }

    /**
     * Tests that 10th frame with no spare/strike ends after exactly 2 rolls.
     */
    @Test
    void tenthFrameNoSpareOrStrikeEndsAfterTwoRollsExactly() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(7);
        game.roll(2); // total 9, no spare
        assertTrue(game.isGameOver());
        assertEquals(9, game.getState().totalScore());
    }

    /**
     * Tests that 10th frame with spare allows exactly 3 rolls with fresh pins on roll 3.
     */
    @Test
    void tenthFrameSpareAllowsFreshPinsOnRoll3() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(7);
        game.roll(3);  // spare
        game.roll(10); // fresh pins - strike is valid
        assertTrue(game.isGameOver());
        assertEquals(20, game.getState().totalScore()); // 7+3+10
    }

    /**
     * Tests 10th frame: strike, strike, strike (perfect finish).
     */
    @Test
    void tenthFrameThreeStrikesInARow() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(10); // strike
        game.roll(10); // strike (fresh pins)
        game.roll(10); // strike (fresh pins)
        assertTrue(game.isGameOver());
        assertEquals(30, game.getState().totalScore());
    }

    /**
     * Tests 10th frame roll 2 validation: after non-strike, cannot exceed remaining.
     */
    @Test
    void tenthFrameRoll2AfterNonStrikeValidation() {
        BowlingGame game = new BowlingGame();
        rollMany(game, 18, 0);
        game.roll(6);
        // Second roll cannot exceed 4 remaining pins
        assertThrows(IllegalArgumentException.class, () -> game.roll(5));
    }

    // ========== Immutability Verification Tests ==========

    /**
     * Tests that GameState.rolls() returns an immutable list.
     */
    @Test
    void gameStateRollsListIsImmutable() {
        BowlingGame game = new BowlingGame();
        game.roll(5);
        GameState state = game.getState();

        assertThrows(UnsupportedOperationException.class,
            () -> state.rolls().add(10));
    }

    /**
     * Tests that GameState.frames() returns an immutable list.
     */
    @Test
    void gameStateFramesListIsImmutable() {
        BowlingGame game = new BowlingGame();
        game.roll(5);
        game.roll(3);
        GameState state = game.getState();

        assertThrows(UnsupportedOperationException.class,
            () -> state.frames().clear());
    }

    /**
     * Tests that FrameResult.rolls() returns an immutable list.
     */
    @Test
    void frameResultRollsListIsImmutable() {
        BowlingGame game = new BowlingGame();
        game.roll(5);
        game.roll(3);
        GameState state = game.getState();
        FrameResult frame = state.frames().get(0);

        assertThrows(UnsupportedOperationException.class,
            () -> frame.rolls().add(10));
    }

    /**
     * Tests that modifying returned GameState doesn't affect internal game state.
     */
    @Test
    void gameStateIsDefensivelyCopied() {
        BowlingGame game = new BowlingGame();
        game.roll(5);
        GameState state1 = game.getState();

        game.roll(3);
        GameState state2 = game.getState();

        // state1 should still show only 1 roll
        assertEquals(1, state1.rolls().size());
        assertEquals(5, state1.rolls().get(0));

        // state2 should show 2 rolls
        assertEquals(2, state2.rolls().size());
    }
}
