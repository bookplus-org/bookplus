import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { roleGuard } from '@core/auth/role.guard';
import { guestGuard } from '@core/auth/guest.guard';

/**
 * Top-level routing.
 * - Storefront shell hosts public + authenticated customer routes.
 * - Auth routes use a bare layout and are only reachable while logged out.
 * - Admin area is lazy-loaded and gated by ADMIN role.
 */
export const APP_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/store-layout/store-layout.component').then((m) => m.StoreLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'catalog' },
      {
        path: 'catalog',
        loadChildren: () => import('@features/catalog/catalog.routes').then((m) => m.CATALOG_ROUTES),
      },
      {
        path: 'cart',
        loadComponent: () =>
          import('@features/cart/pages/cart-page.component').then((m) => m.CartPageComponent),
      },
      {
        path: 'checkout',
        canMatch: [authGuard],
        loadComponent: () =>
          import('@features/checkout/pages/checkout-page.component').then(
            (m) => m.CheckoutPageComponent,
          ),
      },
      {
        path: 'account',
        canMatch: [authGuard],
        loadComponent: () =>
          import('@features/account/pages/account-page.component').then((m) => m.AccountPageComponent),
      },
      {
        path: 'favorites',
        canMatch: [authGuard],
        loadComponent: () =>
          import('@features/favorites/pages/favorites-page.component').then(
            (m) => m.FavoritesPageComponent,
          ),
      },
      {
        path: 'library',
        canMatch: [authGuard],
        loadComponent: () =>
          import('@features/library/pages/library-page.component').then(
            (m) => m.LibraryPageComponent,
          ),
      },
      {
        path: 'notifications',
        canMatch: [authGuard],
        loadComponent: () =>
          import('@features/notifications/pages/notifications-page.component').then(
            (m) => m.NotificationsPageComponent,
          ),
      },
      {
        path: 'orders',
        canMatch: [authGuard],
        loadChildren: () => import('@features/orders/orders.routes').then((m) => m.ORDERS_ROUTES),
      },
    ],
  },
  {
    path: 'auth/verify-email',
    loadComponent: () =>
      import('@features/auth/pages/verify-email-page.component').then(
        (m) => m.VerifyEmailPageComponent,
      ),
  },
  {
    path: 'auth',
    canMatch: [guestGuard],
    loadComponent: () =>
      import('./layout/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent),
    loadChildren: () => import('@features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'admin',
    canMatch: [authGuard, roleGuard('ADMIN')],
    loadComponent: () =>
      import('./layout/admin-layout/admin-layout.component').then((m) => m.AdminLayoutComponent),
    loadChildren: () => import('@features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'courier',
    canMatch: [authGuard, roleGuard('REPARTIDOR')],
    loadComponent: () =>
      import('./layout/courier-layout/courier-layout.component').then(
        (m) => m.CourierLayoutComponent,
      ),
    children: [
      {
        path: '',
        title: 'Repartidor · Mis entregas',
        loadComponent: () =>
          import('@features/admin/pages/admin-shipments-page.component').then(
            (m) => m.AdminShipmentsPageComponent,
          ),
      },
    ],
  },
  {
    path: '**',
    loadComponent: () =>
      import('@shared/pages/not-found.component').then((m) => m.NotFoundComponent),
  },
];
