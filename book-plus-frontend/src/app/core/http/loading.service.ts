import { Injectable, computed, signal } from '@angular/core';

/**
 * Tracks the number of in-flight HTTP requests so the UI can show a global
 * progress indicator. Incremented/decremented by the loading interceptor.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly active = signal(0);
  readonly isLoading = computed(() => this.active() > 0);

  start(): void {
    this.active.update((n) => n + 1);
  }

  stop(): void {
    this.active.update((n) => Math.max(0, n - 1));
  }
}
