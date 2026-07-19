import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BowlingService } from './bowling.service';
import { GameState } from './models';

describe('BowlingService', () => {
  let service: BowlingService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/bowling';

  const mockGameState: GameState = {
    rolls: [5, 3],
    frames: [
      {
        frameNumber: 1,
        rolls: [5, 3],
        strike: false,
        spare: false,
        complete: true,
        score: 8,
        runningTotal: 8,
      },
    ],
    totalScore: 8,
    gameOver: false,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BowlingService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(BowlingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // Ensure no outstanding HTTP requests
  });

  // ========== HTTP Method Tests ==========

  describe('getState', () => {
    it('should make GET request to /state endpoint', () => {
      service.getState().subscribe();

      const req = httpMock.expectOne(`${baseUrl}/state`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGameState);
    });

    it('should return game state on success', () => {
      service.getState().subscribe({
        next: (state) => {
          expect(state).toEqual(mockGameState);
          expect(state.totalScore).toBe(8);
          expect(state.frames.length).toBe(1);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/state`);
      req.flush(mockGameState);
    });
  });

  describe('createGame', () => {
    it('should call getState internally (stateless backend)', () => {
      service.createGame().subscribe({
        next: (state) => {
          expect(state).toEqual(mockGameState);
        },
      });

      // createGame() just delegates to getState()
      const req = httpMock.expectOne(`${baseUrl}/state`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGameState);
    });
  });

  describe('roll', () => {
    it('should make POST request to /roll endpoint with pins', () => {
      service.roll(7).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ pins: 7 });
      req.flush(mockGameState);
    });

    it('should return updated game state after roll', () => {
      const updatedState: GameState = {
        ...mockGameState,
        rolls: [5, 3, 10],
        totalScore: 18,
      };

      service.roll(10).subscribe({
        next: (state) => {
          expect(state.totalScore).toBe(18);
          expect(state.rolls).toEqual([5, 3, 10]);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      req.flush(updatedState);
    });

    it('should send correct pins for gutter ball', () => {
      service.roll(0).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      expect(req.request.body).toEqual({ pins: 0 });
      req.flush(mockGameState);
    });

    it('should send correct pins for strike', () => {
      service.roll(10).subscribe();

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      expect(req.request.body).toEqual({ pins: 10 });
      req.flush(mockGameState);
    });
  });

  describe('reset', () => {
    it('should make POST request to /reset endpoint', () => {
      service.reset().subscribe();

      const req = httpMock.expectOne(`${baseUrl}/reset`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockGameState);
    });

    it('should return fresh game state after reset', () => {
      const freshState: GameState = {
        rolls: [],
        frames: [],
        totalScore: 0,
        gameOver: false,
      };

      service.reset().subscribe({
        next: (state) => {
          expect(state.rolls).toEqual([]);
          expect(state.totalScore).toBe(0);
          expect(state.gameOver).toBe(false);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/reset`);
      req.flush(freshState);
    });
  });

  // ========== Error Handling Tests ==========

  describe('error handling', () => {
    it('should handle network error (status 0)', () => {
      service.getState().subscribe({
        error: (error: Error) => {
          expect(error.message).toBe(
            'Unable to connect to server. Is it running on http://localhost:8080?'
          );
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/state`);
      req.error(new ProgressEvent('Network error'), { status: 0 });
    });

    it('should handle 500 server error', () => {
      service.roll(5).subscribe({
        error: (error: Error) => {
          expect(error.message).toBe('Server error. Please try again later.');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle 503 server error', () => {
      service.reset().subscribe({
        error: (error: Error) => {
          expect(error.message).toBe('Server error. Please try again later.');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/reset`);
      req.flush(null, { status: 503, statusText: 'Service Unavailable' });
    });

    it('should extract custom error message from backend', () => {
      service.roll(11).subscribe({
        error: (error: Error) => {
          expect(error.message).toBe('Invalid pin count: 11. Must be between 0 and 10.');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      req.flush(
        { message: 'Invalid pin count: 11. Must be between 0 and 10.' },
        { status: 400, statusText: 'Bad Request' }
      );
    });

    it('should handle 400 error with custom message', () => {
      service.roll(5).subscribe({
        error: (error: Error) => {
          expect(error.message).toBe('Cannot roll: game is already complete.');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      req.flush(
        { message: 'Cannot roll: game is already complete.' },
        { status: 400, statusText: 'Bad Request' }
      );
    });

    it('should handle 404 error without custom message', () => {
      service.getState().subscribe({
        error: (error: Error) => {
          expect(error.message).toBe('An unexpected error occurred');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/state`);
      req.flush(null, { status: 404, statusText: 'Not Found' });
    });

    it('should handle error without error.error.message', () => {
      service.roll(5).subscribe({
        error: (error: Error) => {
          expect(error.message).toBe('An unexpected error occurred');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      req.flush('Plain text error', { status: 400, statusText: 'Bad Request' });
    });
  });

  // ========== Integration Scenarios ==========

  describe('integration scenarios', () => {
    it('should handle complete game flow', () => {
      let callCount = 0;

      // 1. Get initial state
      service.getState().subscribe(() => {
        callCount++;
      });
      httpMock.expectOne(`${baseUrl}/state`).flush({
        rolls: [],
        frames: [],
        totalScore: 0,
        gameOver: false,
      });

      // 2. Roll first ball
      service.roll(7).subscribe(() => {
        callCount++;
      });
      httpMock.expectOne(`${baseUrl}/roll`).flush({
        rolls: [7],
        frames: [{ frameNumber: 1, rolls: [7], strike: false, spare: false, complete: false, score: null, runningTotal: null }],
        totalScore: 0,
        gameOver: false,
      });

      // 3. Roll second ball
      service.roll(3).subscribe(() => {
        callCount++;
      });
      httpMock.expectOne(`${baseUrl}/roll`).flush({
        rolls: [7, 3],
        frames: [{ frameNumber: 1, rolls: [7, 3], strike: false, spare: true, complete: false, score: null, runningTotal: null }],
        totalScore: 0,
        gameOver: false,
      });

      // 4. Reset game
      service.reset().subscribe(() => {
        callCount++;
        expect(callCount).toBe(4);
      });
      httpMock.expectOne(`${baseUrl}/reset`).flush({
        rolls: [],
        frames: [],
        totalScore: 0,
        gameOver: false,
      });
    });

    it('should handle multiple concurrent requests', () => {
      let completedRequests = 0;

      // Make 3 concurrent requests
      service.getState().subscribe(() => completedRequests++);
      service.roll(5).subscribe(() => completedRequests++);
      service.reset().subscribe(() => completedRequests++);

      // All requests should be pending
      const requests = httpMock.match(() => true);
      expect(requests.length).toBe(3);

      // Fulfill all requests
      requests[0].flush(mockGameState);
      requests[1].flush(mockGameState);
      requests[2].flush(mockGameState);

      expect(completedRequests).toBe(3);
    });
  });

  // ========== Edge Cases ==========

  describe('edge cases', () => {
    it('should handle empty response body', () => {
      service.getState().subscribe({
        next: (state) => {
          expect(state).toEqual({} as GameState);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/state`);
      req.flush({});
    });

    it('should handle response with extra fields', () => {
      const stateWithExtra = {
        ...mockGameState,
        extraField: 'should be ignored',
      };

      service.roll(5).subscribe({
        next: (state) => {
          expect(state).toEqual(stateWithExtra);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/roll`);
      req.flush(stateWithExtra);
    });
  });
});
