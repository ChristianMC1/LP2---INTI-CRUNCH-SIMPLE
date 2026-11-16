import { ChangeDetectorRef, Component, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, Subscription } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth.service';
import { keycloak } from '../../core/keycloak';
import { Choice, DataRow, Field, FormSpec, ROLE, downloadPdf, errorMessage, labels, money } from '../../core/ui';
import { FormDialogComponent } from '../../shared/form-dialog.component';
import { ModuleDef, ViewDef, dateField, enumField, modules, multiField, noteField, numberField, paymentForm, purchaseOrderForm, saleForm, selectField, textField } from './module-config';

type RowAction = { key: string; label: string; danger?: boolean; main?: boolean };
type FloorTable = { id: number; code: string; capacity: number; area: string; state: string; visit?: DataRow; reservation?: DataRow; orders: DataRow[] };

@Component({ selector: 'ic-workspace', standalone: true, imports: [CommonModule, RouterLink, FormDialogComponent], templateUrl: './workspace.component.html' })
export class WorkspaceComponent implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly detector = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  private readonly subscription: Subscription;
  private generation = 0;
  private readonly poll: ReturnType<typeof setInterval>;
  readonly money = money;
  readonly labels = labels;
  readonly today = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'America/Lima' }).format(new Date());
  active: ModuleDef = modules[0];
  view: ViewDef | undefined;
  rows: DataRow[] = [];
  floor: FloorTable[] = [];
  reservations: DataRow[] = [];
  calendarDate = new Date();
  readonly todayDate = new Date();
  lookups: Record<string, Choice[]> = {};
  dashboard: DataRow | undefined;
  search = ''; statusFilter = ''; page = 1; pageSize = 12; newestFirst = false;
  loading = false; busy = false; error = ''; notice = ''; formError = ''; sidebarOpen = false; showHelp = true;
  updated: Date | undefined;
  form: FormSpec | undefined;
  detailTitle = ''; details: { title: string; rows: DataRow[] }[] = [];
  confirmation: { message: string; run: () => Promise<void> } | undefined;

  get roles(): string[] { return keycloak.realmAccess?.roles ?? []; }
  get allowed(): ModuleDef[] { return modules.filter(module => this.has(module.roles)); }
  get authenticated(): boolean { return !!keycloak.authenticated && this.allowed.length > 0; }
  get views(): ViewDef[] { return this.active.views.filter(view => !view.roles || this.has(view.roles)); }
  get canCreate(): boolean { return !!this.view?.form && this.has(this.view.createRoles ?? this.view.roles ?? this.active.roles); }
  get columns(): string[] { const hidden = new Set(['Imagen', ...(this.view?.hide ?? [])]); return this.rows.length ? Object.keys(this.rows[0]).filter(key => !key.startsWith('_') && !hidden.has(key)) : []; }
  get statuses(): string[] { return [...new Set(this.rows.map(row => String(row['Estado'] ?? '')).filter(Boolean))]; }
  get filtered(): DataRow[] { const query = this.search.toLocaleLowerCase('es'); const list = this.rows.filter(row => (!this.statusFilter || row['Estado'] === this.statusFilter) && (!query || Object.entries(row).some(([key, value]) => !key.startsWith('_') && this.format(value, key).toLocaleLowerCase('es').includes(query)))); return this.newestFirst ? [...list].reverse() : list; }
  get paged(): DataRow[] { return this.filtered.slice((this.page - 1) * this.pageSize, this.page * this.pageSize); }
  get pages(): number { return Math.max(1, Math.ceil(this.filtered.length / this.pageSize)); }
  get isFloor(): boolean { return this.active.slug === 'reservas' && this.view?.key === 'floor'; }

  constructor() {
    this.subscription = this.route.paramMap.subscribe(params => { const requested = modules.find(module => module.slug === params.get('module')); this.active = requested && this.has(requested.roles) ? requested : this.allowed[0] ?? modules[0]; this.view = this.views[0]; this.resetFilter(); void this.load(); void this.loadLookups(); });
    this.poll = setInterval(() => { if (!document.hidden && !this.form && !this.detailTitle && !this.busy && !this.confirmation) void this.load(true); }, 30000);
    if (keycloak.authenticated && !this.auth.profile()) void this.auth.loadProfile().then(() => this.detector.markForCheck());
  }
  ngOnDestroy(): void { this.subscription.unsubscribe(); clearInterval(this.poll); this.generation++; }
  has(roles: string[]): boolean { return roles.some(role => this.roles.includes(role)); }
  login(): void { this.auth.require(() => { const first = this.allowed[0]; if (first) void this.router.navigate(['/gestion', this.allowed.includes(this.active) ? this.active.slug : first.slug]).then(() => { this.view = this.views[0]; void this.load(); void this.loadLookups(); }); this.detector.markForCheck(); }); }
  logout(): void { this.auth.logout(); }
  selectView(view: ViewDef): void { this.view = view; this.resetFilter(); this.notice = ''; this.rows = []; this.floor = []; void this.load(); }
  resetFilter(): void { this.search = ''; this.statusFilter = ''; this.page = 1; }

  async load(silent = false): Promise<void> {
    if (!this.authenticated) return; const generation = ++this.generation; if (!silent) this.loading = true; this.error = '';
    try {
      if (this.active.slug === 'resumen') { const data = await firstValueFrom(this.api.get<DataRow>('/admin/dashboard')); if (generation === this.generation) this.dashboard = data; }
      else if (this.view) {
        if (this.isFloor) { const data = await firstValueFrom(this.api.get<FloorTable[]>(this.view.path)); if (generation === this.generation) this.floor = data; }
        else {
          const data = await firstValueFrom(this.api.get<DataRow[]>(this.view.path));
          if (generation === this.generation) { this.rows = this.normalize(data); if (this.active.slug === 'reservas' && this.view.key === 'reservations') this.reservations = data; }
        }
      }
      if (generation === this.generation) { this.updated = new Date(); this.page = Math.min(this.page, this.pages); }
    } catch (error) { if (generation === this.generation) this.error = errorMessage(error); }
    finally { if (generation === this.generation) this.loading = false; this.detector.markForCheck(); }
  }
  /** Ordena por su identificador y numera 1, 2, 3… sin saltos. */
  private normalize(data: DataRow[]): DataRow[] {
    const view = this.view?.key, scope = this.active.slug;
    let rows = data;
    if (scope === 'ventas' && view === 'orders') rows = data.map(row => ({ _id: row['id'], _n: row['number'], 'Cliente': row['customerName'], 'Atención': row['channel'], 'Mesa': row['tableCode'] ?? '', 'Estado': row['status'], 'Total': row['total'], 'Falta pagar': row['balance'], 'Fecha': row['createdAt'], _raw: row }));
    else if (scope === 'ventas' && view === 'products') rows = data.map(row => ({ _id: row['id'], 'Producto': row['name'], 'Categoría': row['category'], 'Precio': row['price'], 'Se puede pedir': row['available'], _raw: row }));
    else { const key = this.view?.idKey ?? 'ID'; rows = data.map(row => { const { [key]: id, ...rest } = row; return id === undefined ? { ...row } : { _id: id, ...rest }; }); }
    const numeric = rows.every(row => row['_id'] !== undefined && Number.isFinite(Number(row['_id'])));
    const ordered = numeric ? [...rows].sort((a, b) => Number(a['_n'] ?? a['_id']) - Number(b['_n'] ?? b['_id'])) : rows;
    return ordered.map((row, index) => ({ 'N°': row['_n'] ?? index + 1, ...row }));
  }
  async loadLookups(): Promise<void> {
    if (!this.authenticated) return;
    try {
      const [choices, options] = await Promise.all([firstValueFrom(this.api.get<Record<string, Choice[]>>('/admin/lookups')), firstValueFrom(this.api.get<DataRow[]>('/public/options'))]);
      choices['productOptions'] = options.map(item => ({ id: item['id'], label: item['name'] + (Number(item['price']) ? ' · +' + money(item['price']) : ''), productId: item['productId'] }));
      choices['orders'] = choices['orders']?.map(item => ({ ...item, label: item.label + ' · falta ' + money(Number(item['total']) - Number(item['paid'])) })) ?? [];
      this.lookups = { ...this.lookups, ...choices };
    } catch (error) { this.error = errorMessage(error); } finally { this.detector.markForCheck(); }
  }
  format(value: any, key = ''): string {
    if (value === null || value === undefined || value === '') return '—';
    if (typeof value === 'boolean') return value ? 'Sí' : 'No';
    if (typeof value === 'number') return /total|saldo|precio|monto|costo|tarifa|fondo|contado|esperado|diferencia|importe|igv|consumo|propina|devuelto|descuento|pagar|pendiente|recargo/i.test(key) ? money(value) : new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 }).format(value);
    if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}(T|$)/.test(value)) { const date = new Date(value.length === 10 ? value + 'T12:00:00-05:00' : value); return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', ...(value.length > 10 ? { timeStyle: 'short' as const } : {}), timeZone: 'America/Lima' }).format(date); }
    if (key === 'Roles') return String(value).split(', ').map(role => labels[role] ?? role).join(', ');
    return labels[String(value)] ?? String(value);
  }
  rowId(row: DataRow): number { return Number(row['_id']); }
  statusClass(value: any): string { const status = String(value); return ['COMPLETED', 'DELIVERED', 'READY', 'Disponible', 'CLOSED', 'RESOLVED', 'PRESENT', 'LIBRE', 'ARRIVED'].includes(status) ? 'good' : ['CANCELLED', 'NO_SHOW', 'Vencido', 'ABSENT', 'Agotado', 'OCUPADA'].includes(status) ? 'bad' : ['PARTIAL', 'Stock bajo', 'Por vencer', 'LATE', 'Ocupada', 'RESERVADA', 'Reservada', 'PREPARING'].includes(status) ? 'warn' : 'neutral'; }
  open(spec: FormSpec, initial: DataRow = {}): void { this.formError = ''; this.form = { ...spec, initial: { requestKey: crypto.randomUUID(), ...spec.initial, ...initial } }; void this.loadLookups(); }
  create(): void { if (!this.view?.form || !this.canCreate) return; this.open(this.view.form, this.isFloor ? { tableCode: this.nextTableCode() } : {}); }
  /** Siguiente código libre: si existen M1…M12, propone M13. */
  nextTableCode(): string { const numbers = this.floor.map(table => /^M(\d+)$/i.exec(table.code)).filter(match => !!match).map(match => Number(match![1])); return 'M' + (Math.max(0, ...numbers) + 1); }
  /** "13" o "m13" se muestran y guardan como "M13". */
  private tableCode(value: any): string { const code = String(value ?? '').trim().toUpperCase().replace(/\s+/g, ''); return /^\d+$/.test(code) ? 'M' + Number(code) : code; }
  async save(payload: DataRow): Promise<void> {
    if (!this.form || this.busy) return; const spec = this.form; this.busy = true; this.formError = '';
    try {
      if (typeof payload['tableCode'] === 'string') payload['tableCode'] = this.tableCode(payload['tableCode']);
      let path = spec.path; if (path.includes('{employeeId}')) { path = path.replace('{employeeId}', String(payload['employeeId'])); delete payload['employeeId']; }
      const response = await firstValueFrom(this.api.request<DataRow>(spec.method ?? 'POST', path, payload)); this.form = undefined; this.notice = response['message'] ?? 'Listo, se guardó.';
      if (response['documents']?.length) { for (const document of response['documents']) await this.download(document['id']); }
      await Promise.all([this.load(), this.loadLookups()]);
    } catch (error) { this.formError = errorMessage(error); } finally { this.busy = false; this.detector.markForCheck(); }
  }
  async mutate(method: 'POST' | 'PATCH' | 'PUT' | 'DELETE', path: string, data: DataRow = {}): Promise<void> {
    if (this.busy) return; this.busy = true; this.error = '';
    try { const result = await firstValueFrom(this.api.request<DataRow>(method, path, data)); this.notice = result['message'] ?? 'Listo.'; await Promise.all([this.load(), this.loadLookups()]); }
    catch (error) { this.error = errorMessage(error); } finally { this.busy = false; this.confirmation = undefined; this.detector.markForCheck(); }
  }
  confirm(message: string, run: () => Promise<void>): void { this.confirmation = { message, run }; }
  async download(id: number): Promise<void> { try { const blob = await firstValueFrom(this.api.pdf('/admin/cash/documents/' + id + '/pdf')); downloadPdf(blob, 'Inti-Crunch-' + id + '.pdf'); } catch (error) { this.error = errorMessage(error); this.detector.markForCheck(); } }
  async show(title: string, path: string): Promise<void> { this.error = ''; try { const rows = await firstValueFrom(this.api.get<DataRow[]>(path)); this.details = [{ title, rows }]; this.detailTitle = title; } catch (error) { this.error = errorMessage(error); } finally { this.detector.markForCheck(); } }
  detailColumns(rows: DataRow[]): string[] { return rows.length ? Object.keys(rows[0]).filter(key => !key.startsWith('_')) : []; }
  private fill(path: string, row: DataRow): string { return path.replace('{id}', String(this.rowId(row))).replace(/\{(_\w+)\}/g, (_, key) => encodeURIComponent(String(row[key] ?? ''))); }

  actions(row: DataRow): RowAction[] {
    const view = this.view?.key, scope = this.active.slug, status = row['Estado'], result: RowAction[] = [];
    const add = (key: string, label: string, danger = false, main = false) => result.push({ key, label, danger, main });
    if (scope === 'ventas' && view === 'orders') {
      add('detail', 'Ver');
      if (['RECEIVED', 'PREPARING', 'READY', 'DISPATCHED'].includes(status)) {
        if (status === 'RECEIVED') add('advance', 'Empezar a preparar', false, true); else if (status === 'PREPARING') add('advance', 'Marcar listo', false, true); else if (row['_raw']['channel'] !== 'DELIVERY') add('advance', 'Entregar', false, true);
        if (Number(row['Falta pagar']) > 0 && this.has(ROLE.cash)) add('pay', 'Cobrar', false, true);
        if (status === 'RECEIVED' && this.has(ROLE.manage) && Number(row['_raw']['paid']) === 0) add('discount', 'Descuento');
        if (status !== 'DISPATCHED' && this.has(ROLE.ops)) add('cancel', 'Cancelar', true);
      }
    }
    if (scope === 'caja') { if (view === 'sessions' && status === 'OPEN') add('close', 'Cerrar caja', false, true); if (view === 'payments') { add('pdf', 'Boleta PDF'); if (this.has(ROLE.manage) && Number(row['Monto']) > Number(row['Devuelto'])) add('refund', 'Devolver'); } if (view === 'documents') add('pdf', 'Descargar PDF'); }
    if (scope === 'inventario') { if (['recipes', 'movements'].includes(view ?? '')) add('detail', 'Ver detalle'); if (view === 'ingredients') add('count', 'Contar', false, true); if (view === 'lots') add('discard', 'Desechar', true); }
    if (scope === 'compras') { if (['orders', 'requests', 'receipts'].includes(view ?? '')) add('detail', 'Ver detalle'); if (view === 'requests' && status === 'PENDING') add('toOrder', 'Hacer orden', false, true); if (view === 'orders' && ['PENDING', 'PARTIAL'].includes(status)) add('receive', 'Recibir', false, true); if (view === 'orders' && status === 'PENDING') add('cancel', 'Cancelar', true); if (view === 'invoices' && Number(row['Pendiente']) > 0) add('payInvoice', 'Registrar pago', false, true); }
    if (scope === 'despacho') {
      if (view === 'kitchen' && ['RECEIVED', 'PREPARING'].includes(status)) add('advance', status === 'RECEIVED' ? 'Empezar' : '¡Listo!', false, true);
      if (view === 'dispatches') { add('detail', 'Seguimiento'); if (this.has([...ROLE.ops, 'REPARTIDOR']) && !['DELIVERED', 'CANCELLED'].includes(status)) { if (row['Tipo'] === 'DELIVERY' && status !== 'ON_ROUTE' && this.has(ROLE.ops)) add('assign', row['Repartidor'] ? 'Cambiar repartidor' : 'Asignar repartidor'); if (status === 'READY' || status === 'ON_ROUTE') add('advance', row['Tipo'] === 'DELIVERY' && status === 'READY' ? 'Salió a repartir' : 'Entregado', false, true); } }
      if (view === 'incidents' && status === 'OPEN') add('resolve', 'Resolver', false, true);
    }
    if (scope === 'clientes') { if (view === 'customers') add('detail', 'Historial'); if (view === 'promotions') { if (this.has(ROLE.manage)) { add('conditions', 'Condiciones'); add('toggle', row['Activa'] ? 'Pausar' : 'Activar'); } if (row['Activa']) add('redeem', 'Aplicar a un pedido'); } }
    if (scope === 'personal') { if (view === 'employees' && row['Puede ingresar'] === 'Sí') add('password', 'Cambiar contraseña'); if (view === 'shifts') { add('detail', 'Ver equipo'); if (['PLANNED', 'ACTIVE'].includes(status)) { add('attendance', 'Asistencia', false, true); add('close', 'Cerrar turno'); } } }
    if (scope === 'reservas') { if (view === 'reservations') { add('detail', 'Historial'); if (status === 'BOOKED') { add('arrive', 'Llegaron', false, true); add('cancel', 'Cancelar / no vino', true); } if (status === 'ARRIVED') add('finish', 'Ya se fueron', false, true); } if (view === 'visits') { add('detail', 'Pedidos'); if (status === 'OPEN') { add('attach', 'Agregar pedido'); add('close', 'Liberar mesas', false, true); } } }
    const crud = this.view?.crud, viewRoles = this.view?.createRoles ?? this.view?.roles ?? this.active.roles;
    if (crud?.editPath && this.has(crud.editRoles ?? viewRoles) && (crud.canEdit?.(row) ?? true)) add('edit', 'Editar');
    if (crud?.deletePath && this.has(crud.deleteRoles ?? viewRoles) && (crud.canDelete?.(row) ?? true)) add('delete', crud.deleteLabel ?? 'Eliminar', true);
    return result;
  }
  async act(action: string, row: DataRow): Promise<void> { try { await this.performAction(action, row); } catch (error) { this.error = errorMessage(error); } finally { this.detector.markForCheck(); } }
  private async performAction(action: string, row: DataRow): Promise<void> {
    if (this.busy) return; const id = this.rowId(row), scope = this.active.slug, view = this.view?.key, status = row['Estado'], n = row['N°'];
    if (action === 'edit') { await this.edit(row); return; }
    if (action === 'delete') { const crud = this.view!.crud!; this.confirm(crud.deleteQuestion?.(row) ?? '¿Eliminar este registro?', () => this.mutate('DELETE', this.fill(crud.deletePath!, row))); return; }
    if (action === 'pdf') { if (view === 'documents') await this.download(id); else { const docs = await firstValueFrom(this.api.get<DataRow[]>('/admin/cash/payments/' + id + '/documents')); if (!docs.length) this.notice = 'Este pago antiguo no tiene comprobante.'; for (const doc of docs) await this.download(doc['id']); } return; }
    if (action === 'detail') { await this.openDetail(row); return; }
    if (action === 'pay') { this.open({ ...paymentForm, lineInitial: [{ method: 'CASH', amount: row['Falta pagar'], tip: 0 }] }, { orderId: id, cashSessionId: this.lookups['sessions']?.[0]?.id }); return; }
    if (action === 'discount') { this.open({ title: 'Descuento · pedido N° ' + n, path: '/orders/' + id + '/discount', fields: [numberField('amount', 'Descuento (S/)'), noteField('reason', 'Motivo', true)] }); return; }
    if (action === 'advance') {
      if (scope === 'ventas') { const next: Record<string, string> = { RECEIVED: 'PREPARING', PREPARING: 'READY', READY: 'COMPLETED', DISPATCHED: 'COMPLETED' }; await this.mutate('PATCH', '/orders/' + id + '/status', { status: next[status] }); }
      else if (view === 'kitchen') await this.mutate('PATCH', '/admin/delivery/kitchen/' + id, { status: status === 'RECEIVED' ? 'PREPARING' : 'READY' });
      else await this.mutate('PATCH', '/admin/delivery/dispatches/' + id, { status: status === 'READY' && row['Tipo'] === 'DELIVERY' ? 'ON_ROUTE' : 'DELIVERED' });
      return;
    }
    if (action === 'cancel' && scope === 'ventas') { this.confirm('¿Cancelar el pedido N° ' + n + '?', () => this.mutate('PATCH', '/orders/' + id + '/cancel')); return; }
    if (action === 'cancel' && scope === 'compras') { this.confirm('¿Cancelar la orden de compra N° ' + n + '?', () => this.mutate('PATCH', '/admin/purchases/orders/' + id + '/cancel')); return; }
    if (action === 'close' && scope === 'caja') { this.open({ title: 'Cerrar caja N° ' + n, description: 'Debería haber ' + money(row['Esperado']) + ' en efectivo. Cuenta el dinero y escribe cuánto hay.', path: '/admin/cash/sessions/' + id + '/close', method: 'PATCH', fields: [numberField('closingAmount', 'Efectivo contado (S/)')] }); return; }
    if (action === 'refund') {
      const info = await firstValueFrom(this.api.get<DataRow>('/admin/cash/payments/' + id + '/refundable'));
      const methods: DataRow[] = info['methods'] ?? []; if (!methods.length) { this.notice = 'Este pago ya fue devuelto por completo.'; return; }
      this.open({ title: 'Devolver dinero · pago N° ' + n, description: 'Puedes devolver hasta ' + money(info['total']) + '. ' + methods.map(m => m['label'] + ': ' + money(m['available'])).join(' · '), path: '/admin/cash/payments/' + id + '/refund', submitLabel: 'Devolver', fields: [{ key: 'method', label: '¿Por dónde se devuelve?', type: 'select', required: true, options: methods.map(m => ({ id: m['method'], label: m['label'] + ' (hasta ' + money(m['available']) + ')' })), initial: methods[0]['method'] }, { ...numberField('amount', 'Monto a devolver (S/)'), initial: Number(methods[0]['available']), min: 0.01 }, noteField('reason', 'Motivo', true)] }, { cashSessionId: info['cashSessionId'] || undefined });
      return;
    }
    if (action === 'count') { this.open({ title: 'Contar · ' + row['Insumo'], description: 'Escribe cuánto hay realmente (en ' + row['Unidad'] + '). El sistema corrige la diferencia.', path: '/admin/inventory/ingredients/' + id + '/count', submitLabel: 'Guardar conteo', fields: [{ ...selectField('warehouseId', 'Almacén', 'warehouses'), initial: 1 }, { ...numberField('quantity', 'Cantidad que hay (' + row['Unidad'] + ')', true, '0.001'), initial: row['Hay'] }, { ...dateField('expiresOn', 'Si sobró, ¿cuándo vence? (opcional)', 'date'), required: false }, noteField()] }); return; }
    if (action === 'discard') { this.confirm('¿Desechar ' + this.format(row['Cantidad']) + ' ' + row['Unidad'] + ' de ' + row['Insumo'] + '? Se registrará como merma.', () => this.mutate('POST', '/admin/inventory/lots/' + id + '/discard')); return; }
    if (action === 'receive') { await this.receive(id, n); return; }
    if (action === 'toOrder') { const lines = await firstValueFrom(this.api.get<DataRow[]>('/admin/purchases/requests/' + id + '/lines')); this.open({ ...purchaseOrderForm, lineInitial: lines.map(line => ({ ingredientId: line['ID'], quantity: line['Cantidad'] })) }, { requestId: id }); return; }
    if (action === 'payInvoice') { this.open({ title: 'Pagar al proveedor', description: 'Falta pagar ' + money(row['Pendiente']), path: '/admin/purchases/invoices/' + id + '/payments', fields: [{ ...numberField('amount', 'Monto (S/)'), initial: row['Pendiente'] }, textField('reference', 'N° de operación (opcional)', false)] }); return; }
    if (action === 'assign') { this.open({ title: 'Asignar repartidor', path: '/admin/delivery/dispatches/' + id + '/assign', fields: [selectField('employeeId', 'Repartidor disponible', 'couriers')] }); return; }
    if (action === 'resolve') { this.open({ title: 'Resolver problema', path: '/admin/delivery/incidents/' + id, method: 'PATCH', fields: [noteField('resolution', '¿Cómo se solucionó?', true)] }); return; }
    if (action === 'toggle') { await this.mutate('PATCH', '/admin/promotions/' + id + '/status', { active: !row['Activa'] }); return; }
    if (action === 'conditions') { const current = await firstValueFrom(this.api.get<DataRow>('/admin/promotions/' + id + '/products')); this.open({ title: 'Condiciones · ' + row['Promoción'], description: 'Si no eliges productos, aplica a toda la carta. Un pedido usa una sola promoción.', path: '/admin/promotions/' + id + '/products', method: 'PUT', fields: [multiField('productIds', 'Productos incluidos', 'products', false), { ...numberField('minimumOrder', 'Consumo mínimo (S/)'), initial: 0 }, numberField('maximumUses', 'Máximo de usos (opcional)', false, '1')] }, current); return; }
    if (action === 'redeem') { this.open({ title: 'Aplicar ' + row['Promoción'], path: '/admin/promotions/redeem', fields: [selectField('orderId', 'Pedido sin cobrar', 'orders')] }, { promotionId: id }); return; }
    if (action === 'password') { this.open({ title: 'Cambiar contraseña · ' + row['Colaborador'], description: 'Podrá entrar con su DNI y la nueva contraseña.', path: '/admin/staff/' + id + '/password', method: 'PUT', submitLabel: 'Cambiar contraseña', fields: [{ key: 'password', label: 'Nueva contraseña', type: 'password', required: true, hint: 'Mínimo 8 caracteres, con letras y números.' }, { key: 'confirmPassword', label: 'Repite la contraseña', type: 'password', required: true }] }); return; }
    if (action === 'attendance') { await this.attendance(id, row['Nombre']); return; }
    if (action === 'close' && scope === 'personal') { this.open({ title: 'Cerrar turno · ' + row['Nombre'], path: '/admin/staff/shifts/' + id + '/status', method: 'PATCH', fields: [enumField('status', '¿Qué hacemos?', ['CLOSED', 'CANCELLED'], 'CLOSED')] }); return; }
    if (action === 'arrive') { this.confirm('¿Llegaron los clientes de ' + row['Cliente'] + '? Sus mesas se marcarán como ocupadas.', () => this.mutate('PATCH', '/admin/reservations/' + id, { status: 'ARRIVED' })); return; }
    if (action === 'finish') { this.confirm('¿Los clientes ya se fueron? Las mesas quedarán libres.', () => this.mutate('PATCH', '/admin/reservations/' + id, { status: 'COMPLETED' })); return; }
    if (action === 'cancel' && scope === 'reservas') { this.open({ title: 'Cancelar reserva de ' + row['Cliente'], path: '/admin/reservations/' + id, method: 'PATCH', fields: [enumField('status', '¿Qué pasó?', ['CANCELLED', 'NO_SHOW'], 'CANCELLED'), noteField('note', 'Motivo')] }); return; }
    if (action === 'attach') { this.open({ title: 'Agregar pedido a la mesa', path: '/admin/reservations/visits/' + id + '/orders', fields: [selectField('orderId', 'Pedido para comer en el local', 'orders')] }); return; }
    if (action === 'close' && scope === 'reservas') { this.confirm('¿Los clientes ya se fueron? Las mesas quedarán libres.', () => this.mutate('PATCH', '/admin/reservations/visits/' + id + '/close')); }
  }
  private async edit(row: DataRow): Promise<void> {
    const view = this.view; if (!view?.form || !view.crud?.editPath) return; const crud = view.crud;
    let initial: DataRow = crud.toInitial?.(row, this.lookups) ?? {}; let lineInitial: DataRow[] | undefined;
    if (crud.loadPath) { const loaded = await firstValueFrom(this.api.get<DataRow>(this.fill(crud.loadPath, row))); initial = { ...initial, ...loaded }; if (view.form.lineKey && Array.isArray(loaded[view.form.lineKey])) lineInitial = loaded[view.form.lineKey]; }
    if (crud.loadLines) { const lines = await firstValueFrom(this.api.get<DataRow[]>(this.fill(crud.loadLines.path, row))); lineInitial = lines.map(crud.loadLines.map); }
    const fields: Field[] = crud.editFields ?? view.form.fields;
    for (const field of fields) { const value = initial[field.key]; if (value && field.type === 'datetime-local') initial[field.key] = this.localInput(new Date(value)); if (value && field.type === 'date') initial[field.key] = String(value).slice(0, 10); }
    const name = row['Producto'] ?? row['Insumo'] ?? row['Proveedor'] ?? row['Cliente'] ?? row['Promoción'] ?? row['Colaborador'] ?? row['Repartidor'] ?? row['Zona'] ?? row['Almacén'] ?? row['Opción'] ?? row['Nombre'] ?? ('N° ' + row['N°']);
    const spec: FormSpec = { ...view.form, title: crud.editTitle?.(row) ?? 'Editar · ' + name, path: this.fill(crud.editPath!, row), method: crud.editMethod ?? 'PATCH', fields, submitLabel: 'Guardar cambios', lineInitial: lineInitial ?? view.form.lineInitial, availability: view.form.availability ? { ...view.form.availability, reservationId: this.rowId(row) } : undefined };
    this.open(spec, initial);
  }
  private async openDetail(row: DataRow): Promise<void> {
    const id = this.rowId(row), scope = this.active.slug, view = this.view?.key, n = row['N°'];
    if (scope === 'ventas' && view === 'orders') {
      const history = await firstValueFrom(this.api.get<DataRow[]>('/orders/' + id + '/history')); const raw = row['_raw'];
      this.details = [{ title: 'Productos', rows: raw['lines'].map((line: DataRow) => ({ 'Producto': line['product']['name'], 'Cantidad': line['quantity'], 'Precio': line['unitPrice'], 'Subtotal': line['subtotal'], 'Opciones': line['options'].map((item: DataRow) => item['name']).join(', '), 'Notas': line['notes'] })) }, { title: 'Cuentas', rows: [{ 'Subtotal': raw['subtotal'], 'Descuento': raw['discount'], 'Envío': raw['deliveryFee'], 'Total': raw['total'], 'Pagado': raw['paid'], 'Falta pagar': raw['balance'] }] }, { title: 'Historial', rows: history }];
      this.detailTitle = 'Pedido N° ' + n; this.detector.markForCheck(); return;
    }
    const paths: Record<string, string> = { 'inventario:recipes': '/admin/inventory/recipes/' + id + '/lines', 'inventario:movements': '/admin/inventory/movements/' + id + '/lines', 'compras:orders': '/admin/purchases/orders/' + id + '/lines', 'compras:requests': '/admin/purchases/requests/' + id + '/lines', 'compras:receipts': '/admin/purchases/receipts/' + id + '/lines', 'despacho:dispatches': '/admin/delivery/dispatches/' + id + '/events', 'clientes:customers': '/admin/customers/' + id + '/orders', 'personal:shifts': '/admin/staff/shifts/' + id + '/employees', 'reservas:reservations': '/admin/reservations/' + id + '/events', 'reservas:visits': '/admin/reservations/visits/' + id + '/orders' };
    const path = paths[scope + ':' + view]; if (path) await this.show('Detalle · N° ' + n, path);
  }
  private localInput(date: Date): string { const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000); return local.toISOString().slice(0, 16); }
  private async receive(id: number, n: any): Promise<void> {
    const pending = await firstValueFrom(this.api.get<DataRow[]>('/admin/purchases/orders/' + id + '/lines'));
    const open = pending.filter(line => Number(line['Pendiente']) > 0);
    this.lookups['purchaseLines'] = open.map(line => ({ id: line['ID'], label: line['Insumo'] + ' · faltan ' + line['Pendiente'] }));
    this.open({ title: 'Recibir compra N° ' + n, path: '/admin/purchases/orders/' + id + '/receive', method: 'PATCH', submitLabel: 'Guardar recepción', description: 'Escribe lo que llegó en buen estado. Si algo llegó mal, anótalo en "Con problemas": no entra al inventario.', fields: [{ ...selectField('warehouseId', '¿Dónde se guarda?', 'warehouses'), initial: 1 }, textField('documentNumber', 'Guía o factura', false), noteField()], lineFields: [selectField('lineId', 'Insumo', 'purchaseLines'), numberField('quantity', 'Llegó bien', true, '0.001'), { ...numberField('observedQuantity', 'Con problemas', false, '0.001'), initial: 0 }, { ...dateField('expiresOn', 'Vence', 'date'), required: false }, noteField()], lineInitial: open.map(line => ({ lineId: line['ID'], quantity: line['Pendiente'], observedQuantity: 0 })) });
  }
  private async attendance(id: number, name: string): Promise<void> {
    const employees = await firstValueFrom(this.api.get<DataRow[]>('/admin/staff/shifts/' + id + '/employees'));
    this.lookups['shiftEmployees'] = employees.map(employee => ({ id: employee['ID'], label: employee['Colaborador'] + ' · ' + this.format(employee['Estado']) }));
    this.open({ title: 'Asistencia · ' + name, path: '/admin/staff/shifts/' + id + '/employees/{employeeId}/attendance', method: 'PATCH', submitLabel: 'Marcar', fields: [selectField('employeeId', '¿Quién?', 'shiftEmployees'), enumField('action', '¿Qué marca?', ['IN', 'OUT', 'ABSENT', 'NOTE'], 'IN'), noteField('incident', 'Motivo o comentario')] });
  }

  // ---------- Plano de mesas ----------
  floorAreas(): string[] { return [...new Set(this.floor.map(table => table.area || 'Salón'))]; }
  tablesIn(area: string): FloorTable[] { return this.floor.filter(table => (table.area || 'Salón') === area); }
  floorCount(state: string): number { return this.floor.filter(table => table.state === state).length; }
  readyOrders(table: FloorTable): number { return table.orders.filter(order => order['status'] === 'READY').length; }
  seat(table: FloorTable): void { const spec = this.active.views.find(view => view.key === 'visits')!.form!; this.open(spec, { tableIds: [table.id], partySize: Math.min(2, table.capacity) }); }
  takeOrder(table: FloorTable): void { this.open({ ...saleForm, title: 'Pedido para la mesa ' + table.code }, { channel: 'DINE_IN', tableId: table.id, customerName: table.visit?.['guest'] ?? '' }); }
  freeTable(table: FloorTable): void { this.confirm('¿Los clientes de la mesa ' + table.code + ' ya se fueron? La mesa quedará libre.', () => this.mutate('PATCH', '/admin/reservations/tables/' + table.id + '/free')); }
  arrived(table: FloorTable): void { const reservation = table.reservation; if (reservation) this.confirm('¿Llegó ' + reservation['guest'] + '? Se ocuparán sus mesas.', () => this.mutate('PATCH', '/admin/reservations/' + reservation['id'], { status: 'ARRIVED' })); }
  editTable(table: FloorTable): void { const spec = this.view!.form!; this.open({ ...spec, title: 'Editar mesa ' + table.code, path: '/admin/reservations/tables/' + table.id, method: 'PATCH', submitLabel: 'Guardar cambios' }, { tableCode: table.code, capacity: table.capacity, area: table.area }); }
  deleteTable(table: FloorTable): void { this.confirm('¿Quitar la mesa ' + table.code + '?', () => this.mutate('DELETE', '/admin/reservations/tables/' + table.id)); }
  serve(order: DataRow): void { if (Number(order['balance']) > 0) { this.payOrder(order); return; } void this.mutate('PATCH', '/orders/' + order['id'] + '/status', { status: 'COMPLETED' }); }
  payOrder(order: DataRow): void { this.open({ ...paymentForm, lineInitial: [{ method: 'CASH', amount: order['balance'], tip: 0 }] }, { orderId: order['id'], cashSessionId: this.lookups['sessions']?.[0]?.id }); }
  advanceOrder(order: DataRow): void { void this.mutate('PATCH', '/orders/' + order['id'] + '/status', { status: order['status'] === 'RECEIVED' ? 'PREPARING' : 'READY' }); }
  timeOf(value: any): string { const date = this.asDate(value); return date ? new Intl.DateTimeFormat('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true, timeZone: 'America/Lima' }).format(date) : ''; }

  // ---------- Resumen ----------
  channelCount(code: string): number { return Number(this.dashboard?.['canales']?.find((channel: DataRow) => channel['canal'] === code)?.['pedidos'] ?? 0); }
  channelPercent(code: string): number { return 100 * this.channelCount(code) / Math.max(1, Number(this.dashboard?.['metricas']?.['pedidosHoy'] ?? 0)); }
  monthBars(): { label: string; total: number; percent: number }[] { const series: DataRow[] = this.dashboard?.['ganancias']?.['porMes'] ?? []; const max = Math.max(1, ...series.map(item => Number(item['total']))); return series.map(item => { const [year, month] = String(item['mes']).split('-').map(Number); return { label: new Intl.DateTimeFormat('es-PE', { month: 'short' }).format(new Date(year, month - 1, 1)).replace('.', ''), total: Number(item['total']), percent: Math.max(2, 100 * Math.max(0, Number(item['total'])) / max) }; }); }

  // ---------- Calendario de reservas ----------
  get weekDays(): Date[] { const first = new Date(this.calendarDate.getFullYear(), this.calendarDate.getMonth(), this.calendarDate.getDate()); first.setDate(first.getDate() - ((first.getDay() + 6) % 7)); return Array.from({ length: 7 }, (_, index) => new Date(first.getFullYear(), first.getMonth(), first.getDate() + index)); }
  get weekHours(): number[] { return Array.from({ length: 16 }, (_, index) => index + 8); }
  get weekTitle(): string { const days = this.weekDays, first = days[0], last = days[6], sameMonth = first.getMonth() === last.getMonth(); const start = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: sameMonth ? undefined : 'short' }).format(first); const end = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }).format(last); return `${start} – ${end}`; }
  changeWeek(amount: number): void { const next = new Date(this.calendarDate); next.setDate(next.getDate() + amount * 7); this.calendarDate = next; }
  goToCurrentWeek(): void { this.calendarDate = new Date(); }
  sameDay(a: Date, b: Date): boolean { return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate(); }
  dayLabel(day: Date): string { return new Intl.DateTimeFormat('es-PE', { weekday: 'short' }).format(day).replace('.', ''); }
  dateLabel(day: Date): string { return new Intl.DateTimeFormat('es-PE', { day: 'numeric' }).format(day); }
  asDate(value: unknown): Date | undefined { if (!value) return; const date = new Date(String(value)); return Number.isNaN(date.getTime()) ? undefined : date; }
  activeReservations(): DataRow[] { return this.reservations.filter(row => !['CANCELLED', 'NO_SHOW', 'COMPLETED'].includes(String(row['Estado'] ?? '').toUpperCase())); }
  reservationsOn(day: Date): DataRow[] { return this.activeReservations().filter(row => { const start = this.asDate(row['Inicio']); return !!start && this.sameDay(start, day); }).sort((a, b) => (this.asDate(a['Inicio'])?.getTime() ?? 0) - (this.asDate(b['Inicio'])?.getTime() ?? 0)); }
  eventTop(row: DataRow): number { const start = this.asDate(row['Inicio']); return start ? Math.max(0, (start.getHours() - 8) * 58 + start.getMinutes() * .966) : 0; }
  eventHeight(row: DataRow): number { const start = this.asDate(row['Inicio']), end = this.asDate(row['Fin']); return start && end ? Math.max(42, Math.min(230, (end.getTime() - start.getTime()) / 60000 * .966)) : 104; }
  eventTime(row: DataRow): string { const start = this.asDate(row['Inicio']), end = this.asDate(row['Fin']); if (!start) return ''; const fmt = new Intl.DateTimeFormat('es-PE', { hour: 'numeric', minute: '2-digit', hour12: true }); return `${fmt.format(start)}${end ? ' – ' + fmt.format(end) : ''}`; }
  eventStatus(row: DataRow): string { return String(row['Estado'] ?? '').toUpperCase() === 'ARRIVED' ? 'arrived' : ''; }
  createReservationAt(day: Date, hour: number): void { if (!this.view?.form || !this.canCreate) return; const start = new Date(day.getFullYear(), day.getMonth(), day.getDate(), hour); const end = new Date(start.getTime() + 2 * 60 * 60 * 1000); this.open(this.view.form, { startsAt: this.localInput(start), endsAt: this.localInput(end) }); }
}
