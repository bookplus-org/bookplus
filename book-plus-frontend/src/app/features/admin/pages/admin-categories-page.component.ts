import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { CatalogService } from '@features/catalog/data/catalog.service';
import { AdminCatalogService } from '../data/admin-catalog.service';
import { Category } from '@features/catalog/models/book.model';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-admin-categories-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-categories-page.component.html',
})
export class AdminCategoriesPageComponent {
  private readonly catalog = inject(CatalogService);
  private readonly adminCatalog = inject(AdminCatalogService);
  private readonly notifier = inject(NotificationService);

  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(false);
  protected readonly name = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(2)],
  });

  constructor() {
    this.load();
  }

  add(): void {
    if (this.name.invalid) {
      this.name.markAsTouched();
      return;
    }
    this.adminCatalog.createCategory({ name: this.name.value }).subscribe({
      next: () => {
        this.notifier.success('Categoría creada.');
        this.name.reset();
        this.load();
      },
      error: (problem: ProblemDetail) =>
        this.notifier.error(problem.detail ?? 'No se pudo crear la categoría.'),
    });
  }

  remove(category: Category): void {
    this.adminCatalog.deleteCategory(category.id).subscribe({
      next: () => {
        this.notifier.success('Categoría eliminada.');
        this.load();
      },
      error: (problem: ProblemDetail) =>
        this.notifier.error(problem.detail ?? 'No se pudo eliminar.'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.catalog.getCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
