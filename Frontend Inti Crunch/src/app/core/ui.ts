export type DataRow = Record<string, any>;
export type Choice = { id: string | number; label: string;[key: string]: any };
export type Field = {
  key: string; label: string; type?: 'text' | 'number' | 'date' | 'datetime-local' | 'textarea' | 'select' | 'multi' | 'boolean' | 'password' | 'email' | 'tel' | 'tables';
  required?: boolean; source?: string; options?: Choice[]; hint?: string; min?: number; max?: number; step?: string; placeholder?: string; maxlength?: number;
  when?: { key: string; values: any[] }; initial?: any;
};
export type FormSpec = {
  title: string; description?: string; path: string; method?: 'POST' | 'PATCH' | 'PUT'; fields: Field[];
  lineFields?: Field[]; lineKey?: string; initial?: DataRow; lineInitial?: DataRow[];
  availability?: { path: string; startField: string; endField: string; tablesField: string; partyField?: string; reservationId?: number };
  submitLabel?: string;
  /** REGLA DE NEGOCIO en pantalla: devuelve un mensaje para impedir guardar, o null si todo está bien. */
  validar?: (datos: DataRow) => string | null;
};
export const ROLE = { admin: ['ADMINISTRADOR'], manage: ['ADMINISTRADOR', 'SUPERVISOR'], sales: ['ADMINISTRADOR', 'SUPERVISOR', 'CAJERO', 'OPERACIONES'], hall: ['ADMINISTRADOR', 'SUPERVISOR', 'CAJERO', 'OPERACIONES'], delivery: ['ADMINISTRADOR', 'SUPERVISOR', 'CAJERO', 'OPERACIONES', 'REPARTIDOR'], cash: ['ADMINISTRADOR', 'SUPERVISOR', 'CAJERO'], stock: ['ADMINISTRADOR', 'SUPERVISOR', 'ABASTECIMIENTO', 'OPERACIONES'], supply: ['ADMINISTRADOR', 'SUPERVISOR', 'ABASTECIMIENTO'], ops: ['ADMINISTRADOR', 'SUPERVISOR', 'OPERACIONES'] };
export const money = (value: unknown): string => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(Number(value ?? 0));
export const errorMessage = (error: any): string => error?.status === 0 ? 'No se pudo conectar con el servidor. Verifica que los servicios estén iniciados.' : error?.name === 'TimeoutError' ? 'El servidor tarda en responder. Actualiza para comprobar el resultado antes de reintentar.' : error?.error?.detail ?? error?.error?.message ?? (error?.status === 401 ? 'Tu sesión venció. Vuelve a iniciar sesión.' : error?.status === 403 ? 'Tu rol no tiene permiso para esta operación.' : 'No se pudo completar la operación. Actualiza y vuelve a intentarlo.');
export const labels: Record<string, string> = { RECEIVED: 'Recibido', PREPARING: 'En preparación', READY: 'Listo', DISPATCHED: 'En ruta', COMPLETED: 'Completado', CANCELLED: 'Cancelado', PICKUP: 'Recojo', TAKEAWAY: 'Para llevar', DINE_IN: 'Salón', DELIVERY: 'Delivery', CASH: 'Efectivo', CARD: 'Tarjeta', YAPE: 'Yape', PLIN: 'Plin', TRANSFER: 'Transferencia', MIXED: 'Mixto', OPEN: 'Abierto', CLOSED: 'Cerrado', PENDING: 'Pendiente', PARTIAL: 'Parcial', QUEUED: 'En espera', ASSIGNED: 'Asignado', ON_ROUTE: 'En ruta', DELIVERED: 'Entregado', BOOKED: 'Reservada', ARRIVED: 'En el local', NO_SHOW: 'No asistió', PLANNED: 'Programado', ACTIVE: 'En curso', LATE: 'Tardanza', PRESENT: 'Presente', ABSENT: 'Ausente', RESOLVED: 'Resuelta', ORDERED: 'Orden emitida', RECEIVED_PO: 'Recibida', IN: 'Entrada', OUT: 'Salida (uso)', WASTE: 'Merma (se malogró)', ADJUST_IN: 'Conteo: sobró', ADJUST_OUT: 'Conteo: faltó', DEMORA: 'Demora', 'DIRECCIÓN': 'Dirección equivocada', CLIENTE_AUSENTE: 'Cliente no estaba', PRODUCTO: 'Problema con el producto', OTRO: 'Otro', ADMINISTRADOR: 'Administrador', SUPERVISOR: 'Supervisor', CAJERO: 'Cajero', OPERACIONES: 'Operaciones (cocina y salón)', ABASTECIMIENTO: 'Abastecimiento (compras)', REPARTIDOR: 'Repartidor', LIBRE: 'Libre', OCUPADA: 'Ocupada', RESERVADA: 'Reservada', COMPLETED_RES: 'Finalizada', BOLETA: 'Boleta', FACTURA: 'Factura' };
export const option = (values: string[]): Choice[] => values.map(value => ({ id: value, label: labels[value] ?? value }));
export function downloadPdf(blob: Blob, name: string): void { const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = name; link.click(); setTimeout(() => URL.revokeObjectURL(url), 30000); }
