package com.example.bowling.web;

import com.example.bowling.model.GameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link BowlingController}.
 * Tests the full HTTP layer including JSON serialization, error handling, and API contracts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BowlingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Reset game state before each test since GameService is a singleton.
     */
    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/bowling/reset"))
                .andExpect(status().isOk());
    }

    // ========== GET /api/bowling/state Tests ==========

    @Test
    void getState_shouldReturnInitialState() throws Exception {
        mockMvc.perform(get("/api/bowling/state"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.rolls", hasSize(0)))
                .andExpect(jsonPath("$.frames", hasSize(0)))
                .andExpect(jsonPath("$.totalScore", is(0)))
                .andExpect(jsonPath("$.gameOver", is(false)));
    }

    // ========== POST /api/bowling/roll Tests ==========

    @Test
    void roll_shouldRecordSingleRoll() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolls", hasSize(1)))
                .andExpect(jsonPath("$.rolls[0]", is(5)))
                .andExpect(jsonPath("$.frames", hasSize(1)))
                .andExpect(jsonPath("$.totalScore", is(0))) // Frame incomplete
                .andReturn();

        String json = result.getResponse().getContentAsString();
        GameState state = objectMapper.readValue(json, GameState.class);
        assertEquals(1, state.rolls().size());
        assertFalse(state.gameOver());
    }

    @Test
    void roll_shouldCompleteFrame() throws Exception {
        // Roll 1: 5 pins
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 5}"))
                .andExpect(status().isOk());

        // Roll 2: 3 pins (frame complete)
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolls", hasSize(2)))
                .andExpect(jsonPath("$.totalScore", is(8)))
                .andExpect(jsonPath("$.frames", hasSize(1)))
                .andExpect(jsonPath("$.frames[0].complete", is(true)))
                .andExpect(jsonPath("$.frames[0].score", is(8)));
    }

    @Test
    void roll_shouldHandleStrike() throws Exception {
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolls", hasSize(1)))
                .andExpect(jsonPath("$.frames", hasSize(1)))
                .andExpect(jsonPath("$.frames[0].strike", is(true)))
                .andExpect(jsonPath("$.frames[0].complete", is(false))) // Waiting for bonus
                .andExpect(jsonPath("$.frames[0].score").doesNotExist());
    }

    @Test
    void roll_shouldHandleSpare() throws Exception {
        // Roll 1: 6 pins
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 6}"))
                .andExpect(status().isOk());

        // Roll 2: 4 pins (spare)
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frames[0].spare", is(true)))
                .andExpect(jsonPath("$.frames[0].complete", is(false))) // Waiting for bonus
                .andExpect(jsonPath("$.frames[0].score").doesNotExist());
    }

    @Test
    void roll_shouldCompletePerfectGame() throws Exception {
        // Roll 12 strikes (perfect game)
        for (int i = 0; i < 12; i++) {
            mockMvc.perform(post("/api/bowling/roll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pins\": 10}"))
                    .andExpect(status().isOk());
        }

        // Verify perfect game
        mockMvc.perform(get("/api/bowling/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore", is(300)))
                .andExpect(jsonPath("$.gameOver", is(true)))
                .andExpect(jsonPath("$.frames", hasSize(10)));
    }

    // ========== Validation Tests ==========

    @Test
    void roll_shouldRejectNegativePins() throws Exception {
        // Bean Validation (@Min(0)) rejects this at controller level
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roll_shouldRejectTooManyPins() throws Exception {
        // Bean Validation (@Max(10)) rejects this at controller level
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 11}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roll_shouldRejectExceedingFrameLimit() throws Exception {
        // Roll 1: 6 pins
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 6}"))
                .andExpect(status().isOk());

        // Roll 2: 6 pins (exceeds 10)
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds")));
    }

    @Test
    void roll_shouldRejectAfterGameOver() throws Exception {
        // Complete game with gutter balls
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/bowling/roll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pins\": 0}"))
                    .andExpect(status().isOk());
        }

        // Try to roll after game over
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("complete")));
    }

    @Test
    void roll_shouldRejectMalformedJSON() throws Exception {
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roll_shouldRejectMissingPinsField() throws Exception {
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /api/bowling/reset Tests ==========

    @Test
    void reset_shouldStartNewGame() throws Exception {
        // Roll some pins
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 5}"))
                .andExpect(status().isOk());

        // Reset game
        mockMvc.perform(post("/api/bowling/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolls", hasSize(0)))
                .andExpect(jsonPath("$.frames", hasSize(0)))
                .andExpect(jsonPath("$.totalScore", is(0)))
                .andExpect(jsonPath("$.gameOver", is(false)));
    }

    @Test
    void reset_shouldWorkMultipleTimes() throws Exception {
        for (int i = 0; i < 3; i++) {
            // Roll some pins
            mockMvc.perform(post("/api/bowling/roll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pins\": 10}"))
                    .andExpect(status().isOk());

            // Reset
            mockMvc.perform(post("/api/bowling/reset"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rolls", hasSize(0)));
        }
    }

    // ========== 10th Frame Integration Tests ==========

    @Test
    void roll_shouldHandle10thFrameWithSpare() throws Exception {
        // Roll 18 gutter balls (9 frames)
        for (int i = 0; i < 18; i++) {
            mockMvc.perform(post("/api/bowling/roll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pins\": 0}"))
                    .andExpect(status().isOk());
        }

        // 10th frame: spare (7 + 3)
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 7}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameOver", is(false))); // Need bonus roll

        // Bonus roll
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore", is(15))) // 7+3+5
                .andExpect(jsonPath("$.gameOver", is(true)));
    }

    @Test
    void roll_shouldHandle10thFrameWithStrike() throws Exception {
        // Roll 18 gutter balls (9 frames)
        for (int i = 0; i < 18; i++) {
            mockMvc.perform(post("/api/bowling/roll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pins\": 0}"))
                    .andExpect(status().isOk());
        }

        // 10th frame: strike
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameOver", is(false)));

        // Bonus roll 1
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameOver", is(false)));

        // Bonus roll 2
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore", is(20))) // 10+7+3
                .andExpect(jsonPath("$.gameOver", is(true)));
    }

    // ========== Immutability Tests ==========

    @Test
    void getState_shouldReturnIndependentSnapshots() throws Exception {
        // Get state 1
        MvcResult result1 = mockMvc.perform(get("/api/bowling/state"))
                .andExpect(status().isOk())
                .andReturn();
        String json1 = result1.getResponse().getContentAsString();
        GameState state1 = objectMapper.readValue(json1, GameState.class);

        // Roll some pins
        mockMvc.perform(post("/api/bowling/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pins\": 5}"))
                .andExpect(status().isOk());

        // Get state 2
        MvcResult result2 = mockMvc.perform(get("/api/bowling/state"))
                .andExpect(status().isOk())
                .andReturn();
        String json2 = result2.getResponse().getContentAsString();
        GameState state2 = objectMapper.readValue(json2, GameState.class);

        // State 1 should be unchanged
        assertEquals(0, state1.rolls().size());
        assertEquals(1, state2.rolls().size());
    }

    // ========== CORS Tests ==========

    @Test
    void options_shouldHandleCORSPreflight() throws Exception {
        mockMvc.perform(options("/api/bowling/state")
                        .header("Origin", "http://localhost:4201")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }
}
