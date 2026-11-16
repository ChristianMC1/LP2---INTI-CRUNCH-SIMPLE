import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth.service';
import { DataRow, downloadPdf, errorMessage, labels, money } from '../../core/ui';

export type CatalogProduct = { id: number; name: string; description: string; price: number; category: string; imageUrl: string; };
export type CartLine = { product: CatalogProduct; quantity: number; optionIds: number[]; notes: string; };
type SeatChoice = { id: number; code: string; capacity: number; area: string; state: string; available: boolean };

@Component({ selector: 'ic-checkout', standalone: true, imports: [CommonModule], templateUrl: './checkout.component.html' })
export class CheckoutComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly detector = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  @Input() lines: CartLine[] = [];
  @Input() resumeToken = '';
  @Input() promotions: DataRow[] = [];
  @Output() closed = new EventEmitter<void>();
  @Output() ordered = new EventEmitter<string>();
  readonly money = money;
  readonly labels = labels;
  options: DataRow[] = []; zones: DataRow[] = []; documents: DataRow[] = []; seats: SeatChoice[] = [];
  customerName = ''; customerPhone = ''; channel = 'PICKUP'; zoneId = ''; deliveryAddress = ''; promotionCode = ''; notes = ''; tableId: number | undefined; partySize = 2;
  promo: DataRow | undefined; promoChecking = false;
  method = 'CASH'; documentType = 'BOLETA'; documentNumber = ''; businessName = ''; customerAddress = '';
  busy = false; loading = false; error = ''; message = ''; order: DataRow | undefined; token = '';
  private requestKey = crypto.randomUUID();
  private paymentKey = crypto.randomUUID();
  private timer: ReturnType<typeof setInterval> | undefined;
  get subtotal(): number { return this.lines.reduce((total, line) => total + this.unitPrice(line) * line.quantity, 0); }
  get fee(): number { return this.channel === 'DELIVERY' ? Number(this.zones.find(zone => String(zone['id']) === this.zoneId)?.['fee'] ?? 0) : 0; }
  get discount(): number { return this.promo?.['valid'] ? Number(this.promo['discount']) : 0; }
  get total(): number { return Math.max(0, this.subtotal - this.discount) + this.fee; }
  get count(): number { return this.lines.reduce((total, line) => total + line.quantity, 0); }
  async ngOnInit(): Promise<void> {
    this.loading = true;
    try {
      const profile = this.auth.profile() ?? await this.auth.loadProfile();
      this.customerName = profile?.fullName ?? this.auth.displayName; this.customerPhone = profile?.phone ?? '';
      [this.options, this.zones] = await Promise.all([firstValueFrom(this.api.get<DataRow[]>('/public/options')), firstValueFrom(this.api.get<DataRow[]>('/public/zones'))]);
      if (this.resumeToken) { this.token = this.resumeToken; await this.refresh(); }
    } catch (error) { this.error = errorMessage(error); } finally { this.loading = false; this.detector.markForCheck(); }
    this.timer = setInterval(() => { if (this.token && !this.busy && !document.hidden) void this.refresh(true); }, 15000);
  }
  ngOnDestroy(): void { if (this.timer) clearInterval(this.timer); }
  availableOptions(line: CartLine): DataRow[] { return this.options.filter(option => option['productId'] === line.product.id); }
  unitPrice(line: CartLine): number { return line.product.price + this.availableOptions(line).filter(option => line.optionIds.includes(option['id'])).reduce((sum, option) => sum + Number(option['price']), 0); }
  toggle(line: CartLine, id: number, checked: boolean): void { line.optionIds = checked ? [...line.optionIds, id] : line.optionIds.filter(option => option !== id); this.recheckPromo(); }
  quantity(line: CartLine, change: number): void { line.quantity = Math.max(0, Math.min(100, line.quantity + change)); const index = this.lines.indexOf(line); if (line.quantity === 0) this.lines.splice(index, 1); this.recheckPromo(); }
  async chooseChannel(code: string): Promise<void> { this.channel = code; this.error = ''; if (code === 'DINE_IN') await this.loadSeats(); }
  async loadSeats(): Promise<void> { try { this.seats = await firstValueFrom(this.api.get<SeatChoice[]>('/customer/tables/now')); const mine = this.seats.find(seat => ['TU_RESERVA', 'TU_MESA'].includes(seat.state)); if (mine && !this.tableId) this.tableId = mine.id; } catch (error) { this.error = errorMessage(error); } finally { this.detector.markForCheck(); } }
  seatLabel(seat: SeatChoice): string { return ({ LIBRE: 'Libre', OCUPADA: 'Ocupada', RESERVADA: 'Reservada', TU_RESERVA: 'Tu reserva', TU_MESA: 'Tu mesa' } as Record<string, string>)[seat.state] ?? seat.state; }
  private cartLines(): DataRow[] { return this.lines.map(line => ({ productId: line.product.id, quantity: line.quantity, optionIds: line.optionIds })); }
  async applyPromo(code = this.promotionCode): Promise<void> {
    this.promotionCode = code.trim().toUpperCase(); if (!this.promotionCode) { this.promo = undefined; return; }
    this.promoChecking = true;
    try { this.promo = await firstValueFrom(this.api.post<DataRow>('/public/promotions/preview', { code: this.promotionCode, lines: this.cartLines() })); }
    catch (error) { this.promo = { valid: false, message: errorMessage(error) }; }
    finally { this.promoChecking = false; this.detector.markForCheck(); }
  }
  removePromo(): void { this.promo = undefined; this.promotionCode = ''; }
  private recheckPromo(): void { if (this.promotionCode && this.lines.length) void this.applyPromo(); }
  async place(event: Event): Promise<void> {
    event.preventDefault(); if (this.busy || !this.lines.length) return;
    if (this.channel === 'DINE_IN' && !this.tableId) { this.error = 'Elige la mesa donde vas a comer.'; return; }
    if (this.channel === 'DELIVERY' && (!this.zoneId || !this.deliveryAddress.trim())) { this.error = 'Elige tu zona y escribe tu dirección.'; return; }
    if (!/^9\d{8}$/.test(this.customerPhone.replace(/\s/g, ''))) { this.error = 'Escribe un celular de 9 números que empiece con 9.'; return; }
    this.busy = true; this.error = '';
    try {
      const result = await firstValueFrom(this.api.post<DataRow>('/public/orders', { requestKey: this.requestKey, customerName: this.customerName, customerPhone: this.customerPhone.replace(/\s/g, ''), channel: this.channel, ...(this.channel === 'DELIVERY' ? { zoneId: Number(this.zoneId), deliveryAddress: this.deliveryAddress } : {}), ...(this.channel === 'DINE_IN' ? { tableId: this.tableId, partySize: this.partySize } : {}), promotionCode: this.promo?.['valid'] ? this.promotionCode : '', notes: this.notes, lines: this.lines.map(line => ({ productId: line.product.id, quantity: line.quantity, optionIds: line.optionIds, notes: line.notes })) }));
      this.order = result; this.token = result['trackingToken']; this.ordered.emit(this.token); this.message = '¡Listo! Tu pedido ya está en cocina.'; await this.refresh();
    } catch (error) { this.error = errorMessage(error); } finally { this.busy = false; this.detector.markForCheck(); }
  }
  async pay(event: Event): Promise<void> {
    event.preventDefault(); if (this.busy || !this.order) return; this.busy = true; this.error = '';
    try { const result = await firstValueFrom(this.api.post<DataRow>('/public/orders/' + this.token + '/payments', { requestKey: this.paymentKey, method: this.method, documentType: this.documentType, customerDocumentNumber: this.documentNumber, customerBusinessName: this.businessName, customerAddress: this.customerAddress })); this.documents = result['documents'] ?? []; this.message = '¡Pago confirmado! Tu comprobante está listo.'; await this.refresh(); for (const receipt of this.documents) await this.download(receipt['id']); }
    catch (error) { this.error = errorMessage(error); } finally { this.busy = false; this.detector.markForCheck(); }
  }
  async refresh(silent = false): Promise<void> { if (!this.token) return; try { const [order, documents] = await Promise.all([firstValueFrom(this.api.get<DataRow>('/public/orders/' + this.token)), firstValueFrom(this.api.get<DataRow[]>('/public/orders/' + this.token + '/documents'))]); this.order = order; this.documents = documents; if (!silent) this.error = ''; } catch (error) { if (!silent) this.error = errorMessage(error); } finally { this.detector.markForCheck(); } }
  async download(id: number): Promise<void> { try { const blob = await firstValueFrom(this.api.pdf('/public/orders/' + this.token + '/documents/' + id + '/pdf')); downloadPdf(blob, 'Inti-Crunch-comprobante-' + id + '.pdf'); } catch { this.error = 'El pago está registrado. No se pudo descargar el PDF; vuelve a tocar el botón de descarga.'; } this.detector.markForCheck(); }
  get steps(): string[] { return this.order?.['channel'] === 'DELIVERY' ? ['Recibido', 'Preparando', 'Listo', 'En camino', 'Entregado'] : ['Recibido', 'Preparando', 'Listo', 'Entregado']; }
  get progress(): number { const states = this.order?.['channel'] === 'DELIVERY' ? ['RECEIVED', 'PREPARING', 'READY', 'DISPATCHED', 'COMPLETED'] : ['RECEIVED', 'PREPARING', 'READY', 'COMPLETED']; return states.indexOf(this.order?.['status']); }
  promoText(promotion: DataRow): string { return promotion['discountPercent'] ? promotion['discountPercent'] + ' % menos' : money(promotion['discountAmount']) + ' menos'; }
}
