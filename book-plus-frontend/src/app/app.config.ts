import {
  ApplicationConfig,
  provideZoneChangeDetection,
  provideAppInitializer,
  inject,
} from '@angular/core';
import {
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
  withViewTransitions,
} from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from '@angular/material/snack-bar';

import { APP_ROUTES } from './app.routes';
import { authInterceptor } from '@core/auth/auth.interceptor';
import { errorInterceptor } from '@core/http/error.interceptor';
import { unwrapInterceptor } from '@core/http/unwrap.interceptor';
import { loadingInterceptor } from '@core/http/loading.interceptor';
import { retryInterceptor } from '@core/http/retry.interceptor';
import { AuthStore } from '@core/auth/auth.store';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      APP_ROUTES,
      withComponentInputBinding(),
      withViewTransitions(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
    ),
    // Order matters: loading drives the global bar; auth adds the bearer token;
    // retry re-attempts transient GETs; error maps failures (incl. 401 refresh);
    // unwrap strips the ApiResponse envelope so services see the inner payload.
    provideHttpClient(
      withFetch(),
      withInterceptors([
        loadingInterceptor,
        authInterceptor,
        retryInterceptor,
        errorInterceptor,
        unwrapInterceptor,
      ]),
    ),
    provideAnimationsAsync(),
    {
      provide: MAT_SNACK_BAR_DEFAULT_OPTIONS,
      useValue: { duration: 4000, horizontalPosition: 'end', verticalPosition: 'top' },
    },
    // Restore the session (if any) from storage before the first render.
    provideAppInitializer(() => inject(AuthStore).restoreSession()),
  ],
};
