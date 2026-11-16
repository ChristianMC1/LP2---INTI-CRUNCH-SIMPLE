import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthDialogComponent } from './shared/auth-dialog.component';
import { AccountHistoryComponent } from './shared/account-history.component';
import { AuthService } from './core/auth.service';

@Component({ selector: 'ic-root', standalone: true, imports: [RouterOutlet, AuthDialogComponent, AccountHistoryComponent], template: '<router-outlet /><ic-auth-dialog /><ic-account-history />' })
export class AppComponent {
  constructor() { void inject(AuthService).loadProfile(); }
}
