import { Routes } from '@angular/router';
import { authenticatedGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'doctor',
    canActivate: [authenticatedGuard, roleGuard('doctor')],
    loadComponent: () => import('./doctor/doctor').then((m) => m.Doctor),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'home', redirectTo: 'dashboard' },
      { path: 'overview', redirectTo: 'dashboard' },
      { path: 'patients', redirectTo: 'clients' },
      { path: 'clients', loadComponent: () => import('./secretaire/pages/clients/clients.component').then((m) => m.ClientsComponent) },
      { path: 'treatments', loadComponent: () => import('./doctor/pages/treatment-management.component').then((m) => m.TreatmentManagementComponent) },
      { path: 'schedule', redirectTo: 'appointments' },
      { path: 'appointments', loadComponent: () => import('./secretaire/pages/appointments/appointments.component').then((m) => m.AppointmentsComponent) },
      { path: 'payments', loadComponent: () => import('./secretaire/pages/payments/payments.component').then((m) => m.PaymentsComponent) },
      { path: 'expenses', loadComponent: () => import('./secretaire/pages/expenses/expenses.component').then((m) => m.ExpensesComponent) },
      { path: 'messages', loadComponent: () => import('./secretaire/pages/messages/messages.component').then((m) => m.MessagesComponent) },
      { path: 'materials', loadComponent: () => import('./secretaire/pages/materials/materials.component').then((m) => m.MaterialsComponent) },
      { path: 'money', redirectTo: 'expenses' },
      { path: 'dashboard', loadComponent: () => import('./secretaire/pages/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
    ],
  },
  {
    path: 'secretaire',
    canActivate: [authenticatedGuard, roleGuard('secretaire')],
    loadComponent: () => import('./secretaire/secretaire').then((m) => m.Secretaire),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'home', loadComponent: () => import('./secretaire/pages/home/home.component').then((m) => m.HomeComponent) },
      { path: 'clients', loadComponent: () => import('./secretaire/pages/clients/clients.component').then((m) => m.ClientsComponent) },
      { path: 'appointments', loadComponent: () => import('./secretaire/pages/appointments/appointments.component').then((m) => m.AppointmentsComponent) },
      { path: 'payments', loadComponent: () => import('./secretaire/pages/payments/payments.component').then((m) => m.PaymentsComponent) },
      { path: 'expenses', loadComponent: () => import('./secretaire/pages/expenses/expenses.component').then((m) => m.ExpensesComponent) },
      { path: 'messages', loadComponent: () => import('./secretaire/pages/messages/messages.component').then((m) => m.MessagesComponent) },
      { path: 'materials', loadComponent: () => import('./secretaire/pages/materials/materials.component').then((m) => m.MaterialsComponent) },
      { path: 'dashboard', loadComponent: () => import('./secretaire/pages/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
    ],
  },
  {
    path: 'manager',
    canActivate: [authenticatedGuard, roleGuard('manager')],
    loadComponent: () => import('./manager/manager.component').then((m) => m.ManagerComponent),
  },
  {
    path: 'invite/:token',
    loadComponent: () => import('./invite/invite.component').then((m) => m.InviteComponent),
  },
  {
    path: 'admin',
    canActivate: [authenticatedGuard, roleGuard('admin')],
    loadComponent: () => import('./admin/admin.component').then((m) => m.AdminComponent),
  },
  { path: '**', redirectTo: 'login' },
];
