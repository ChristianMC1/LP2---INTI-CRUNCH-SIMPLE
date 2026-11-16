# Banco de reglas de negocio (posibles pedidos del docente)

Reglas listas para copiar y pegar, agrupadas por módulo.
Cada una dice **qué pide el docente**, **dónde va en el backend** (archivo, método y lugar exacto),
**qué poner en el frontend** y **cómo probarla**.

> Todas las rutas del backend empiezan en `Backend Inti Crunch/src/main/java/pe/inticrunch/`
> y las del frontend en `Frontend Inti Crunch/src/app/`.
> El formulario del panel está en **`features/operaciones/module-config.ts`** (lo llamamos `module-config.ts`).

---

## 0. Antes de empezar (lee esto una vez)

### Cómo se escribe una regla en el backend

Siempre en el **Service** del módulo, con el mismo patrón:

```java
// REGLA NUEVA-XX: qué controla
if (condición que está MAL) throw new IllegalArgumentException("Mensaje para el usuario.");
```

| Excepción | Cuándo usarla | Respuesta |
|---|---|---|
| `IllegalArgumentException` | El **dato** está mal (monto, texto, fecha) | Error 400 |
| `IllegalStateException` | La **situación** no lo permite (estado, horario, límite) | Error 409 |

No hay que tocar nada más: el mensaje aparece solo en la pantalla (en rojo).
Usa códigos `NUEVA-01`, `NUEVA-02`… para que no choquen con las reglas que ya existen (`REGLAS.md`).

### Cómo se escribe una regla en el frontend (para avisar antes de enviar)

En `module-config.ts`, dentro del formulario, de la más fácil a la más completa:

| Forma | Ejemplo | Sirve para |
|---|---|---|
| `min` / `max` | `{ ...numberField('amount', 'Monto'), min: 50, max: 1000 }` | Números |
| `maxlength` | `{ ...textField('dni', 'DNI'), maxlength: 8 }` | Largo de texto |
| `true` (obligatorio) | `textField('email', 'Correo', true)` | Campos obligatorios |
| `validar` | `validar: d => d['amount'] > 200 ? 'Mensaje' : null` | Cualquier otra condición |

`validar` va **dentro** de las llaves del formulario, al final, separado por coma:

```ts
const zoneForm: FormSpec = { title: '...', path: '...', fields: [...], validar: d => ... };
```

Dentro de `validar`, `d` tiene los datos del formulario: `d['campo']`. Si hay filas (productos, insumos…),
están en `d['lines']` (o en `d['employees']` / `d['methods']` según el formulario).

> **Siempre pon la regla en el backend.** El frontend solo avisa antes; el backend es el que manda.

### Imports que a veces hacen falta (arriba del archivo .java)

```java
import java.math.BigDecimal;   // montos
import java.time.LocalTime;    // horas del día
import java.time.Duration;     // duraciones (horas, días)
```

Si VS Code subraya en rojo `LocalTime` o `Duration`, agrega su import.

### Después de cada regla

1. Backend: Ctrl+C y vuelve a ejecutar `.\iniciar.ps1`.
2. Frontend: se actualiza solo al guardar.
3. Prueba el caso **malo** (debe salir tu mensaje) y el **bueno** (debe guardar).

---

## 1. Pedidos (Ventas)

Backend: **`pedidos/PedidoService.java`**, método **`create`** (crear pedido, sirve para el panel y para la página web).

### NUEVA-01 · Máximo 20 unidades de un mismo producto

**El docente dice:** "Un pedido no puede tener más de 20 unidades de un mismo producto."

**Backend** — en `create`, dentro del `for` de las líneas, justo después de:
`long product = id(detail, "productId"); int quantity = integer(detail.get("quantity"), "Cantidad");`

```java
// REGLA NUEVA-01: máximo 20 unidades de un mismo producto.
if (quantity > 20) throw new IllegalArgumentException("Máximo 20 unidades por producto.");
```

**Frontend** — `module-config.ts`, en `saleForm` → `lineFields`, el campo cantidad:

```ts
{ ...numberField('quantity', 'Cantidad', true, '1'), min: 1, max: 20, initial: 1 }
```

**Prueba:** Ventas y pedidos → Nuevo pedido → cantidad 21.

### NUEVA-02 · Máximo 10 productos distintos por pedido

