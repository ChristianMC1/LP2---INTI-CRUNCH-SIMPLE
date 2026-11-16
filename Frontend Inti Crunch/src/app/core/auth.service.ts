import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api/api.service';
import { SessionTokens, keycloak, storeSession } from './keycloak';

export type Profile = { name: string; fullName?: string; roles: string[]; roleLabel: string; phone?: string; email?: string; dni?: string; customerId?: number; employeeId?: number };
export const STAFF_ROLES = ['ADMINISTRADOR', 'SUPERVISOR', 'CAJERO', 'OPERACIONES', 'ABASTECIMIENTO', 'REPARTIDOR'];
const ROLE_LABELS: Record<string, string> = { ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', CAJERO: 'Cajero', OPERACIONES: 'Operaciones', ABASTECIMIENTO: 'Abastecimiento', REPARTIDOR: 'Repartidor', CLIENTE: 'Cliente' };

/** Sesión de la persona: inicio con DNI, registro de clientes y perfil con su rol. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  readonly profile = signal<Profile | undefined>(undefined);
  readonly dialog = signal<'login' | 'register' | undefined>(undefined);
  readonly historyOpen = signal(false);
  readonly historyTab = signal<string | undefined>(undefined);
  /** Abre "Mi historial" (pide iniciar sesión si hace falta). */
  openHistory(tab?: string): void { this.require(() => { this.historyTab.set(tab); this.historyOpen.set(true); }); }
  /** Acción pendiente que se ejecuta en cuanto la persona inicia sesión. */
  private afterLogin: (() => void) | undefined;

  get authenticated(): boolean { return !!keycloak.authenticated; }
  get roles(): string[] { return keycloak.realmAccess?.roles ?? []; }
  get isStaff(): boolean { return this.roles.some(role => STAFF_ROLES.includes(role)); }
  get isCustomer(): boolean { return this.authenticated && !this.isStaff; }
  get displayName(): string { return this.profile()?.fullName ?? keycloak.tokenParsed?.['name'] ?? keycloak.tokenParsed?.['preferred_username'] ?? 'Invitado'; }
  get roleLabel(): string { return this.profile()?.roleLabel ?? (Object.entries(ROLE_LABELS).find(([role]) => this.roles.includes(role))?.[1] ?? 'Cliente'); }
  get initials(): string { return this.displayName.split(' ').filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase(); }

  /** Pide iniciar sesión y, al terminar, continúa con lo que la persona quería hacer. */
  require(then?: () => void, mode: 'login' | 'register' = 'login'): boolean {
    if (this.authenticated) { then?.(); return true; }
    this.afterLogin = then; this.dialog.set(mode); return false;
  }
  open(mode: 'login' | 'register' = 'login'): void { this.dialog.set(mode); }
  close(): void { this.dialog.set(undefined); this.afterLogin = undefined; }

  async login(dni: string, password: string): Promise<void> {
    const result = await firstValueFrom(this.api.post<{ tokens: SessionTokens }>('/public/auth/login', { dni: dni.trim(), password }));
    await this.useTokens(result.tokens);
  }
  async register(data: Record<string, string>): Promise<string> {
    const result = await firstValueFrom(this.api.post<{ tokens: SessionTokens; message: string }>('/public/auth/register', data));
    await this.useTokens(result.tokens);
    return result.message;
  }
  private async useTokens(tokens: SessionTokens): Promise<void> {
    keycloak.refreshToken = tokens.refresh_token;
    await keycloak.updateToken(-1);
    storeSession({ access_token: keycloak.token!, refresh_token: keycloak.refreshToken!, id_token: keycloak.idToken });
    await this.loadProfile();
    this.dialog.set(undefined);
    const next = this.afterLogin; this.afterLogin = undefined; next?.();
  }
  async loadProfile(): Promise<Profile | undefined> {
    if (!this.authenticated) { this.profile.set(undefined); return undefined; }
    try { const profile = await firstValueFrom(this.api.get<Profile>('/me')); this.profile.set(profile); return profile; }
    catch { return undefined; }
  }
  logout(): void {
    const refresh = keycloak.refreshToken;
    storeSession(undefined);
    if (refresh) void fetch('http://localhost:8180/realms/inti-crunch/protocol/openid-connect/logout', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ client_id: 'inti-crunch-web', refresh_token: refresh }) }).catch(() => undefined);
    keycloak.clearToken();
    this.profile.set(undefined);
    window.location.href = window.location.origin;
  }
}
