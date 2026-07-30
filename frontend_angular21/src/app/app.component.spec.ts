import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { AppComponent } from './app.component';
import { BowlingStoreService } from './bowling/services/bowling-store.service';
import { GameState } from './bowling/models/bowling.models';

describe('AppComponent', () => {
  let mockStore: {
    state: ReturnType<typeof signal<GameState | null>>;
    loading: ReturnType<typeof signal<boolean>>;
    errorMessage: ReturnType<typeof signal<string | null>>;
    load: ReturnType<typeof vi.fn>;
    roll: ReturnType<typeof vi.fn>;
    reset: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockStore = {
      state: signal<GameState | null>(null),
      loading: signal(false),
      errorMessage: signal<string | null>(null),
      load: vi.fn(),
      roll: vi.fn(),
      reset: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: BowlingStoreService, useValue: mockStore }],
    }).compileComponents();
  });

  function createComponent() {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('loads the game on construction', () => {
    createComponent();
    expect(mockStore.load).toHaveBeenCalledTimes(1);
  });

  it('delegates roll() to the store', () => {
    const component = createComponent();
    component.roll(7);
    expect(mockStore.roll).toHaveBeenCalledWith(7);
  });

  describe('resetGame', () => {
    it('resets via the store when the user confirms', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      const component = createComponent();

      component.resetGame();

      expect(mockStore.reset).toHaveBeenCalledTimes(1);
    });

    it('does nothing when the user cancels the confirmation', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(false);
      const component = createComponent();

      component.resetGame();

      expect(mockStore.reset).not.toHaveBeenCalled();
    });
  });
});