**Backend** — en `create`, justo **antes** de `BigDecimal subtotal = BigDecimal.ZERO;`:

```java
// REGLA NUEVA-02: máximo 10 productos distintos por pedido.
if (lines(data, "lines").size() > 10) throw new IllegalArgumentException("Un pedido puede tener como máximo 10 productos distintos.");
```

**Frontend** — `module-config.ts`, al final de `saleForm` (después de `lineFields: [...]`):

```ts
, validar: d => d['lines'].length > 10 ? 'Un pedido puede tener como máximo 10 productos distintos.' : null
```

### NUEVA-03 · Consumo mínimo de S/ 10

**Backend** — en `create`, justo después de `order.setSubtotal(subtotal); order.setTotal(subtotal.add(fee));`:

```java
// REGLA NUEVA-03: el pedido debe ser de al menos S/ 10.
if (subtotal.compareTo(new BigDecimal("10")) < 0) throw new IllegalArgumentException("El pedido mínimo es de S/ 10.");
```

**Frontend** — página web: `features/portal/checkout.component.ts`, método `place`, debajo de la línea
`if (this.channel === 'DINE_IN' && !this.tableId) ...`:

```ts
if (this.subtotal < 10) { this.error = 'El pedido mínimo es de S/ 10.'; return; }
```

(En el panel no hace falta: el mensaje del backend aparece solo.)

### NUEVA-04 · Delivery solo desde S/ 25

**Backend** — mismo lugar que NUEVA-03:

```java
// REGLA NUEVA-04: el delivery requiere un consumo mínimo de S/ 25.
if (channel.equals("DELIVERY") && subtotal.compareTo(new BigDecimal("25")) < 0) throw new IllegalArgumentException("Para delivery el consumo mínimo es de S/ 25.");
```

**Frontend** — `checkout.component.ts`, método `place`, mismo lugar que NUEVA-03:

```ts
if (this.channel === 'DELIVERY' && this.subtotal < 25) { this.error = 'Para delivery el consumo mínimo es de S/ 25.'; return; }
```

### NUEVA-05 · Solo se reciben pedidos de 11:00 a 23:00

**Backend** — en `create`, como **primera línea** del método (antes de `String channel = ...`):

```java
// REGLA NUEVA-05: horario de atención de 11:00 a 23:00 (hora de Lima).
LocalTime ahora = LocalTime.now(LIMA);
if (ahora.isBefore(LocalTime.of(11, 0)) || ahora.isAfter(LocalTime.of(23, 0))) throw new IllegalStateException("Solo recibimos pedidos de 11:00 a 23:00.");
```

Import: `import java.time.LocalTime;` (`LIMA` ya existe en `Validaciones`).

**Frontend** — `checkout.component.ts`, método `place`, al inicio de las validaciones:

```ts
const hora = new Date().getHours();
if (hora < 11 || hora >= 23) { this.error = 'Solo recibimos pedidos de 11:00 a 23:00.'; return; }
```

### NUEVA-06 · El nombre del cliente debe tener al menos 3 letras y solo letras

**Backend** — en `create`, justo después de `String name = str(data, "customerName"), phone = str(data, "customerPhone");`:

```java
// REGLA NUEVA-06: el nombre solo lleva letras y espacios (mínimo 3).
if (!name.matches("[\\p{L} ]{3,120}")) throw new IllegalArgumentException("El nombre debe tener al menos 3 letras y no llevar números.");
```

**Frontend** — `module-config.ts`, al final de `saleForm`:

```ts
, validar: d => /^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,}$/.test(d['customerName']) ? null : 'El nombre debe tener al menos 3 letras y no llevar números.'
```

(Si ya pusiste otro `validar` en `saleForm`, únelos: `validar: d => condición1 ? 'msg1' : condición2 ? 'msg2' : null`.)

### NUEVA-07 · Un pedido solo se puede cancelar si aún no se prepara

**Backend** — `preparacion/service/PreparacionService.java`, método **`transition`**, justo después de la línea `// REGLA DELIVERY-02 ...` y su `if`:

```java
// REGLA NUEVA-07: solo se cancela un pedido que aún no se empezó a preparar.
if (next.equals("CANCELLED") && !previous.equals("RECEIVED")) throw new IllegalStateException("El pedido ya se está preparando; ya no se puede cancelar.");
```

