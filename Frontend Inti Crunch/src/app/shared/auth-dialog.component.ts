import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/ui';

/** Ventana para iniciar sesión con DNI o crear una cuenta nueva. */
@Component({
  selector: 'ic-auth-dialog', standalone: true, imports: [CommonModule],
  template: `
  <div class="ic-overlay" *ngIf="auth.dialog() as mode" (click)="!busy && auth.close()">
    <section class="ic-dialog auth-dialog" role="dialog" aria-modal="true" [attr.aria-label]="mode==='login'?'Iniciar sesión':'Crear cuenta'" (click)="$event.stopPropagation()">
      <header class="ic-dialog-head"><div><span class="ic-kicker">TU CUENTA INTI CRUNCH</span><h2>{{mode==='login'?'¡Hola de nuevo!':'Crea tu cuenta'}}</h2><p>{{mode==='login'?'Entra con tu DNI y tu contraseña.':'Solo una vez. Después tus datos se completan solos.'}}</p></div><button class="ic-close" type="button" aria-label="Cerrar" [disabled]="busy" (click)="auth.close()">×</button></header>
      <div class="auth-tabs" role="tablist"><button type="button" role="tab" [class.active]="mode==='login'" (click)="switchTo('login')">Ya tengo cuenta</button><button type="button" role="tab" [class.active]="mode==='register'" (click)="switchTo('register')">Soy nuevo</button></div>
      <form *ngIf="mode==='login'" (submit)="login($event)" class="auth-form">
        <label class="ic-field"><span>DNI</span><input name="dni" inputmode="numeric" autocomplete="username" maxlength="40" required placeholder="Ej. 71234567" [value]="dni" (input)="dni=$any($event.target).value"><small>El personal también puede usar su usuario.</small></label>
        <label class="ic-field"><span>Contraseña</span><div class="password-box"><input name="password" [type]="show?'text':'password'" autocomplete="current-password" required [value]="password" (input)="password=$any($event.target).value"><button type="button" class="ic-text-button" (click)="show=!show">{{show?'Ocultar':'Ver'}}</button></div></label>
        <p *ngIf="error" class="ic-alert error" role="alert">{{error}}</p>
        <button class="ic-primary auth-submit" type="submit" [disabled]="busy">{{busy?'Entrando…':'Entrar'}} →</button>
        <p class="auth-switch">¿No tienes cuenta? <button type="button" class="ic-text-button" (click)="switchTo('register')">Regístrate gratis</button></p>
      </form>
      <form *ngIf="mode==='register'" (submit)="register($event)" class="auth-form">
        <div class="ic-form-grid">
          <label class="ic-field"><span>Nombres *</span><input required maxlength="60" autocomplete="given-name" placeholder="Ej. María" [value]="form.firstName" (input)="form.firstName=$any($event.target).value"></label>
          <label class="ic-field"><span>Apellidos *</span><input required maxlength="80" autocomplete="family-name" placeholder="Ej. Hanco Quispe" [value]="form.lastName" (input)="form.lastName=$any($event.target).value"></label>
          <label class="ic-field"><span>DNI *</span><input required inputmode="numeric" maxlength="8" placeholder="8 números" [value]="form.dni" (input)="form.dni=digits($event,8)"><small [class.ok]="form.dni.length===8">{{form.dni.length}}/8 números</small></label>
          <label class="ic-field"><span>Celular *</span><input required inputmode="tel" maxlength="9" autocomplete="tel" placeholder="9XXXXXXXX" [value]="form.phone" (input)="form.phone=digits($event,9)"><small [class.ok]="form.phone.length===9">{{form.phone.length}}/9 números</small></label>
          <label class="ic-field wide-field"><span>Correo *</span><input required type="email" maxlength="150" autocomplete="email" placeholder="nombre@correo.com" [value]="form.email" (input)="form.email=$any($event.target).value"></label>
          <label class="ic-field"><span>Contraseña *</span><input required [type]="show?'text':'password'" autocomplete="new-password" [value]="form.password" (input)="form.password=$any($event.target).value"><small [class.ok]="strong">Mínimo 8, con letras y números</small></label>
          <label class="ic-field"><span>Repite la contraseña *</span><input required [type]="show?'text':'password'" autocomplete="new-password" [value]="form.confirmPassword" (input)="form.confirmPassword=$any($event.target).value"><small *ngIf="form.confirmPassword" [class.ok]="form.confirmPassword===form.password">{{form.confirmPassword===form.password?'¡Coinciden!':'Todavía no coinciden'}}</small></label>
        </div>
        <label class="auth-show"><input type="checkbox" [checked]="show" (change)="show=$any($event.target).checked"> Mostrar contraseñas</label>
        <p *ngIf="error" class="ic-alert error" role="alert">{{error}}</p>
        <button class="ic-primary auth-submit" type="submit" [disabled]="busy">{{busy?'Creando tu cuenta…':'Crear mi cuenta'}} →</button>
        <p class="auth-switch">¿Ya tienes cuenta? <button type="button" class="ic-text-button" (click)="switchTo('login')">Inicia sesión</button></p>
      </form>
    </section>
  </div>`
})
export class AuthDialogComponent {
  readonly auth = inject(AuthService);
  private readonly detector = inject(ChangeDetectorRef);
  dni = ''; password = ''; show = false; busy = false; error = '';
  form = { firstName: '', lastName: '', dni: '', phone: '', email: '', password: '', confirmPassword: '' };
  get strong(): boolean { return this.form.password.length >= 8 && /[A-Za-z]/.test(this.form.password) && /\d/.test(this.form.password); }
  switchTo(mode: 'login' | 'register'): void { this.error = ''; this.auth.dialog.set(mode); }
  digits(event: Event, max: number): string { const input = event.target as HTMLInputElement; input.value = input.value.replace(/\D/g, '').slice(0, max); return input.value; }
  async login(event: Event): Promise<void> {
    event.preventDefault(); if (this.busy) return;
    if (!this.dni.trim() || !this.password) { this.error = 'Escribe tu DNI y tu contraseña.'; return; }
    this.busy = true; this.error = '';
    try { await this.auth.login(this.dni, this.password); this.password = ''; }
    catch (error) { this.error = errorMessage(error); }
    finally { this.busy = false; this.detector.markForCheck(); }
  }
  async register(event: Event): Promise<void> {
    event.preventDefault(); if (this.busy) return;
    const f = this.form;
    const problem = !f.firstName.trim() || !f.lastName.trim() ? 'Escribe tus nombres y apellidos.' : f.dni.length !== 8 ? 'El DNI debe tener 8 números.' : !/^9\d{8}$/.test(f.phone) ? 'El celular debe tener 9 números y empezar con 9.' : !/^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/.test(f.email) ? 'Escribe un correo válido.' : !this.strong ? 'La contraseña debe tener al menos 8 caracteres, con letras y números.' : f.password !== f.confirmPassword ? 'Las contraseñas no coinciden.' : '';
    if (problem) { this.error = problem; return; }
    this.busy = true; this.error = '';
    try { await this.auth.register({ ...f }); this.form = { firstName: '', lastName: '', dni: '', phone: '', email: '', password: '', confirmPassword: '' }; }
    catch (error) { this.error = errorMessage(error); }
    finally { this.busy = false; this.detector.markForCheck(); }
  }
}
