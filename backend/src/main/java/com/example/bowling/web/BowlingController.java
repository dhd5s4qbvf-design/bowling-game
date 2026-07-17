package com.example.bowling.web;

import com.example.bowling.model.GameState;
import com.example.bowling.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bowling")
@Tag(name = "Bowling Game", description = "Bowling game management and gameplay operations")
public class BowlingController {

    private final GameService gameService;

    public BowlingController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(
            summary = "Get current game state",
            description = "Returns the current state of the bowling game including rolls, frames, and score"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current game state",
                    content = @Content(schema = @Schema(implementation = GameState.class))
            )
    })
    @GetMapping("/state")
    public GameState getState() {
        return gameService.getState();
    }

    @Operation(
            summary = "Record a roll",
            description = "Records a roll with the specified number of pins knocked down (0-10)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Roll recorded successfully",
                    content = @Content(schema = @Schema(implementation = GameState.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pins value or game is over",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/roll")
    public GameState roll(@Valid @RequestBody RollRequest request) {
        return gameService.roll(request.getPins());
    }

    @Operation(
            summary = "Reset game",
            description = "Resets the game back to the initial state (frame 1)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Game reset successfully",
                    content = @Content(schema = @Schema(implementation = GameState.class))
            )
    })
    @PostMapping("/reset")
    public GameState reset() {
        return gameService.reset();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleGameError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {
    }
}