**Frontend:** no hace falta; el mensaje aparece al tocar "Cancelar".

### NUEVA-08 · El descuento manual no puede pasar del 30 % del total

**Backend** — `pedidos/PedidoService.java`, método **`discount`**, justo después de la línea `// REGLA PEDIDOS-04 ...` y su `if`:

```java
// REGLA NUEVA-08: el descuento manual máximo es el 30 % del total.
if (discount.compareTo(record.getTotal().multiply(new BigDecimal("0.30"))) > 0) throw new IllegalArgumentException("El descuento no puede pasar del 30 % del total.");
```

**Frontend:** no hace falta (el formulario de descuento está en `workspace.component.ts` y muestra el mensaje).

---

## 2. Caja y pagos

Backend: **`caja/service/CajaService.java`**.

### NUEVA-09 · La caja se abre con un fondo entre S/ 50 y S/ 1 000

**Backend** — método **`openCash`**, justo **antes** de `SesionCaja s = new SesionCaja();`:

```java
BigDecimal fondo = money(data.get("openingAmount"), "Fondo inicial");
// REGLA NUEVA-09: el fondo inicial va de S/ 50 a S/ 1 000.
if (fondo.compareTo(new BigDecimal("50")) < 0 || fondo.compareTo(new BigDecimal("1000")) > 0) throw new IllegalArgumentException("El fondo inicial debe estar entre S/ 50 y S/ 1 000.");
```

**Frontend** — `module-config.ts`, formulario **"Abrir caja"** (vista `sessions`), el campo:

```ts
{ ...numberField('openingAmount', 'Dinero inicial en efectivo (S/)'), min: 50, max: 1000 }
```

### NUEVA-10 · Un retiro de caja no puede ser mayor a S/ 200

**Backend** — método **`movement`**, justo después de la línea `// REGLA CAJA-03 ...` y su `if`:

```java
// REGLA NUEVA-10: cada retiro es de máximo S/ 200.
if (kind.equals("OUT") && amount.compareTo(new BigDecimal("200")) > 0) throw new IllegalArgumentException("Cada retiro de caja es de máximo S/ 200.");
```

**Frontend** — `module-config.ts`, formulario **"Dinero que entra o sale"** (vista `movements`), al final del formulario:

```ts
, validar: d => d['kind'] === 'OUT' && d['amount'] > 200 ? 'Cada retiro de caja es de máximo S/ 200.' : null
```

### NUEVA-11 · El motivo del movimiento debe tener al menos 10 caracteres

**Backend** — método **`movement`**, mismo lugar que NUEVA-10:

```java
// REGLA NUEVA-11: el motivo debe explicarse (mínimo 10 caracteres).
if (str(data, "reason").length() < 10) throw new IllegalArgumentException("Explica el motivo con al menos 10 caracteres.");
```

**Frontend** — mismo formulario:

```ts
, validar: d => (d['reason'] ?? '').length < 10 ? 'Explica el motivo con al menos 10 caracteres.' : null
```

### NUEVA-12 · No se puede cerrar la caja si falta o sobra más de S/ 50

**Backend** — método **`closeCash`**, justo después de la línea que calcula `difference`:

```java
// REGLA NUEVA-12: la diferencia al cerrar no puede pasar de S/ 50.
if (difference.abs().compareTo(new BigDecimal("50")) > 0) throw new IllegalStateException("La diferencia es de S/ " + difference + ". Vuelve a contar el efectivo antes de cerrar.");
```

**Frontend:** no hace falta (el sistema calcula lo esperado en el backend).

### NUEVA-13 · La propina no puede pasar del 20 % del pago

**Backend** — método **`collectPayment`**, justo **después** del `for (var method : methods) { ... }` que suma `amount` y `tip`:

```java
// REGLA NUEVA-13: la propina máxima es el 20 % del pago.
if (tip.compareTo(amount.multiply(new BigDecimal("0.20"))) > 0) throw new IllegalArgumentException("La propina no puede pasar del 20 % del pago.");
```

**Frontend** — `module-config.ts`, al final de **`paymentForm`**:

```ts
, validar: d => {
    const monto = d['methods'].reduce((s: number, m: any) => s + m.amount, 0);
    const propina = d['methods'].reduce((s: number, m: any) => s + (m.tip ?? 0), 0);
    return propina > monto * 0.2 ? 'La propina no puede pasar del 20 % del pago.' : null;
  }
```

