
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AppComponent } from './app.component';
import { BowlingService } from './bowling.service';
import { FrameResult } from './models';
import { of } from 'rxjs';

describe('AppComponent', () => {
  let component: AppComponent;
  let mockBowlingService: {
    getState: ReturnType<typeof vi.fn>;
    createGame: ReturnType<typeof vi.fn>;
    roll: ReturnType<typeof vi.fn>;
    reset: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockBowlingService = {
      getState: vi.fn(),
      createGame: vi.fn(),
      roll: vi.fn(),
      reset: vi.fn(),
    };

    // Default mock response
    mockBowlingService.getState.mockReturnValue(
      of({
        rolls: [],
        frames: [],
        totalScore: 0,
        gameOver: false,
      })
    );

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: BowlingService, useValue: mockBowlingService }],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  describe('calculateMaxPins', () => {
    // ========== No Current Frame ==========

    it('should return 10 when no current frame exists', () => {
      expect(component.calculateMaxPins(undefined)).toBe(10);
    });

    // ========== Frames 1-9 Tests ==========

    it('should return 10 for first roll of a regular frame', () => {
      const frame: FrameResult = {
        frameNumber: 5,
        rolls: [],
        strike: false,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10);
    });

    it('should return remaining pins for second roll of regular frame', () => {
      const frame: FrameResult = {
        frameNumber: 3,
        rolls: [7],
        strike: false,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(3); // 10 - 7
    });

    it('should return 10 after a strike in frames 1-9', () => {
      const frame: FrameResult = {
        frameNumber: 4,
        rolls: [10],
        strike: true,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10);
    });

    it('should return 10 when frame has 2 rolls (frame complete)', () => {
      const frame: FrameResult = {
        frameNumber: 6,
        rolls: [5, 3],
        strike: false,
        spare: false,
        complete: true,
        score: 8,
        runningTotal: 45,
      };
      expect(component.calculateMaxPins(frame)).toBe(10);
    });

    // ========== 10th Frame Tests ==========

    it('should return remaining pins for 10th frame roll 2 after non-strike', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [6],
        strike: false,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(4); // 10 - 6
    });

    it('should return 10 for 10th frame roll 2 after strike (fresh pins)', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [10],
        strike: true,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10);
    });

    it('should return 10 for 10th frame roll 3 after spare (fresh pins)', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [7, 3],
        strike: false,
        spare: true,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10);
    });

    it('should return 10 for 10th frame roll 3 after strike+strike (fresh pins)', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [10, 10],
        strike: true,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10);
    });

    it('should return remaining pins for 10th frame roll 3 after strike+non-strike', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [10, 7],
        strike: true,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(3); // 10 - 7
    });

    // ========== Edge Cases ==========

    it('should handle 10th frame with 0 on first roll', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [0],
        strike: false,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10); // 10 - 0
    });

    it('should handle regular frame with 0 on first roll', () => {
      const frame: FrameResult = {
        frameNumber: 5,
        rolls: [0],
        strike: false,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      expect(component.calculateMaxPins(frame)).toBe(10); // 10 - 0
    });

    it('should handle 10th frame roll 3 after non-strike combo (e.g., 6+3)', () => {
      const frame: FrameResult = {
        frameNumber: 10,
        rolls: [6, 3], // total 9, not a spare
        strike: false,
        spare: false,
        complete: false,
        score: null,
        runningTotal: null,
      };
      // No spare, no strike → should return 10 (treating as fresh pins)
      expect(component.calculateMaxPins(frame)).toBe(10);
    });
  });

  describe('isPinDisabled', () => {
    it('should disable all pins when game is over', () => {
      component.state.set({
        rolls: [10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10],
        frames: [],
        totalScore: 300,
        gameOver: true,
      });

      expect(component.isPinDisabled(0)).toBe(true);
      expect(component.isPinDisabled(5)).toBe(true);
      expect(component.isPinDisabled(10)).toBe(true);
    });

    it('should disable pins exceeding maxPinsForNextRoll', () => {
      component.state.set({
        rolls: [7],
        frames: [
          {
            frameNumber: 1,
            rolls: [7],
            strike: false,
            spare: false,
            complete: false,
            score: null,
            runningTotal: null,
          },
        ],
        totalScore: 0,
        gameOver: false,
      });

      expect(component.isPinDisabled(3)).toBe(false); // 3 <= max (3)
      expect(component.isPinDisabled(4)).toBe(true); // 4 > max (3)
      expect(component.isPinDisabled(10)).toBe(true); // 10 > max (3)
    });
  });
});
