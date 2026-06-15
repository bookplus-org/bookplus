import { Routes } from '@angular/router';

export const CATALOG_ROUTES: Routes = [
  {
    path: '',
    title: 'Catálogo · BookPlus',
    loadComponent: () =>
      import('./pages/book-list-page.component').then((m) => m.BookListPageComponent),
  },
  {
    path: ':id',
    title: 'Detalle · BookPlus',
    loadComponent: () =>
      import('./pages/book-detail-page.component').then((m) => m.BookDetailPageComponent),
  },
  {
    path: ':id/preview',
    title: 'Previsualización · BookPlus',
    loadComponent: () =>
      import('./pages/book-preview-page.component').then((m) => m.BookPreviewPageComponent),
  },
];