### NUEVA-14 · Pago con tarjeta desde S/ 5

**Backend** — método **`collectPayment`**, dentro del `for (var method : methods)`, justo después de la línea `// REGLA CAJA-09 ...` y su `if`:

```java
// REGLA NUEVA-14: con tarjeta se cobra desde S/ 5.
if (str(method, "method").equalsIgnoreCase("CARD") && part.compareTo(new BigDecimal("5")) < 0) throw new IllegalArgumentException("Con tarjeta el pago mínimo es S/ 5.");
```

**Frontend** — `paymentForm`:

```ts
, validar: d => d['methods'].some((m: any) => m.method === 'CARD' && m.amount < 5) ? 'Con tarjeta el pago mínimo es S/ 5.' : null
```

### NUEVA-15 · Factura solo para pagos desde S/ 20

**Backend** — método **`collectPayment`**, justo después de la línea `// REGLA CAJA-12 ...` y su `if`:

```java
// REGLA NUEVA-15: la factura se emite solo para pagos desde S/ 20.
if (type.equals("FACTURA") && amount.compareTo(new BigDecimal("20")) < 0) throw new IllegalArgumentException("La factura se emite para pagos desde S/ 20.");
```

**Frontend** — `paymentForm`:

```ts
, validar: d => d['documentType'] === 'FACTURA' && d['methods'].reduce((s: number, m: any) => s + m.amount, 0) < 20 ? 'La factura se emite para pagos desde S/ 20.' : null
```

### NUEVA-16 · Solo se devuelve dinero dentro de las 24 horas del pago

**Backend** — método **`refund`**, justo después de `Pago record = pagos.bloquearConfirmado(payment)...;`:

```java
// REGLA NUEVA-16: las devoluciones se hacen dentro de las 24 horas del pago.
if (record.getCreatedAt().isBefore(java.time.Instant.now().minusSeconds(24 * 3600))) throw new IllegalStateException("Solo se puede devolver dentro de las 24 horas del pago.");
```

**Frontend:** no hace falta.

---

## 3. Carta (productos)

### NUEVA-17 · El precio de un producto va de S/ 1 a S/ 200

**Backend (editar)** — `catalogo/CatalogoService.java`, método **`updateProduct`**, justo después de la línea `// REGLA CATALOGO-01 ...` y su `if`:

```java
// REGLA NUEVA-17: el precio va de S/ 1 a S/ 200.
if (price.compareTo(BigDecimal.ONE) < 0 || price.compareTo(new BigDecimal("200")) > 0) throw new IllegalArgumentException("El precio debe estar entre S/ 1 y S/ 200.");
```

**Backend (crear)** — `catalogo/ProductoAdministracionController.java`, en el `record ProductRequest`, cambia la línea del precio:

```java
@NotNull @DecimalMin(value = "1.00", message = "El precio mínimo es S/ 1") @DecimalMax(value = "200.00", message = "El precio máximo es S/ 200") BigDecimal price,
```

(Crear producto usa anotaciones de validación; el mensaje sale como `price: El precio máximo es S/ 200`.)

**Frontend** — `module-config.ts`, en `productForm`:

```ts
{ ...numberField('price', 'Precio con IGV (S/)'), min: 1, max: 200 }
```

### NUEVA-18 · No puede haber dos productos con el mismo nombre

**Backend** —
1. `catalogo/ProductoRepository.java`, agrega el método (JPA lo arma solo por su nombre):
   ```java
   boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);
   ```
2. `catalogo/ProductoAdministracionController.java`, método **`create`**, como primera línea:
   ```java
   // REGLA NUEVA-18: no se repiten nombres de productos.
   if (products.existsByNombreIgnoreCaseAndActivoTrue(request.name())) throw new IllegalArgumentException("Ya existe un producto llamado " + request.name() + ".");
   ```

**Frontend:** no hace falta.

---

## 4. Clientes y promociones

Backend: **`clientes/service/ClienteService.java`**.

### NUEVA-19 · El correo del cliente es obligatorio

**Backend** — método **`createCustomer`**, justo después de `String phone = ...;`:

```java
// REGLA NUEVA-19: el correo es obligatorio y válido.
if (!str(data, "email").matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) throw new IllegalArgumentException("Escribe un correo válido.");
```

