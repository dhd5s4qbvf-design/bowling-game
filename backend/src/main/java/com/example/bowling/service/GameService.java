package com.example.bowling.service;

import com.example.bowling.model.GameState;
import org.springframework.stereotype.Service;

/**
 * Holds the currently running game. A single player is enough per the
 * requirements, so one in-memory instance for the whole application is
 * sufficient - no session/user handling needed.
 */
@Service
public class GameService {

    private BowlingGame game = new BowlingGame();

    public synchronized GameState roll(int pins) {
        game.roll(pins);
        return game.getState();
    }

    public synchronized GameState reset() {
        game = new BowlingGame();
        return game.getState();
    }

    public synchronized GameState getState() {
        return game.getState();
    }
}
