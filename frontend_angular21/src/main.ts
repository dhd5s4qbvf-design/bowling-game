// Angular 21: No Zone.js import needed! Zoneless by default.
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { AppComponent } from './app/app.component';

// No session interceptor needed for stateless backend
bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(
      withFetch() // Angular 21:Fetch API
    )
  ],
}).catch((err) => console.error(err));