**Frontend** — `module-config.ts`, en `customerForm`:

```ts
{ ...textField('email', 'Correo', true), type: 'email' }
```

### NUEVA-20 · El DNI del cliente es obligatorio

**Backend** — método **`dniValido`**, como primera línea:

```java
// REGLA NUEVA-20: el DNI es obligatorio.
if (optional(data, "dni").isBlank()) throw new IllegalArgumentException("El DNI es obligatorio.");
```

**Frontend** — `customerForm`: `{ ...textField('dni', 'DNI', true), maxlength: 8 }`

### NUEVA-21 · Una promoción descuenta como máximo 50 % (o S/ 30)

**Backend** — método **`llenar`**, justo después de la línea `// REGLA CLIENTES-07 ...` y su `if`:

```java
// REGLA NUEVA-21: descuento máximo 50 % o S/ 30.
if (porcentaje != null && porcentaje.compareTo(new BigDecimal("50")) > 0) throw new IllegalArgumentException("Una promoción descuenta como máximo 50 %.");
if (monto != null && monto.compareTo(new BigDecimal("30")) > 0) throw new IllegalArgumentException("Una promoción descuenta como máximo S/ 30.");
```

**Frontend** — `module-config.ts`, en `promotionForm`:

```ts
{ ...numberField('discountPercent', 'Descuento en %', false), max: 50 }, { ...numberField('discountAmount', 'O descuento fijo (S/)', false), max: 30 }
```

### NUEVA-22 · Una promoción dura como máximo 30 días

**Backend** — método **`llenar`**, justo después de la línea `// REGLA CLIENTES-08 ...` y su `if`:

```java
// REGLA NUEVA-22: una promoción dura como máximo 30 días.
if (end.isAfter(start.plus(java.time.Duration.ofDays(30)))) throw new IllegalArgumentException("Una promoción dura como máximo 30 días.");
```

**Frontend** — al final de `promotionForm`:

```ts
, validar: d => new Date(d['endsAt']).getTime() - new Date(d['startsAt']).getTime() > 30 * 86400000 ? 'Una promoción dura como máximo 30 días.' : null
```

### NUEVA-23 · El cupón va en MAYÚSCULAS, de 4 a 12 letras o números

**Backend** — método **`llenar`**, mismo lugar que NUEVA-22:

```java
// REGLA NUEVA-23: cupón de 4 a 12 letras mayúsculas o números.
if (!String.valueOf(data.get("code")).matches("[A-Z0-9]{4,12}")) throw new IllegalArgumentException("El cupón debe tener de 4 a 12 letras MAYÚSCULAS o números, sin espacios.");
```

**Frontend** — `promotionForm`:

```ts
, validar: d => /^[A-Z0-9]{4,12}$/.test(d['code']) ? null : 'El cupón debe tener de 4 a 12 letras MAYÚSCULAS o números, sin espacios.'
```

---

## 5. Inventario

Backend: **`inventario/service/InventarioService.java`**.

### NUEVA-24 · El stock mínimo de un insumo debe ser al menos 1

**Backend** — método **`createIngredient`**, justo después de `BigDecimal minimo = decimal(...);`:

```java
// REGLA NUEVA-24: el aviso de stock mínimo debe ser al menos 1.
if (minimo.compareTo(BigDecimal.ONE) < 0) throw new IllegalArgumentException("El stock mínimo debe ser al menos 1.");
```

**Frontend** — `module-config.ts`, en `ingredientFields`:

```ts
{ ...numberField('minimumStock', 'Avisarme cuando quede menos de', true, '0.001'), min: 1, initial: 1 }
```

### NUEVA-25 · Una merma no puede pasar de 5 unidades por insumo

**Backend** — método **`createMovement`**, dentro del `for`, justo después de
`long ingredient = id(line, "ingredientId"); BigDecimal quantity = positive(line.get("quantity"), "Cantidad");`:

```java
// REGLA NUEVA-25: una merma es de máximo 5 unidades por insumo.
if (kind.equals("WASTE") && quantity.compareTo(new BigDecimal("5")) > 0) throw new IllegalArgumentException("Una merma no puede pasar de 5 unidades por insumo.");
```

**Frontend** — al final de `movementForm`:

