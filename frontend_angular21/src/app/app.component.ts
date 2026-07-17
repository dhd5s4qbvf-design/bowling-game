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

  readonly maxPinsForNextRoll = computed(() => {
    const current = this.currentFrame();
    if (!current) return 10;

    if (current.frameNumber < 10) {
      // If first roll was a strike, frame is done - all pins available
      if (current.rolls[0] === 10) return 10;
      // If we already have 2 rolls in frames 1-9, frame is complete - all pins available
      if (current.rolls.length >= 2) return 10;
      // Second roll of frame - can't exceed remaining pins
      return 10 - current.rolls[0];
    }

    // 10th frame
    if (current.rolls.length === 1) {
      return current.rolls[0] === 10 ? 10 : 10 - current.rolls[0];
    }
    if (current.rolls.length === 2) {
      const firstIsStrike = current.rolls[0] === 10;
      if (firstIsStrike) {
        return current.rolls[1] === 10 ? 10 : 10 - current.rolls[1];
      }
      return 10; // spare -> fresh set of pins for the bonus roll
    }
    return 10;
  });

  constructor() {
    this.initializeGame();
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
