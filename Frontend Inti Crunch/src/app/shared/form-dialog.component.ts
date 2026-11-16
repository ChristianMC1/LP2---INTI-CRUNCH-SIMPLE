import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { Choice, DataRow, Field, FormSpec } from '../core/ui';
import { ApiService } from '../core/api/api.service';

type TableChoice = { id: number; code: string; capacity: number; area?: string; available: boolean };

@Component({
  selector: 'ic-form-dialog', standalone: true, imports: [CommonModule],
  template: `
    <div class="ic-overlay" (click)="!busy && closed.emit()">
      <section class="ic-dialog" role="dialog" aria-modal="true" [attr.aria-label]="spec.title" (click)="$event.stopPropagation()">
        <header class="ic-dialog-head"><div><span class="ic-kicker">INTI CRUNCH · GESTIÓN</span><h2>{{spec.title}}</h2><p *ngIf="spec.description">{{spec.description}}</p></div><button class="ic-close" type="button" aria-label="Cerrar formulario" [disabled]="busy" (click)="closed.emit()">×</button></header>
        <form (submit)="submit($event)">
          <div class="ic-form-grid"><ng-container *ngFor="let field of spec.fields"><ng-container *ngTemplateOutlet="fieldTemplate; context: {field:field, model:values, line:false}"></ng-container></ng-container></div>
          <section class="ic-lines-editor" *ngIf="spec.lineFields?.length"><h3>{{spec.lineKey==='methods'?'¿Cómo paga?':spec.lineKey==='employees'?'¿Quiénes trabajan?':'Detalle'}}</h3><p class="ic-help">Agrega una fila por cada elemento. Puedes quitar las que sobren.</p>
            <div class="ic-line-card" *ngFor="let row of lines; let index=index"><b>{{index+1}}.</b><button type="button" class="ic-text-button danger" (click)="lines.splice(index,1)">Quitar</button><div class="ic-form-grid"><ng-container *ngFor="let field of spec.lineFields"><ng-container *ngTemplateOutlet="fieldTemplate; context:{field:field,model:row,line:true}"></ng-container></ng-container></div></div>
            <button class="ic-secondary" type="button" (click)="addLine()">＋ Agregar otra fila</button>
          </section>
          <p *ngIf="error || localError" class="ic-alert error" role="alert">{{error || localError}}</p>
          <footer class="ic-dialog-footer"><span>Los campos con * son obligatorios.</span><button type="button" class="ic-secondary" [disabled]="busy" (click)="closed.emit()">Cancelar</button><button type="submit" class="ic-primary" [disabled]="busy || availabilityLoading">{{busy?'Guardando…':(spec.submitLabel||'Guardar')}}</button></footer>
        </form>
        <ng-template #fieldTemplate let-field="field" let-model="model" let-line="line">
          <div class="ic-field" [class.wide-field]="field.type==='tables'||field.type==='multi'&&!line" *ngIf="visible(field,model)"><span>{{field.label}}{{field.required?' *':''}}</span>
            <select *ngIf="field.type==='select'" [required]="field.required" (change)="selectValue(field,model,$any($event.target).value)"><option value="" [selected]="!model[field.key]">Elige una opción</option><option *ngFor="let choice of choices(field,model)" [value]="choice.id" [selected]="String(model[field.key])===String(choice.id)">{{choice.label}}</option></select>
            <div *ngIf="field.type==='multi'" class="ic-check-list"><label *ngFor="let choice of choices(field,model)"><input type="checkbox" [checked]="selected(model,field.key,choice.id)" (change)="toggle(field.key,model,choice.id,$any($event.target).checked)">{{choice.label}}</label><small *ngIf="!choices(field,model).length">No hay opciones disponibles.</small></div>
            <div *ngIf="field.type==='tables'" class="table-picker">
              <p class="ic-help" *ngIf="!tableOptions.length">{{availabilityMessage}}</p>
              <div class="table-picker-grid" *ngIf="tableOptions.length"><button type="button" *ngFor="let table of tableOptions" class="table-pick" [class.chosen]="selected(model,field.key,table.id)" [class.busy]="!table.available" [disabled]="!table.available" (click)="toggle(field.key,model,table.id,!selected(model,field.key,table.id))"><b>{{table.code}}</b><small>{{table.capacity}} personas</small><em>{{table.available?(selected(model,field.key,table.id)?'Elegida ✓':'Disponible'):'Ocupada'}}</em></button></div>
              <p class="table-picker-note" *ngIf="tableOptions.length" [class.ok]="seatsOk(model,field.key)">{{seatsMessage(model,field.key)}}</p>
            </div>
            <textarea *ngIf="field.type==='textarea'" [required]="field.required" maxlength="500" rows="3" [attr.placeholder]="field.placeholder ?? null" [value]="model[field.key] ?? ''" (input)="model[field.key]=$any($event.target).value"></textarea>
            <div *ngIf="field.type==='boolean'" class="yes-no"><button type="button" [class.on]="model[field.key]!==false" (click)="model[field.key]=true">Sí</button><button type="button" [class.on]="model[field.key]===false" (click)="model[field.key]=false">No</button></div>
            <input *ngIf="!['select','multi','textarea','boolean','tables'].includes(field.type ?? '')" [type]="field.type ?? 'text'" [required]="field.required" [attr.min]="field.min ?? (field.type==='number'?0:null)" [attr.max]="field.max ?? null" [attr.step]="field.step ?? (field.type==='number'?'0.01':null)" [attr.maxlength]="field.maxlength ?? 300" [attr.placeholder]="field.placeholder ?? null" [attr.autocomplete]="field.type==='password'?'new-password':null" [value]="model[field.key] ?? ''" (input)="setValue(field,model,$any($event.target).value)">
            <small *ngIf="field.hint">{{field.hint}}</small>
          </div>
        </ng-template>
      </section>
    </div>`
})
export class FormDialogComponent implements OnChanges, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly detector = inject(ChangeDetectorRef);
  private availabilityGeneration = 0;
  private availabilityTimer: ReturnType<typeof setTimeout> | undefined;
  readonly String = String;
  @Input({ required: true }) spec!: FormSpec;
  @Input() lookups: Record<string, Choice[]> = {};
  @Input() busy = false;
  @Input() error = '';
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<DataRow>();
  values: DataRow = {};
  lines: DataRow[] = [];
  localError = '';
  tableOptions: TableChoice[] = [];
  availabilityLoading = false;
  availabilityMessage = 'Primero elige la fecha y la hora para ver las mesas libres.';
  ngOnChanges(changes: any): void { if (changes['spec']) { this.values = { ...this.defaults(this.spec.fields), ...this.spec.initial }; this.lines = this.spec.lineInitial?.map(row => ({ ...row })) ?? (this.spec.lineFields?.length ? [this.defaults(this.spec.lineFields)] : []); this.localError = ''; this.tableOptions = []; void this.loadTables(++this.availabilityGeneration); } }
  ngOnDestroy(): void { if (this.availabilityTimer) clearTimeout(this.availabilityTimer); this.availabilityGeneration++; }
  defaults(fields: Field[]): DataRow { return Object.fromEntries(fields.filter(field => field.initial !== undefined).map(field => [field.key, field.initial])); }
  visible(field: Field, model: DataRow): boolean { return !field.when || field.when.values.includes(model[field.when.key]); }
  selected(model: DataRow, key: string, id: string | number): boolean { return (model[key] ?? []).some((value: any) => String(value) === String(id)); }
  choices(field: Field, model: DataRow): Choice[] { const choices = field.options ?? this.lookups[field.source ?? ''] ?? []; return field.source === 'productOptions' ? choices.filter(choice => choice['productId'] === Number(model['productId'])) : choices; }
  setValue(field: Field, model: DataRow, value: string): void {
    model[field.key] = value; const rule = this.spec.availability;
    if (!rule || ![rule.startField, rule.endField].includes(field.key)) return;
    if (field.key === rule.startField && value) { const start = new Date(value); if (Number.isFinite(start.getTime())) { const end = new Date(start.getTime() + 2 * 3600000); end.setMinutes(end.getMinutes() - end.getTimezoneOffset()); this.values[rule.endField] = end.toISOString().slice(0, 16); } }
    const generation = ++this.availabilityGeneration; if (this.availabilityTimer) clearTimeout(this.availabilityTimer);
    this.availabilityLoading = true; this.availabilityMessage = 'Buscando mesas libres…';
    this.availabilityTimer = setTimeout(() => void this.loadTables(generation), 300);
  }
  seatsNeeded(): number { const key = this.spec.availability?.partyField; return key ? Number(this.values[key] ?? 0) : 0; }
  seatsChosen(model: DataRow, key: string): number { return this.tableOptions.filter(table => this.selected(model, key, table.id)).reduce((sum, table) => sum + Number(table.capacity), 0); }
  seatsOk(model: DataRow, key: string): boolean { const chosen = this.seatsChosen(model, key); return chosen > 0 && chosen >= this.seatsNeeded(); }
  seatsMessage(model: DataRow, key: string): string { const need = this.seatsNeeded(), chosen = this.seatsChosen(model, key); if (!chosen) return `Toca una o varias mesas libres. Necesitas ${need} lugares.`; return chosen >= need ? `✓ Listo: ${chosen} lugares para ${need} personas.` : `Necesitas ${need} lugares y elegiste ${chosen}. Elige otra mesa más.`; }
  private async loadTables(generation: number): Promise<void> {
    const rule = this.spec?.availability; if (!rule) return;
    const start = new Date(String(this.values[rule.startField] ?? '')), end = new Date(String(this.values[rule.endField] ?? ''));
    if (!Number.isFinite(start.getTime()) || !Number.isFinite(end.getTime()) || end <= start) { this.tableOptions = []; this.availabilityLoading = false; this.availabilityMessage = 'Primero elige la fecha y la hora para ver las mesas libres.'; this.detector.markForCheck(); return; }
    try {
      const query = `?start=${encodeURIComponent(start.toISOString())}&end=${encodeURIComponent(end.toISOString())}${rule.reservationId ? '&reservation=' + rule.reservationId : ''}`;
      const rows = await firstValueFrom(this.api.get<TableChoice[]>(rule.path + query));
      if (generation !== this.availabilityGeneration) return;
      this.tableOptions = rows;
      const free = new Set(rows.filter(row => row.available).map(row => String(row.id)));
      this.values[rule.tablesField] = (this.values[rule.tablesField] ?? []).filter((id: any) => free.has(String(id)));
      this.availabilityMessage = rows.some(row => row.available) ? '' : 'No hay mesas libres a esa hora. Prueba otro horario.';
    } catch { if (generation !== this.availabilityGeneration) return; this.tableOptions = []; this.availabilityMessage = 'No se pudieron consultar las mesas. Vuelve a intentarlo.'; }
    finally { if (generation === this.availabilityGeneration) { this.availabilityLoading = false; this.detector.markForCheck(); } }
  }
  selectValue(field: Field, model: DataRow, value: string): void {
    const choice = this.choices(field, model).find(item => String(item.id) === value); model[field.key] = choice?.id ?? '';
    if (field.key === 'productId') model['optionIds'] = [];
    if (field.key === 'orderId' && this.spec.lineKey === 'methods' && choice) { const balance = Number(choice['total']) - Number(choice['paid']); this.lines = [{ method: 'CASH', amount: Number(balance.toFixed(2)), tip: 0 }]; }
  }
  toggle(key: string, model: DataRow, value: string | number, checked: boolean): void { const selected: any[] = model[key] ?? []; model[key] = checked ? [...selected.filter(item => String(item) !== String(value)), value] : selected.filter(item => String(item) !== String(value)); }
  addLine(): void { if (this.lines.length < 100) this.lines.push(this.defaults(this.spec.lineFields ?? [])); }
  normalize(model: DataRow, fields: Field[]): DataRow {
    const result: DataRow = { ...model };
    for (const field of fields) {
      if (!this.visible(field, model)) { delete result[field.key]; continue; }
      const value = model[field.key];
      if (value === '' || value === undefined || value === null) { if (field.required && field.type !== 'boolean') throw new Error('Completa: ' + field.label + '.'); delete result[field.key]; continue; }
      if (field.type === 'number') { const number = Number(value); if (!Number.isFinite(number)) throw new Error(field.label + ' debe ser un número.'); result[field.key] = number; }
      if (field.type === 'datetime-local') result[field.key] = new Date(value).toISOString();
      if (field.required && (field.type === 'multi' || field.type === 'tables') && !(value as unknown[]).length) throw new Error('Elige al menos una opción en ' + field.label + '.');
    }
    return result;
  }
  submit(event: Event): void {
    event.preventDefault(); if (this.busy) return;
    try {
      const payload = this.normalize(this.values, this.spec.fields);
      if (this.spec.fields.some(field => field.key === 'confirmPassword') && (payload['password'] ?? '') !== (payload['confirmPassword'] ?? '')) throw new Error('Las contraseñas no coinciden.');
      const rule = this.spec.availability;
      if (rule && (payload[rule.tablesField] ?? []).length && !this.seatsOk(this.values, rule.tablesField)) throw new Error(this.seatsMessage(this.values, rule.tablesField));
      if (this.spec.lineFields?.length) { if (!this.lines.length) throw new Error('Agrega al menos una fila.'); payload[this.spec.lineKey ?? 'lines'] = this.lines.map(row => this.normalize(row, this.spec.lineFields!)); }
      const reglaRota = this.spec.validar?.(payload); // reglas de negocio propias del formulario (ver module-config.ts)
      if (reglaRota) throw new Error(reglaRota);
      this.localError = ''; this.saved.emit(payload);
    } catch (error) { this.localError = (error as Error).message; }
  }
}
