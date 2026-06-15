import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    title: 'Admin · Dashboard',
    loadComponent: () =>
      import('./pages/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
  },
  {
    path: 'books',
    title: 'Admin · Catálogo',
    loadComponent: () =>
      import('./pages/admin-books-page.component').then((m) => m.AdminBooksPageComponent),
  },
  {
    path: 'books/new',
    title: 'Admin · Nuevo libro',
    loadComponent: () =>
      import('./pages/admin-book-form.component').then((m) => m.AdminBookFormComponent),
  },
  {
    path: 'books/:id/edit',
    title: 'Admin · Editar libro',
    loadComponent: () =>
      import('./pages/admin-book-form.component').then((m) => m.AdminBookFormComponent),
  },
  {
    path: 'categories',
    title: 'Admin · Categorías',
    loadComponent: () =>
      import('./pages/admin-categories-page.component').then((m) => m.AdminCategoriesPageComponent),
  },
  {
    path: 'inventory',
    title: 'Admin · Inventario',
    loadComponent: () =>
      import('./pages/admin-inventory-page.component').then((m) => m.AdminInventoryPageComponent),
  },
  {
    path: 'orders',
    title: 'Admin · Pedidos',
    loadComponent: () =>
      import('./pages/admin-orders-page.component').then((m) => m.AdminOrdersPageComponent),
  },
  {
    path: 'shipments',
    title: 'Admin · Envíos',
    loadComponent: () =>
      import('./pages/admin-shipments-page.component').then((m) => m.AdminShipmentsPageComponent),
  },
  {
    path: 'users',
    title: 'Admin · Usuarios',
    loadComponent: () =>
      import('./pages/admin-users-page.component').then((m) => m.AdminUsersPageComponent),
  },
  {
    path: 'coupons',
    title: 'Admin · Cupones',
    loadComponent: () =>
      import('./pages/admin-coupons-page.component').then((m) => m.AdminCouponsPageComponent),
  },
  {
    path: 'notifications',
    title: 'Admin · Notificaciones',
    loadComponent: () =>
      import('./pages/admin-notifications-page.component').then((m) => m.AdminNotificationsPageComponent),
  },
];
