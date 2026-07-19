import { Component, computed, inject, signal } from '@angular/core';
import { BowlingService } from './bowling.service';
import { FrameResult, GameState } from './models';

/**
 * Main application component using Angular 18 features:
 * - Signals for reactive state management
 * - inject() function for dependency injection
 * - Computed signals for derived state
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  // Modern DI with inject() function (Angular 14+)
  private readonly bowlingService = inject(BowlingService);

  // Signals for reactive state (Angular 16+)
  readonly state = signal<GameState | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(false);

  // Computed signals for derived state
  readonly pinOptions = computed(() => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);

  readonly currentFrame = computed(() => {
    const currentState = this.state();
    if (!currentState || currentState.frames.length === 0) {
      return undefined;
    }
    const last = currentState.frames[currentState.frames.length - 1];
    return last.complete ? undefined : last;
  });

  readonly maxPinsForNextRoll = computed(() =>
    this.calculateMaxPins(this.currentFrame())
  );

  constructor() {
    this.initializeGame();
  }

  /**
   * Calculates the maximum number of pins available for the next roll.
   * Extracted for testability and reusability.
   *
   * Rules:
   * - No current frame: 10 pins (fresh frame)
   * - Frames 1-9: First roll = 10 remaining, second roll = 10 - first roll
   * - Frame 10, roll 1: 10 remaining if strike, else 10 - first roll
   * - Frame 10, roll 2 after strike: 10 if previous was strike, else 10 - previous
   * - Frame 10, roll 3 after spare: 10 (fresh pins)
   *
   * @param currentFrame The incomplete frame, or undefined if no frame is in progress
   * @returns Maximum pins available (0-10)
   */
  calculateMaxPins(currentFrame: FrameResult | undefined): number {
    if (!currentFrame) {
      return 10; // Starting fresh frame
    }

    // Frames 1-9
    if (currentFrame.frameNumber < 10) {
      return this.calculateMaxPinsRegularFrame(currentFrame);
    }

    // Frame 10 (special rules)
    return this.calculateMaxPinsTenthFrame(currentFrame);
  }

  /**
   * Calculates max pins for frames 1-9.
   * - Strike (1 roll): Next roll is a new frame → 10 pins
   * - Two rolls already: Frame complete → 10 pins
   * - Second roll: 10 - first roll
   */
  private calculateMaxPinsRegularFrame(frame: FrameResult): number {
    // No rolls yet, strike, or frame complete - next roll is for a fresh frame
    if (frame.rolls.length === 0 || frame.rolls[0] === 10 || frame.rolls.length >= 2) {
      return 10;
    }
    // Second roll - can't exceed remaining pins
    return 10 - frame.rolls[0];
  }

  /**
   * Calculates max pins for frame 10 (has bonus rolls).
   * - Roll 1: 10 remaining
   * - Roll 2: If roll 1 was strike → fresh pins (10), else → 10 - roll 1
   * - Roll 3 after strike: If roll 2 was strike → 10, else → 10 - roll 2
   * - Roll 3 after spare: Fresh pins → 10
   */
  private calculateMaxPinsTenthFrame(frame: FrameResult): number {
    const rollCount = frame.rolls.length;

    // Roll 2
    if (rollCount === 1) {
      return frame.rolls[0] === 10 ? 10 : 10 - frame.rolls[0];
    }

    // Roll 3
    if (rollCount === 2) {
      const firstIsStrike = frame.rolls[0] === 10;

      if (firstIsStrike) {
        // After strike on roll 1, roll 2 gets fresh pins
        // Roll 3 depends on roll 2
        return frame.rolls[1] === 10 ? 10 : 10 - frame.rolls[1];
      }

      // No strike on roll 1 - either spare or regular
      // After spare (roll 1 + roll 2 = 10), roll 3 gets fresh pins
      return 10;
    }

    return 10;
  }

  /**
   * Initialize the game by creating a session if needed.
   * For the production backend with sessions, we need to:
   * 1. Try to get existing state (might have session cookie)
   * 2. If no session exists (404), create a new game
   */
  private initializeGame(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.bowlingService.getState().subscribe({
      next: (state) => {
        // Session exists, load the state
        this.state.set(state);
        this.loading.set(false);
      },
      error: (err: Error) => {
        // No session exists, create a new game
        console.log('No existing session, creating new game...');
        this.createNewGame();
      },
    });
  }

  private createNewGame(): void {
    this.bowlingService.createGame().subscribe({
      next: (state) => {
        console.log('Game loaded (stateless backend)');
        this.state.set(state);
        this.loading.set(false);
      },
      error: (err: Error) => {
        this.errorMessage.set('Failed to load game: ' + err.message);
        this.loading.set(false);
      },
    });
  }

  private loadGameState(): void {
    this.bowlingService.getState().subscribe({
      next: (state) => {
        this.state.set(state);
        this.loading.set(false);
      },
      error: (err: Error) => {
        this.errorMessage.set(err.message);
        this.loading.set(false);
      },
    });
  }

  roll(pins: number): void {
    this.errorMessage.set(null);
    this.loading.set(true);

    this.bowlingService.roll(pins).subscribe({
      next: (state) => {
        this.state.set(state);
        this.loading.set(false);
      },
      error: (err: Error) => {
        this.errorMessage.set(err.message);
        this.loading.set(false);
        // Reload state to ensure UI is in sync
        this.loadGameState();
      },
    });
  }

  resetGame(): void {
    if (!confirm('Start a new game? Current progress will be lost.')) {
      return;
    }

    this.errorMessage.set(null);
    this.loading.set(true);

    // Call the reset endpoint to start a fresh game
    this.bowlingService.reset().subscribe({
      next: (state) => {
        this.state.set(state);
        this.loading.set(false);
      },
      error: (err: Error) => {
        this.errorMessage.set('Failed to reset game: ' + err.message);
        this.loading.set(false);
      },
    });
  }

  isPinDisabled(pins: number): boolean {
    const currentState = this.state();
    if (!currentState || currentState.gameOver || this.loading()) {
      return true;
    }
    return pins > this.maxPinsForNextRoll();
  }

  frameDisplay(frame: FrameResult, rollIndex: number): string {
    const roll = frame.rolls[rollIndex];
    if (roll === undefined) {
      return '';
    }
    if (roll === 10) {
      return 'X';
    }
    // spare marker: second roll of a normal frame completing to 10
    if (rollIndex > 0) {
      const previous = frame.rolls[rollIndex - 1];
      if (previous + roll === 10 && previous !== 10) {
        return '/';
      }
    }
    return roll === 0 ? '-' : String(roll);
  }

  trackByFrameNumber(_index: number, frame: FrameResult): number {
    return frame.frameNumber;
  }

  private isFrameDoneForInput(frame: FrameResult): boolean {
    // Frame is done for input if: complete, has strike (1 roll), or has 2 rolls (spare or normal) in frames 1-9
    return frame.complete ||
           frame.rolls[0] === 10 ||
           (frame.frameNumber < 10 && frame.rolls.length >= 2);
  }

  isCurrentFrame(frame: FrameResult): boolean {
    const currentState = this.state();
    if (!currentState || currentState.gameOver) {
      return false;
    }

    const lastFrame = currentState.frames[currentState.frames.length - 1];
    if (!lastFrame) {
      return frame.frameNumber === 1;
    }

    // If last frame is done for input, highlight the next frame
    if (this.isFrameDoneForInput(lastFrame)) {
      return frame.frameNumber === lastFrame.frameNumber + 1;
    }
    // Otherwise highlight the incomplete frame
    return frame.frameNumber === lastFrame.frameNumber;
  }

  isPlaceholderActive(index: number): boolean {
    const currentState = this.state();
    if (!currentState || currentState.frames.length === 0) {
      return index === 0; // First placeholder when no frames exist
    }
    const lastFrame = currentState.frames[currentState.frames.length - 1];
    return this.isFrameDoneForInput(lastFrame) && index === 0;
  }
}
