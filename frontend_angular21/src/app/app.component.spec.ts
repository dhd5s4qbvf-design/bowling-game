import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { BowlingService } from './bowling.service';

describe('AppComponent (Angular 21 with Vitest)', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let service: BowlingService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(BowlingService);
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with loading state', async () => {
    expect(component.loading()).toBe(true);
    await fixture.whenStable(); // Zoneless approach
  });

  it('should disable buttons when loading', async () => {
    component.loading.set(true);
    await fixture.whenStable();

    const buttons = fixture.nativeElement.querySelectorAll('.pin-btn');
    expect(buttons[0].disabled).toBeTruthy();
  });

  it('should calculate maxPinsForNextRoll correctly', () => {
    component.state.set({
      frames: [{
        frameNumber: 1,
        rolls: [7],
        runningTotal: 0,
        complete: false
      }],
      rolls: [7],
      totalScore: 0,
      gameOver: false
    });

    expect(component.maxPinsForNextRoll()).toBe(3);
  });

  it('should show all pins available after strike', () => {
    component.state.set({
      frames: [{
        frameNumber: 1,
        rolls: [10],
        runningTotal: 10,
        complete: false
      }],
      rolls: [10],
      totalScore: 10,
      gameOver: false
    });

    expect(component.maxPinsForNextRoll()).toBe(10);
  });
});
