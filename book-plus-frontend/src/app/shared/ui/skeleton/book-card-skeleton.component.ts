import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Placeholder card shown while the catalog grid is loading. */
@Component({
  selector: 'bp-book-card-skeleton',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overflow-hidden rounded-lg border">
      <div class="aspect-[3/4] animate-pulse bg-gray-200"></div>
      <div class="flex flex-col gap-2 p-4">
        <div class="h-4 w-3/4 animate-pulse rounded bg-gray-200"></div>
        <div class="h-3 w-1/2 animate-pulse rounded bg-gray-200"></div>
        <div class="mt-2 h-6 w-1/3 animate-pulse rounded bg-gray-200"></div>
        <div class="mt-2 h-9 w-full animate-pulse rounded bg-gray-200"></div>
      </div>
    </div>
  `,
})
export class BookCardSkeletonComponent {}
