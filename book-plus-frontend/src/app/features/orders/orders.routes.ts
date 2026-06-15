import { Routes } from '@angular/router';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mis pedidos · BookPlus',
    loadComponent: () =>
      import('./pages/order-list-page.component').then((m) => m.OrderListPageComponent),
  },
  {
    path: ':id',
    title: 'Pedido · BookPlus',
    loadComponent: () =>
      import('./pages/order-detail-page.component').then((m) => m.OrderDetailPageComponent),
  },
];
