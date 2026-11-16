import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent) },
  { path: 'gestion/:module', loadComponent: () => import('./features/operaciones/workspace.component').then(m => m.WorkspaceComponent) },
  { path: '**', redirectTo: '' }
];
