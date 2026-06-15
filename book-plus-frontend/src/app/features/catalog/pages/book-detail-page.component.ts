import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CatalogService } from '../data/catalog.service';
import { Book, Review } from '../models/book.model';
import { AsyncState, failure, loading, success } from '@core/models/async-state.model';
import { CartStore } from '@features/cart/data/cart.store';
import { AuthStore } from '@core/auth/auth.store';
import { NotificationService } from '@core/notifications/notification.service';
import { ProblemDetail } from '@core/models/problem-detail.model';

@Component({
  selector: 'bp-book-detail-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './book-detail-page.component.html',
})
export class BookDetailPageComponent {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartStore);
  private readonly fb = inject(FormBuilder);
  private readonly notifier = inject(NotificationService);

  protected readonly auth = inject(AuthStore);

  /** Bound from the `:id` route param via withComponentInputBinding(). */
  readonly id = input.required<string>();
  protected readonly state = signal<AsyncState<Book>>(loading());
  protected readonly reviews = signal<Review[]>([]);
  protected readonly submittingReview = signal(false);

  protected readonly reviewForm = this.fb.nonNullable.group({
    rating: [5, [Validators.required]],
    comment: ['', [Validators.required, Validators.minLength(5)]],
  });

  constructor() {
    // Reload whenever the routed `id` input changes (e.g. related-book links).
    effect(() => {
      const id = this.id();
      this.load(id);
      this.loadReviews(id);
    });
  }

  addToCart(book: Book): void {
    this.cart.add(book, 1);
  }

  submitReview(): void {
    if (this.reviewForm.invalid || this.submittingReview()) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    this.submittingReview.set(true);
    this.catalog
      .addReview(this.id(), { ...this.reviewForm.getRawValue(), verifiedPurchase: false })
      .subscribe({
        next: () => {
          this.notifier.success('¡Gracias por tu reseña!');
          this.reviewForm.reset({ rating: 5, comment: '' });
          this.submittingReview.set(false);
          this.loadReviews(this.id());
        },
        error: (problem: ProblemDetail) => {
          this.submittingReview.set(false);
          this.notifier.error(problem.detail ?? 'No se pudo publicar la reseña.');
        },
      });
  }

  private load(id: string): void {
    this.state.set(loading());
    this.catalog.getBook(id).subscribe({
      next: (book) => this.state.set(success(book)),
      error: (problem: ProblemDetail) =>
        this.state.set(failure(problem.detail ?? 'No se encontró el libro.')),
    });
  }

  private loadReviews(id: string): void {
    this.catalog.getReviews(id).subscribe({
      next: (page) => this.reviews.set(page.content),
      error: () => this.reviews.set([]),
    });
  }
}