```ts
, validar: d => d['kind'] === 'WASTE' && d['lines'].some((l: any) => l.quantity > 5) ? 'Una merma no puede pasar de 5 unidades por insumo.' : null
```

### NUEVA-26 · Solo entran lotes que vencen en 3 días o más

**Backend** — método **`createMovement`**, cambia la línea de `REGLA INVENTARIO-06` por:

```java
// REGLA NUEVA-26: el lote debe vencer en 3 días o más.
if (expiry != null && expiry.isBefore(hoy().plusDays(3))) throw new IllegalArgumentException("El lote debe vencer en 3 días o más.");
```

**Frontend** — `movementForm`:

```ts
, validar: d => d['lines'].some((l: any) => l.expiresOn && new Date(l.expiresOn).getTime() < Date.now() + 3 * 86400000) ? 'El lote debe vencer en 3 días o más.' : null
```

---

## 6. Compras y proveedores

Backend: **`compras/service/ComprasService.java`**.

### NUEVA-27 · El RUC del proveedor es obligatorio

**Backend** — método **`validateTax`**, reemplaza su `if` por:

```java
// REGLA NUEVA-27: el RUC es obligatorio y tiene 11 dígitos.
if (!optional(data, "taxId").matches("[0-9]{11}")) throw new IllegalArgumentException("El RUC es obligatorio y debe tener 11 dígitos.");
```

**Frontend** — `module-config.ts`, en `supplierForm`:

```ts
{ ...textField('taxId', 'RUC (11 números)', true), maxlength: 11 }
```

### NUEVA-28 · Una orden de compra es de máximo S/ 5 000

**Backend** — método **`createPurchase`**, justo **después** del `for (var detail : lines(data, "lines")) { ... }`:

```java
// REGLA NUEVA-28: una orden de compra es de máximo S/ 5 000.
if (total.compareTo(new BigDecimal("5000")) > 0) throw new IllegalArgumentException("Una orden de compra no puede pasar de S/ 5 000.");
```

**Frontend** — al final de `purchaseOrderForm`:

```ts
, validar: d => d['lines'].reduce((s: number, l: any) => s + l.quantity * l.unitPrice, 0) > 5000 ? 'Una orden de compra no puede pasar de S/ 5 000.' : null
```

### NUEVA-29 · Máximo 100 unidades por insumo en una orden

**Backend** — método **`createPurchase`**, dentro del `for`, justo después de `BigDecimal quantity = positive(...), price = money(...);`:

```java
// REGLA NUEVA-29: máximo 100 unidades por insumo.
if (quantity.compareTo(new BigDecimal("100")) > 0) throw new IllegalArgumentException("Máximo 100 unidades por insumo en una orden.");
```

**Frontend** — en `module-config.ts`, la función `quantity` (arriba del archivo) la usan varias pantallas;
mejor agrega en `purchaseOrderForm`:

```ts
, validar: d => d['lines'].some((l: any) => l.quantity > 100) ? 'Máximo 100 unidades por insumo en una orden.' : null
```

### NUEVA-30 · La factura del proveedor no puede tener fecha de pago vencida

**Backend** — método **`invoice`**, justo **antes** de `FacturaCompra f = new FacturaCompra();`:

```java
// REGLA NUEVA-30: la fecha límite de pago no puede estar vencida.
if (LocalDate.parse(str(data, "dueDate")).isBefore(hoy())) throw new IllegalArgumentException("La fecha límite de pago no puede ser anterior a hoy.");
```

**Frontend** — `module-config.ts`, formulario **"Factura del proveedor"** (vista `invoices`), al final:

```ts
, validar: d => d['dueDate'] < new Date().toISOString().slice(0, 10) ? 'La fecha límite de pago no puede ser anterior a hoy.' : null
```

---

## 7. Personal y turnos

Backend: **`personal/service/PersonalService.java`**.

### NUEVA-31 · Un turno dura como máximo 8 horas

**Backend** — método **`validatePeriod`**, reemplaza su `if` (REGLA PERSONAL-18) por:

```java
// REGLA NUEVA-31: un turno dura como máximo 8 horas.
if (!end.isAfter(start) || end.isAfter(start.plusSeconds(8 * 3600))) throw new IllegalArgumentException("Un turno debe durar como máximo 8 horas.");
```

