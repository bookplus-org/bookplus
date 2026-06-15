import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'Ingresar · BookPlus',
    loadComponent: () => import('./pages/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'register',
    title: 'Crear cuenta · BookPlus',
    loadComponent: () =>
      import('./pages/register-page.component').then((m) => m.RegisterPageComponent),
  },
  {
    path: 'forgot-password',
    title: 'Recuperar contraseña · BookPlus',
    loadComponent: () =>
      import('./pages/forgot-password-page.component').then((m) => m.ForgotPasswordPageComponent),
  },
  {
    path: 'reset-password',
    title: 'Restablecer contraseña · BookPlus',
    loadComponent: () =>
      import('./pages/reset-password-page.component').then((m) => m.ResetPasswordPageComponent),
  },
];
