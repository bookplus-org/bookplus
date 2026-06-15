import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { fromEvent, interval, merge } from 'rxjs';
import { filter } from 'rxjs/operators';

/**
 * Polling inteligente: ejecuta `onTick` cada `ms`, pero solo cuando la pestaña
 * está visible. Además refresca de inmediato al recuperar el foco de la ventana
 * o al volver a la pestaña. Se limpia solo cuando el componente se destruye.
 *
 * Uso:
 *   startAutoRefresh(12000, this.destroyRef, () => this.reload());
 */
export function startAutoRefresh(ms: number, destroyRef: DestroyRef, onTick: () => void): void {
  merge(
    interval(ms),
    fromEvent(window, 'focus'),
    fromEvent(document, 'visibilitychange'),
  )
    .pipe(
      filter(() => document.visibilityState === 'visible'),
      takeUntilDestroyed(destroyRef),
    )
    .subscribe(() => onTick());
}