(Sirve para crear y editar, porque los dos llaman a `validatePeriod`.)

**Frontend** — al final de `shiftForm`:

```ts
, validar: d => new Date(d['endsAt']).getTime() - new Date(d['startsAt']).getTime() > 8 * 3600000 ? 'Un turno debe durar como máximo 8 horas.' : null
```

### NUEVA-32 · Máximo 5 personas por turno

**Backend** — método **`saveMembers`**, como primera línea (lo usan crear y editar):

```java
// REGLA NUEVA-32: máximo 5 personas por turno.
if (lines(data, "employees").size() > 5) throw new IllegalArgumentException("Un turno puede tener como máximo 5 personas.");
```

**Frontend** — `shiftForm`:

```ts
, validar: d => d['employees'].length > 5 ? 'Un turno puede tener como máximo 5 personas.' : null
```

### NUEVA-33 · La fecha de ingreso no puede ser futura

**Backend** — método **`createEmployee`**, justo después de la línea `// REGLA PERSONAL-05 ...` y su `if`:

```java
// REGLA NUEVA-33: la fecha de ingreso no puede ser futura.
if (!optional(data, "hiredOn").isBlank() && LocalDate.parse(str(data, "hiredOn")).isAfter(hoy())) throw new IllegalArgumentException("La fecha de ingreso no puede ser futura.");
```

**Frontend** — al final de `staffCreateForm`:

```ts
, validar: d => d['hiredOn'] && d['hiredOn'] > new Date().toISOString().slice(0, 10) ? 'La fecha de ingreso no puede ser futura.' : null
```

---

## 8. Reservas y mesas

Backend: **`reservas/service/ReservaService.java`**, método **`createReservation`**
(lo usan el panel y la página web). Para que también valga al **editar**, copia la misma regla en **`updateReservation`**.

### NUEVA-34 · Una reserva es para 10 personas como máximo

**Backend** — en `createReservation`, justo después de `int guests = integer(data.get("partySize"), "Personas");`:

```java
// REGLA NUEVA-34: una reserva es para 10 personas como máximo.
if (guests > 10) throw new IllegalArgumentException("Las reservas son para 10 personas como máximo.");
```

**Frontend** —
- Panel: `module-config.ts`, en `reservationForm`: `{ ...numberField('partySize', 'Personas', true, '1'), min: 1, max: 10, initial: 2 }`
- Página web: `features/portal/reservation.component.ts`, línea `readonly people = Array.from({ length: 16 }, ...)` → cambia `16` por `10`.

### NUEVA-35 · Se reserva con al menos 2 horas de anticipación

**Backend** — en `createReservation`, justo después de la línea `// REGLA RESERVAS-06 ...` y su `if`:

```java
// REGLA NUEVA-35: se reserva con al menos 2 horas de anticipación.
if (start.isBefore(Instant.now().plusSeconds(2 * 3600))) throw new IllegalArgumentException("Reserva con al menos 2 horas de anticipación.");
```

**Frontend** — panel, al final de `reservationForm`:

```ts
, validar: d => new Date(d['startsAt']).getTime() < Date.now() + 2 * 3600000 ? 'Reserva con al menos 2 horas de anticipación.' : null
```

### NUEVA-36 · No se reserva con más de 30 días de anticipación

**Backend** — mismo lugar que NUEVA-35:

```java
// REGLA NUEVA-36: máximo 30 días de anticipación.
if (start.isAfter(Instant.now().plusSeconds(30L * 86400))) throw new IllegalArgumentException("Solo se puede reservar hasta con 30 días de anticipación.");
```

**Frontend** — `reservationForm`:

```ts
, validar: d => new Date(d['startsAt']).getTime() > Date.now() + 30 * 86400000 ? 'Solo se puede reservar hasta con 30 días de anticipación.' : null
```

### NUEVA-37 · Las reservas son de 12:00 a 22:00

**Backend** — mismo lugar que NUEVA-35:

```java
// REGLA NUEVA-37: horario de reservas de 12:00 a 22:00 (hora de Lima).
LocalTime hora = start.atZone(LIMA).toLocalTime();
if (hora.isBefore(LocalTime.of(12, 0)) || hora.isAfter(LocalTime.of(22, 0))) throw new IllegalArgumentException("Las reservas son de 12:00 a 22:00.");
```

