import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NotificationsService, UserNotification } from '../data/notifications.service';
import { AsyncState, failure, loading, success } from '@core/models/async-state.model';
import { Page } from '@core/models/page.model';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-notifications-page',
  standalone: true,
  imports: [DatePipe, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notifications-page.component.html',
})
export class NotificationsPageComponent {
  private readonly service = inject(NotificationsService);
  protected readonly state = signal<AsyncState<Page<UserNotification>>>(loading());

  constructor() {
    this.service.list({ page: 0, size: 30 }).subscribe({
      next: (page) => this.state.set(success(page)),
      error: (problem: ProblemDetail) =>
        this.state.set(failure(problem.detail ?? 'No se pudieron cargar las notificaciones.')),
    });
  }

  icon(type: string): string {
    const t = type.toUpperCase();
    if (t.includes('ORDER')) return 'receipt_long';
    if (t.includes('PAYMENT')) return 'payments';
    if (t.includes('STOCK') || t.includes('INVENTORY')) return 'inventory_2';
    if (t.includes('WELCOME') || t.includes('ACCOUNT')) return 'celebration';
    return 'notifications';
  }
}
