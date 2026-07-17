import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { GameState } from './models';

/**
 * Service for interacting with the bowling game backend API.
 * Handles all HTTP communication and error transformation.
 *
 * NO SESSION MANAGEMENT:
 * This version works with stateless backends (backend-node).
 * Game state is maintained in memory on the backend.
 */
@Injectable({ providedIn: 'root' })
export class BowlingService {
  private readonly baseUrl = 'http://localhost:8080/api/bowling';

  constructor(private readonly http: HttpClient) {}

  /**
   * Dummy method for compatibility - does nothing for stateless backend.
   * Just immediately loads the game state instead.
   */
  createGame(): Observable<GameState> {
    return this.getState();
  }

  getState(): Observable<GameState> {
    return this.http.get<GameState>(`${this.baseUrl}/state`)
      .pipe(catchError(this.handleError));
  }

  roll(pins: number): Observable<GameState> {
    return this.http.post<GameState>(`${this.baseUrl}/roll`, { pins })
      .pipe(catchError(this.handleError));
  }

  reset(): Observable<GameState> {
    return this.http.post<GameState>(`${this.baseUrl}/reset`, {})
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unexpected error occurred';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.status === 0) {
      errorMessage = 'Unable to connect to server. Is it running on http://localhost:8080?';
    } else if (error.status >= 500) {
      errorMessage = 'Server error. Please try again later.';
    }

    return throwError(() => new Error(errorMessage));
  }
}