Import: `import java.time.LocalTime;`

**Frontend** — `reservationForm`:

```ts
, validar: d => { const h = new Date(d['startsAt']).getHours(); return h < 12 || h >= 22 ? 'Las reservas son de 12:00 a 22:00.' : null; }
```

### NUEVA-38 · Una reserva dura como máximo 3 horas

**Backend** — mismo lugar que NUEVA-35:

```java
// REGLA NUEVA-38: una reserva dura como máximo 3 horas.
if (end.isAfter(start.plusSeconds(3 * 3600))) throw new IllegalArgumentException("Una reserva dura como máximo 3 horas.");
```

**Frontend** — `reservationForm`:

```ts
, validar: d => d['endsAt'] && new Date(d['endsAt']).getTime() - new Date(d['startsAt']).getTime() > 3 * 3600000 ? 'Una reserva dura como máximo 3 horas.' : null
```

---

## 9. Cocina y delivery

Backend: **`preparacion/service/PreparacionService.java`**.

### NUEVA-39 · El costo de envío de una zona es de máximo S/ 15

**Backend** — método **`createZone`** (y igual en **`updateZone`**), justo después de `String name = str(data, "name");`:

```java
// REGLA NUEVA-39: el costo de envío es de máximo S/ 15.
if (money(data.get("fee"), "Tarifa").compareTo(new BigDecimal("15")) > 0) throw new IllegalArgumentException("El costo de envío es de máximo S/ 15.");
```

**Frontend** — `module-config.ts`, en `zoneForm`: `{ ...numberField('fee', 'Costo de envío (S/)'), max: 15 }`

### NUEVA-40 · El tiempo de entrega estimado es de máximo 60 minutos

**Backend** — mismo lugar que NUEVA-39:

```java
// REGLA NUEVA-40: el tiempo de entrega es de máximo 60 minutos.
if (integer(data.get("estimatedMinutes"), "Minutos") > 60) throw new IllegalArgumentException("El tiempo de entrega es de máximo 60 minutos.");
```

**Frontend** — `zoneForm`: `{ ...numberField('estimatedMinutes', 'Minutos aproximados', true, '1'), min: 1, max: 60 }`

### NUEVA-41 · Un repartidor lleva como máximo 3 entregas pendientes

**Backend** — método **`assignCourier`**, justo después de la línea `// REGLA DELIVERY-08 ...` y su `if`:

```java
// REGLA NUEVA-41: un repartidor lleva como máximo 3 entregas pendientes.
if (asignaciones.entregasPendientes(employee) >= 3) throw new IllegalStateException("Este repartidor ya tiene 3 entregas pendientes.");
```

(`entregasPendientes` ya existe en `preparacion/repository/AsignacionDespachoRepository.java`.)

**Frontend:** no hace falta.

---

## 10. Si el docente pide QUITAR o CAMBIAR una regla

- **Quitar:** busca la regla con Ctrl+Shift+F (por ejemplo `REGLA CAJA-03`) y borra el comentario y su `if`.
  Si en el frontend había un `max`, `min` o `validar` para esa regla, bórralo también.
- **Cambiar un número** (por ejemplo, 20 → 30 unidades): cambia el número **y** el mensaje en el backend,
  y el `max`/`validar` en el frontend.
- La lista completa de reglas existentes, con su archivo y línea, está en `Backend Inti Crunch/REGLAS.md`.

## 11. Si la regla necesita un dato nuevo (columna nueva)

Ejemplo: "cada proveedor debe tener un nombre de contacto". Sigue el **Ejemplo F** de `Reglas.md`:
`ALTER TABLE` en Oracle → campo en la **entity** → regla en el **service** → campo en el formulario de `module-config.ts`.

## 12. Errores comunes al pegar

| Error al compilar | Solución |
|---|---|
| `cannot find symbol: class LocalTime` (o `Duration`) | Agrega el import (sección 0). |
| `cannot find symbol: variable discount` (u otra) | Pegaste la regla antes de que exista esa variable: muévela más abajo. |
| `';' expected` | Te faltó un `;` o un paréntesis. |
| En el frontend: `',' expected` | Te faltó la coma antes de `validar`. |
| Dos `validar` en el mismo formulario | Únelos en uno: `validar: d => cond1 ? 'msg1' : cond2 ? 'msg2' : null` |
