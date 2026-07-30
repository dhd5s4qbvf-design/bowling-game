import { Component, inject } from '@angular/core';
import { BowlingStoreService } from './bowling/services/bowling-store.service';
import { ScoreboardComponent } from './bowling/components/scoreboard/scoreboard.component';
import { PinControlsComponent } from './bowling/components/pin-controls/pin-controls.component';
import { RollHistoryComponent } from './bowling/components/roll-history/roll-history.component';

/**
 * Root shell: wires the store to the presentational bowling components.
 * No scoring/formatting logic lives here.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ScoreboardComponent, PinControlsComponent, RollHistoryComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  private readonly store = inject(BowlingStoreService);

  readonly state = this.store.state;
  readonly loading = this.store.loading;
  readonly errorMessage = this.store.errorMessage;

  constructor() {
    this.store.load();
  }

  roll(pins: number): void {
    this.store.roll(pins);
  }

  resetGame(): void {
    if (!confirm('Start a new game? Current progress will be lost.')) {
      return;
    }
    this.store.reset();
  }
}
