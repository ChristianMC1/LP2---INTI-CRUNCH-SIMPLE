import { ChangeDetectorRef, Component, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../core/api/api.service';
import { AuthService } from '../core/auth.service';
import { DataRow, downloadPdf, errorMessage, labels, money } from '../core/ui';

type Tab = { key: string; label: string; count: number; help?: string; rows?: DataRow[] };

/** "Mi historial": pedidos, compras, reservas y puntos del cliente; y lo que registró si es parte del equipo. */
@Component({
  selector: 'ic-account-history', standalone: true, imports: [CommonModule],
  template: `
  <div class="ic-overlay" *ngIf="auth.historyOpen()" (click)="close()">
    <section class="ic-dialog wide history-dialog" role="dialog" aria-modal="true" aria-label="Mi historial" (click)="$event.stopPropagation()">
      <header class="history-hero">
        <span class="ops-avatar big">{{auth.initials}}</span>
        <div><span class="ic-kicker">MI CUENTA · {{auth.roleLabel.toUpperCase()}}</span><h2>{{profile['fullName'] || auth.displayName}}</h2><p>{{profile['dni'] ? 'DNI '+profile['dni'] : ''}}{{profile['phone'] ? ' · '+profile['phone'] : ''}}{{profile['email'] ? ' · '+profile['email'] : ''}}</p></div>
        <button class="ic-close" aria-label="Cerrar" (click)="close()">×</button>
      </header>
      <div class="history-stats" *ngIf="data">
        <article><b>{{orders.length}}</b><span>pedidos</span></article>
        <article><b>{{money(data['totalSpent'])}}</b><span>en compras entregadas</span></article>
        <article><b>{{reservations.length}}</b><span>reservas</span></article>
        <article><b>{{data['points']}}</b><span>puntos</span></article>
        <article *ngIf="work.length"><b>{{workCount}}</b><span>registros de trabajo</span></article>
      </div>
      <p *ngIf="loading" class="ops-loading">Cargando tu historial…</p>
      <p *ngIf="error" class="ic-alert error">{{error}}</p>
      <nav class="history-tabs" *ngIf="data"><button *ngFor="let item of tabs" [class.active]="tab===item.key" (click)="tab=item.key">{{item.label}} <i>{{item.count}}</i></button></nav>

      <ng-container *ngIf="data && tab==='orders'">
        <div class="ic-empty" *ngIf="!orders.length"><span>♡</span><h3>Aún no tienes pedidos.</h3><p>Cuando pidas, aquí verás cada detalle y tus boletas.</p></div>
        <article class="history-card" *ngFor="let order of orders" [class.open]="expanded===order['id']">
          <button class="history-card-head" (click)="expanded=expanded===order['id']?undefined:order['id']">
            <div><b>Pedido N° {{order['number']}}</b><small>{{date(order['createdAt'])}} · {{channel(order)}}</small></div>
            <span class="ic-badge" [ngClass]="badge(order['status'])">{{labels[order['status']]}}</span>
            <strong>{{money(order['total'])}}</strong><i>{{expanded===order['id']?'▲':'▼'}}</i>
          </button>
          <p class="history-preview" *ngIf="expanded!==order['id']">{{preview(order)}}</p>
          <div class="history-detail" *ngIf="expanded===order['id']">
            <table class="ic-table"><thead><tr><th>Cant.</th><th>Producto</th><th>Precio</th><th>Subtotal</th></tr></thead><tbody><tr *ngFor="let line of order['lines']"><td>{{line['quantity']}}</td><td><b>{{line['product']['name']}}</b><small *ngIf="line['options']?.length" class="line-extra">Con: {{optionNames(line)}}</small><small *ngIf="line['notes']" class="line-extra">“{{line['notes']}}”</small></td><td>{{money(line['unitPrice'])}}</td><td>{{money(line['subtotal'])}}</td></tr></tbody></table>
            <div class="history-totals"><div><span>Productos</span><b>{{money(order['subtotal'])}}</b></div><div *ngIf="order['discount']>0"><span>Descuento / promoción</span><b class="minus">− {{money(order['discount'])}}</b></div><div *ngIf="order['deliveryFee']>0"><span>Envío</span><b>{{money(order['deliveryFee'])}}</b></div><div class="grand"><span>Total</span><b>{{money(order['total'])}}</b></div><div><span>Pagado</span><b>{{money(order['paid'])}}</b></div><div *ngIf="order['balance']>0"><span>Falta pagar</span><b class="due">{{money(order['balance'])}}</b></div></div>
            <p class="history-meta" *ngIf="order['deliveryAddress']">Entrega en: {{order['deliveryAddress']}} ({{order['deliveryZone']}})</p>
            <p class="history-meta" *ngIf="order['notes']">Nota: {{order['notes']}}</p>
            <div class="history-timeline"><h4>Recorrido</h4><div *ngFor="let event of order['history']"><i></i><b>{{labels[event['Estado']] ?? event['Estado']}}</b><small>{{date(event['Fecha'])}}</small></div></div>
            <div class="history-docs" *ngIf="order['documents']?.length"><h4>Comprobantes</h4><button *ngFor="let doc of order['documents']" class="ic-secondary small" (click)="downloadOrderDoc(order,doc)">▤ {{doc['tipo']}} {{doc['serie']}}-{{doc['numero']}} · {{money(doc['total'])}} ↓</button></div>
          </div>
        </article>
      </ng-container>

      <ng-container *ngIf="data && tab==='reservations'">
        <div class="ic-empty" *ngIf="!reservations.length"><span>📅</span><h3>No tienes reservas.</h3></div>
        <article class="history-card simple" *ngFor="let reservation of reservations"><div class="history-card-head static"><div><b>Mesa {{reservation['tables'] || 'por asignar'}}</b><small>{{date(reservation['startsAt'])}} · {{reservation['partySize']}} personas</small></div><span class="ic-badge" [ngClass]="badge(reservation['status'])">{{reservationStatus(reservation['status'])}}</span></div><p class="history-preview" *ngIf="reservation['notes']">“{{reservation['notes']}}”</p><button *ngIf="reservation['status']==='BOOKED'" class="ic-text-button danger" (click)="cancel(reservation)">Cancelar reserva</button></article>
      </ng-container>

      <ng-container *ngIf="data && tab==='points'">
        <p class="ic-help">Ganas 1 punto por cada sol de tus pedidos entregados.</p>
        <ng-container *ngTemplateOutlet="table; context:{rows:data['pointMovements']}"></ng-container>
      </ng-container>

      <ng-container *ngFor="let section of work"><ng-container *ngIf="tab===section['key']">
        <p class="ic-help">{{section['help']}}</p>
        <ng-container *ngTemplateOutlet="table; context:{rows:section['rows'], payments:section['key']==='payments'}"></ng-container>
      </ng-container></ng-container>

      <ng-template #table let-rows="rows" let-payments="payments">
        <div class="ic-empty compact" *ngIf="!rows?.length"><span>✳</span><h3>Sin registros todavía.</h3></div>
        <div class="ic-table-scroll" *ngIf="rows?.length"><table class="ic-table"><thead><tr><th>N°</th><th *ngFor="let column of columns(rows)">{{column}}</th><th *ngIf="payments">Boleta</th></tr></thead><tbody><tr *ngFor="let row of rows; let i=index"><td class="num-cell">{{i+1}}</td><td *ngFor="let column of columns(rows)"><span *ngIf="column==='Estado';else plain" class="ic-badge" [ngClass]="badge(row[column])">{{format(row[column],column)}}</span><ng-template #plain>{{format(row[column],column)}}</ng-template></td><td *ngIf="payments"><button *ngIf="row['_documentId']" class="ic-text-button" (click)="downloadStaffDoc(row['_documentId'])">PDF ↓</button></td></tr></tbody></table></div>
      </ng-template>
    </section>
  </div>`
})
export class AccountHistoryComponent {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly detector = inject(ChangeDetectorRef);
  readonly money = money; readonly labels = labels;
  data: DataRow | undefined; loading = false; error = ''; tab = 'orders'; expanded: number | undefined;
  get profile(): DataRow { return this.data?.['profile'] ?? this.auth.profile() ?? {}; }
  get orders(): DataRow[] { return this.data?.['orders'] ?? []; }
  get reservations(): DataRow[] { return this.data?.['reservations'] ?? []; }
  get work(): DataRow[] { return this.data?.['work'] ?? []; }
  get workCount(): number { return this.work.reduce((sum, section) => sum + section['rows'].length, 0); }
  get tabs(): Tab[] {
    const tabs: Tab[] = [{ key: 'orders', label: 'Mis pedidos', count: this.orders.length }, { key: 'reservations', label: 'Mis reservas', count: this.reservations.length }, { key: 'points', label: 'Mis puntos', count: Number(this.data?.['points'] ?? 0) }];
    return [...tabs, ...this.work.map(section => ({ key: section['key'], label: section['title'], count: section['rows'].length }))];
  }
  constructor() { effect(() => { if (this.auth.historyOpen()) void this.load(); }); }
  close(): void { this.auth.historyOpen.set(false); }
  async load(): Promise<void> {
    this.loading = true; this.error = ''; this.expanded = undefined;
    try { this.data = await firstValueFrom(this.api.get<DataRow>('/me/history')); this.tab = this.auth.historyTab() ?? (!this.orders.length && this.work.length ? this.work[0]['key'] : 'orders'); if (this.orders.length && this.tab === 'orders') this.expanded = this.orders[0]['id']; }
    catch (error) { this.error = errorMessage(error); } finally { this.loading = false; this.detector.markForCheck(); }
  }
  columns(rows: DataRow[]): string[] { return rows.length ? Object.keys(rows[0]).filter(key => !key.startsWith('_')) : []; }
  date(value: any): string { if (!value) return '—'; const date = new Date(String(value)); return Number.isNaN(date.getTime()) ? String(value) : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Lima' }).format(date); }
  format(value: any, key = ''): string {
    if (value === null || value === undefined || value === '') return '—';
    if (typeof value === 'number') return /total|monto|propina|devuelto|fondo|contado|diferencia/i.test(key) ? money(value) : new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 }).format(value);
    if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) return this.date(value);
    return labels[String(value)] ?? String(value);
  }
  badge(status: any): string { const value = String(status); return ['COMPLETED', 'DELIVERED', 'PRESENT', 'ARRIVED', 'RECEIVED_PO', 'CLOSED'].includes(value) ? 'good' : ['CANCELLED', 'NO_SHOW', 'ABSENT'].includes(value) ? 'bad' : ['PREPARING', 'READY', 'LATE', 'PARTIAL', 'ON_ROUTE', 'DISPATCHED'].includes(value) ? 'warn' : 'neutral'; }
  channel(order: DataRow): string { return order['channel'] === 'DINE_IN' ? 'En el local' + (order['tableCode'] ? ' · Mesa ' + order['tableCode'] : '') : labels[order['channel']] ?? order['channel']; }
  preview(order: DataRow): string { return (order['lines'] ?? []).map((line: DataRow) => line['quantity'] + ' × ' + line['product']['name']).join(' · '); }
  optionNames(line: DataRow): string { return (line['options'] ?? []).map((option: DataRow) => option['name']).join(', '); }
  reservationStatus(status: string): string { return ({ BOOKED: 'Confirmada', ARRIVED: 'En el local', COMPLETED: 'Finalizada', CANCELLED: 'Cancelada', NO_SHOW: 'No asistió' } as Record<string, string>)[status] ?? status; }
  async downloadOrderDoc(order: DataRow, doc: DataRow): Promise<void> { try { downloadPdf(await firstValueFrom(this.api.pdf('/public/orders/' + order['trackingToken'] + '/documents/' + doc['id'] + '/pdf')), 'Inti-Crunch-' + doc['serie'] + '-' + doc['numero'] + '.pdf'); } catch (error) { this.error = errorMessage(error); this.detector.markForCheck(); } }
  async downloadStaffDoc(id: number): Promise<void> { try { downloadPdf(await firstValueFrom(this.api.pdf('/admin/cash/documents/' + id + '/pdf')), 'Inti-Crunch-' + id + '.pdf'); } catch (error) { this.error = errorMessage(error); this.detector.markForCheck(); } }
  async cancel(reservation: DataRow): Promise<void> { if (!confirm('¿Cancelar tu reserva? La mesa quedará libre.')) return; try { await firstValueFrom(this.api.patch('/customer/reservations/' + reservation['id'] + '/cancel')); await this.load(); this.tab = 'reservations'; } catch (error) { this.error = errorMessage(error); this.detector.markForCheck(); } }
}
