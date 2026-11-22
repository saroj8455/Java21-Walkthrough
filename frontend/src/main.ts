import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

// Standalone bootstrap (no AppModule) - the Angular 17 default.
bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
