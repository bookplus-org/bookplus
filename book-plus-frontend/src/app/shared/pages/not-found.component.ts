import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'bp-not-found',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center justify-center gap-4 py-24 text-center">
      <p class="text-7xl font-bold text-brand-600">404</p>
      <h1 class="text-2xl font-medium">Página no encontrada</h1>
      <p class="text-gray-500">La ruta que buscas no existe o fue movida.</p>
      <a mat-flat-button color="primary" routerLink="/catalog">Volver al catálogo</a>
    </div>
  `,
})
export class NotFoundComponent {}
