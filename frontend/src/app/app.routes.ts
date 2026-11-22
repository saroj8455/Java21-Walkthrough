import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    title: 'Dashboard | Java 21 Walkthrough',
    // Lazy-loaded standalone component -> separate JS chunk.
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'transactions',
    title: 'Transactions | Java 21 Walkthrough',
    loadComponent: () =>
      import('./features/transactions/transactions-page.component').then((m) => m.TransactionsPageComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
