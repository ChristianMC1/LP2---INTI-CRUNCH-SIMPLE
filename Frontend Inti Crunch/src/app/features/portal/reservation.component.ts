import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth.service';
import { DataRow, errorMessage } from '../../core/ui';

type TableChoice = { id: number; code: string; capacity: number; area: string; available: boolean };

/** "Aparta tu mesa": reserva del cliente con sus datos ya completados. */
@Component({
  selector: 'ic-reservation', standalone: true, imports: [CommonModule],
  template: `
  <div class="ic-overlay" (click)="!busy && closed.emit()">
    <section class="ic-dialog reservation-dialog" role="dialog" aria-modal="true" aria-label="Aparta tu mesa" (click)="$event.stopPropagation()">
      <header class="ic-dialog-head"><div><span class="ic-kicker">RESERVA EN LA RINCONADA</span><h2>Aparta tu <em>mesa.</em></h2><p>Elige el día, la hora y tu mesa. Tus datos ya están listos.</p></div><button class="ic-close" type="button" aria-label="Cerrar" [disabled]="busy" (click)="closed.emit()">×</button></header>
      <div *ngIf="done" class="reservation-done"><span>✓</span><h3>¡Tu mesa te espera!</h3><p>{{done}}</p><button class="ic-primary" (click)="closed.emit()">Perfecto</button></div>
      <form *ngIf="!done" (submit)="confirm($event)">
        <div class="reservation-grid">
          <p class="reservation-owner">Reserva a nombre de <b>{{name}}</b>{{dni?' · DNI '+dni:''}}. Usaremos tus datos registrados.</p>
          <label class="ic-field"><span>Teléfono de contacto</span><input inputmode="tel" maxlength="9" [value]="phone" (input)="phone=$any($event.target).value"></label>
          <label class="ic-field"><span>¿Cuándo vienes?</span><input type="date" [min]="minDate" [max]="maxDate" [value]="date" (change)="date=$any($event.target).value;load()"></label>
          <label class="ic-field"><span>¿A qué hora?</span><select (change)="time=$any($event.target).value;load()"><option *ngFor="let slot of slots" [value]="slot" [selected]="slot===time">{{slot}}</option></select></label>
          <label class="ic-field"><span>Personas</span><select (change)="party=+$any($event.target).value"><option *ngFor="let n of people" [value]="n" [selected]="n===party">{{n}} {{n===1?'persona':'personas'}}</option></select></label>
        </div>
        <div class="ic-field wide-field"><span>Elige tu mesa</span>
          <p class="ic-help" *ngIf="loading">Buscando mesas libres…</p>
          <div class="table-picker-grid" *ngIf="!loading"><button type="button" *ngFor="let table of tables" class="table-pick" [class.chosen]="chosen.has(table.id)" [class.busy]="!table.available" [disabled]="!table.available" (click)="toggle(table)"><b>{{table.code}}</b><small>{{table.capacity}} personas</small><em>{{table.available?(chosen.has(table.id)?'Elegida ✓':'Disponible'):'Reservada'}}</em></button></div>
          <p class="ic-help" *ngIf="!loading && !tables.length">No hay mesas para mostrar.</p>
        </div>
        <label class="ic-field wide-field"><span>Un detalle para preparar la mesa <small>(opcional)</small></span><input maxlength="300" placeholder="Ej. silla para bebé, cumpleaños…" [value]="notes" (input)="notes=$any($event.target).value"></label>
        <p class="table-picker-note" [class.ok]="seats>=party && seats>0">{{seatsMessage}}</p>
        <p *ngIf="error" class="ic-alert error" role="alert">{{error}}</p>
        <button class="ic-primary checkout-submit" type="submit" [disabled]="busy||loading">{{busy?'Confirmando tu mesa…':'Confirmar mi mesa'}} →</button>
        <p class="checkout-demo-note">Si prefieres llegar sin reservar, eres bienvenido igual.</p>
      </form>
    </section>
  </div>`
})
export class ReservationComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly detector = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  @Output() closed = new EventEmitter<void>();
  @Output() booked = new EventEmitter<void>();
  readonly slots = Array.from({ length: 21 }, (_, index) => { const minutes = 11 * 60 + index * 30; return String(Math.floor(minutes / 60)).padStart(2, '0') + ':' + String(minutes % 60).padStart(2, '0'); });
  readonly people = Array.from({ length: 16 }, (_, index) => index + 1);
  name = ''; dni = ''; phone = ''; date = ''; time = '13:00'; party = 2; notes = '';
  tables: TableChoice[] = []; chosen = new Set<number>();
  loading = false; busy = false; error = ''; done = '';
  minDate = this.dateInput(new Date()); maxDate = this.dateInput(new Date(Date.now() + 60 * 86400000));
  get seats(): number { return this.tables.filter(table => this.chosen.has(table.id)).reduce((sum, table) => sum + table.capacity, 0); }
  get seatsMessage(): string { if (!this.chosen.size) return `Toca una o varias mesas libres; necesitas ${this.party} lugares.`; return this.seats >= this.party ? `✓ Elegiste ${this.chosen.size} mesa(s) con ${this.seats} lugares para ${this.party} personas.` : `Necesitas ${this.party} lugares y elegiste ${this.seats}. Elige otra mesa más.`; }
  async ngOnInit(): Promise<void> {
    const profile = this.auth.profile() ?? await this.auth.loadProfile();
    this.name = profile?.fullName ?? this.auth.displayName; this.dni = profile?.dni ?? ''; this.phone = profile?.phone ?? '';
    const now = new Date(); const next = this.slots.find(slot => this.at(this.dateInput(now), slot).getTime() > now.getTime() + 30 * 60000);
    if (next) { this.date = this.dateInput(now); this.time = next; } else { this.date = this.dateInput(new Date(now.getTime() + 86400000)); this.time = '13:00'; }
    await this.load();
  }
  private dateInput(date: Date): string { const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000); return local.toISOString().slice(0, 10); }
  private at(date: string, time: string): Date { return new Date(date + 'T' + time + ':00'); }
  toggle(table: TableChoice): void { if (this.chosen.has(table.id)) this.chosen.delete(table.id); else this.chosen.add(table.id); this.chosen = new Set(this.chosen); }
  async load(): Promise<void> {
    if (!this.date || !this.time) return; this.loading = true; this.error = '';
    try { const start = this.at(this.date, this.time); this.tables = await firstValueFrom(this.api.get<TableChoice[]>('/customer/reservations/tables?start=' + encodeURIComponent(start.toISOString()))); const free = new Set(this.tables.filter(table => table.available).map(table => table.id)); this.chosen = new Set([...this.chosen].filter(id => free.has(id))); }
    catch (error) { this.error = errorMessage(error); } finally { this.loading = false; this.detector.markForCheck(); }
  }
  async confirm(event: Event): Promise<void> {
    event.preventDefault(); if (this.busy) return;
    if (!/^9\d{8}$/.test(this.phone.replace(/\s/g, ''))) { this.error = 'Escribe un celular de 9 números que empiece con 9.'; return; }
    if (!this.chosen.size) { this.error = 'Elige al menos una mesa libre.'; return; }
    if (this.seats < this.party) { this.error = this.seatsMessage; return; }
    this.busy = true; this.error = '';
    try {
      const result = await firstValueFrom(this.api.post<DataRow>('/customer/reservations', { startsAt: this.at(this.date, this.time).toISOString(), partySize: this.party, tableIds: [...this.chosen], notes: this.notes, phone: this.phone.replace(/\s/g, '') }));
      const codes = this.tables.filter(table => this.chosen.has(table.id)).map(table => table.code).join(', ');
      this.done = `${result['message']} Te esperamos el ${new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' }).format(this.at(this.date, this.time))} a las ${this.time} en la mesa ${codes}.`;
      this.booked.emit();
    } catch (error) { this.error = errorMessage(error); await this.load(); } finally { this.busy = false; this.detector.markForCheck(); }
  }
}
