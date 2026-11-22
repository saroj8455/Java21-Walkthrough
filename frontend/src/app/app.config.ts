import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    // Default XHR backend on purpose: withFetch() cannot report UPLOAD progress,
    // which the statement upload progress bar needs.
    provideHttpClient(),
    provideAnimationsAsync(), // required by Angular Material
  ],
};
