# Guía: cómo agregar, cambiar o quitar una regla de negocio

Una **regla de negocio** es una condición que el sistema exige antes de guardar algo.
Ejemplos: "no se puede cobrar un pedido cancelado" o "el DNI debe tener 8 números".
Si la condición no se cumple, el sistema **no guarda** y muestra un mensaje en la pantalla.

---

## 1. Cómo viaja un dato (la arquitectura)

```
PANTALLA (Angular)                       BACKEND (Spring Boot)                                BASE DE DATOS
module-config.ts  ──►  api.service.ts ──►  Controller  ──►  Service  ──►  Repository  ──►  Entity ──► tabla Oracle
(formulario)          (llama a la API)   (recibe la ruta)  (REGLAS)      (JPA: consultas)   (clase = tabla)
```

| Capa | ¿Qué hace? | ¿Dónde está? |
|---|---|---|
| **Controller** | Recibe la petición (`/orders`, `/admin/cash/...`) y llama al servicio. **No tiene reglas.** | `.../<modulo>/controller/` |
| **Service** | **Aquí van las reglas de negocio.** Revisa, calcula y guarda. | `.../<modulo>/service/` |
| **Repository** | Busca y guarda en Oracle con JPA (`findBy...`, `@Query`). | `.../<modulo>/repository/` |
| **Entity** | Una clase por tabla de Oracle (`@Entity @Table`). | `.../<modulo>/entity/` |

**Regla de oro:** una regla de negocio se escribe en el **Service**. En el frontend se puede repetir
para avisar antes, pero la que manda es la del backend.

Módulos del backend (`Backend Inti Crunch/src/main/java/pe/inticrunch/`):

| Módulo | Servicio principal | Pantalla en el frontend |
|---|---|---|
| Pedidos (ventas) | `pedidos/PedidoService.java` | panel "Ventas y pedidos" y la página pública (`portal/`) |
| Caja y pagos | `caja/service/CajaService.java` | panel "Caja y pagos" |
| Clientes y promociones | `clientes/service/ClienteService.java` | panel "Clientes y promociones" |
| Cocina y delivery | `preparacion/service/PreparacionService.java` | panel "Cocina y delivery" |
| Inventario | `inventario/service/InventarioService.java` | panel "Inventario y recetas" |
| Compras | `compras/service/ComprasService.java` | panel "Compras y proveedores" |
| Personal y turnos | `personal/service/PersonalService.java` | panel "Personal y turnos" |
| Reservas y mesas | `reservas/service/ReservaService.java` | panel "Reservas y mesas" y la página pública |
| Carta (productos) | `catalogo/CatalogoService.java` | panel "Ventas y pedidos" → Carta |
| Cuenta (registro/login) | `identidad/CuentaService.java` | ventana de iniciar sesión |

Todos los formularios del panel están en **un solo archivo**:
`Frontend Inti Crunch/src/app/features/operaciones/module-config.ts`.

---

## 2. Dónde están las reglas que ya existen

Todas están marcadas en el código con un comentario así:

```java
// REGLA CAJA-08: No se puede cobrar un pedido cancelado.
if (purchase.getStatus().equals("CANCELLED")) throw new IllegalStateException("No se puede cobrar un pedido cancelado.");
```

La lista completa (con archivo y línea) está en **`Backend Inti Crunch/REGLAS.md`**.
Para buscar una en VS Code: **Ctrl+Shift+F** → escribe `REGLA CAJA-08`.

---

## 3. Cómo se escribe una regla en el backend (siempre igual)

```java
// REGLA <MODULO>-<NUMERO>: <qué controla>
if (<condición que está MAL>) throw new IllegalArgumentException("<mensaje para el usuario>");
```

- `IllegalArgumentException` → el **dato** está mal (precio negativo, DNI corto). Responde error 400.
- `IllegalStateException` → la **situación** no lo permite (caja cerrada, pedido ya pagado). Responde error 409.

No hay que hacer nada más: `configuracion/ManejadorErrores.java` convierte la excepción en una
respuesta y **el frontend muestra el mensaje solo** (en rojo, en la misma ventana).

> ⚠️ El `if` lleva la condición **mala** (cuándo se debe impedir), no la buena.

---

## 4. Cómo se escribe una regla en el frontend (para avisar antes)

Hay tres formas, de la más fácil a la más completa. Todas en `module-config.ts`:

1. **Límites del campo** (número mínimo/máximo, largo del texto):
   ```ts
   { ...numberField('quantity', 'Cantidad', true, '1'), min: 1, max: 20 }
   { ...textField('dni', 'DNI', true), maxlength: 8 }
   ```
2. **Campo obligatorio**: el tercer valor de `textField(...)`/`numberField(...)` es `true`.
3. **Regla propia del formulario** con `validar` (devuelve el mensaje si está mal, o `null` si está bien):
   ```ts
   form: { title: 'Abrir caja', path: '/admin/cash/sessions', fields: [...],
           validar: d => d['openingAmount'] < 50 ? 'La caja se abre con al menos S/ 50.' : null }
   ```

---

## 5. Ejemplos completos

### Ejemplo A — Agregar: "Un pedido no puede llevar más de 20 unidades de un producto"

**Backend** — `pedidos/PedidoService.java`, método `create`, dentro del `for` de las líneas,
justo después de `int quantity = integer(detail.get("quantity"), "Cantidad");`:

```java
// REGLA PEDIDOS-04: máximo 20 unidades de un mismo producto.
if (quantity > 20) throw new IllegalArgumentException("Máximo 20 unidades por producto.");
```

**Frontend** — `module-config.ts`, en `saleForm` → `lineFields`, al campo cantidad agrégale `max: 20`:

```ts
{ ...numberField('quantity', 'Cantidad', true, '1'), min: 1, max: 20, initial: 1 }
```

En la página pública (carrito) no hace falta cambiar nada: si el cliente pide 21, el backend
responde con el mensaje y la pantalla lo muestra.

### Ejemplo B — Agregar: "La caja se abre con al menos S/ 50"

**Backend** — `caja/service/CajaService.java`, método `openCash`, antes de `SesionCaja s = new SesionCaja();`:

```java
BigDecimal fondo = money(data.get("openingAmount"), "Fondo inicial");
// REGLA CAJA-21: la caja se abre con al menos S/ 50.
if (fondo.compareTo(new BigDecimal("50")) < 0) throw new IllegalArgumentException("La caja se abre con al menos S/ 50.");
```

**Frontend** — `module-config.ts`, vista `sessions` (formulario "Abrir caja"):

```ts
{ ...numberField('openingAmount', 'Dinero inicial en efectivo (S/)'), min: 50 }
```

### Ejemplo C — Agregar: "Una reserva es para 10 personas como máximo"

La cantidad de personas se lee en tres métodos de `reservas/service/ReservaService.java`
(crear, editar y reserva del cliente), en la línea `int guests = integer(data.get("partySize"), "Personas");`.
Para no repetir, crea un método y llámalo en las tres:

```java
/** REGLA RESERVAS-20: una reserva es para 10 personas como máximo. */
private static void validarPersonas(int guests) {
    if (guests > 10) throw new IllegalArgumentException("Las reservas son para 10 personas como máximo.");
}
```

```java
int guests = integer(data.get("partySize"), "Personas");
validarPersonas(guests);
```

**Frontend** — en `module-config.ts`, en `reservationForm`, al campo `partySize` agrégale `max: 10`.
En la página pública, `features/portal/reservation.component.ts` línea 47, cambia `length: 16` por `length: 10`
(es la lista de 1 a 16 personas que se puede elegir).

### Ejemplo D — Cambiar: "La contraseña debe tener 6 caracteres (no 8)"

`identidad/CuentaService.java`, regla `CUENTA-06`: cambia `password.length() < 8` por `< 6`
y el mensaje "al menos 8" por "al menos 6". En el frontend, cambia el texto de ayuda (`hint`)
del campo contraseña en `module-config.ts` y en `shared/auth-dialog.component.ts`.

### Ejemplo E — Quitar: "Permitir retirar más efectivo del que hay en caja"

Busca `REGLA CAJA-03` y **borra esas dos líneas** (el comentario y el `if`):

```java
// REGLA CAJA-03: El retiro supera el efectivo esperado.
if (kind.equals("OUT") && expected(caja).compareTo(amount) < 0) throw new IllegalStateException("El retiro supera el efectivo esperado.");
```

Quitar una regla es borrar su `if`. Luego recompila y prueba. Si en el frontend había un `max`
o un `validar` para esa regla, bórralo también.

### Ejemplo F — Una regla que necesita un dato nuevo (una columna nueva)

Ejemplo: "cada proveedor debe tener un nombre de contacto".

1. **Oracle**: `ALTER TABLE suppliers ADD (contact_name VARCHAR2(100 CHAR));`
2. **Entity** `compras/entity/Proveedor.java`: agrega el campo con su getter y setter
   ```java
   @Column(name = "contact_name", length = 100)
   private String contactName;
   public String getContactName() { return contactName; }
   public void setContactName(String contactName) { this.contactName = contactName; }
   ```
3. **Service** `compras/service/ComprasService.java`, en `createSupplier` y `updateSupplier`:
   ```java
   // REGLA COMPRAS-30: el proveedor debe tener un contacto.
   s.setContactName(str(data, "contactName"));   // str(...) ya exige que no esté vacío
   ```
   y en `suppliers()` agrega la columna a la tabla: `"Contacto", s.getContactName()`.
4. **Frontend** `module-config.ts`, en `supplierForm` agrega `textField('contactName', 'Nombre de contacto', true)`.

---

## 6. Ayudantes para leer datos (ya hacen validaciones)

Están en `operaciones/Validaciones.java` y los usan todos los servicios:

| Método | Qué hace |
|---|---|
| `str(data, "campo")` | Texto **obligatorio** (si viene vacío, error). |
| `optional(data, "campo")` | Texto opcional (vacío si no viene). |
| `money(valor, "Nombre")` | Monto con hasta 2 decimales, no negativo. |
| `positive(valor, "Nombre")` | Número mayor que 0. |
| `integer(valor, "Nombre")` | Entero entre 1 y 10000. |
| `id(data, "campo")` | Número de identificador. |
| `choice(data, "campo", "A", "B")` | Solo acepta uno de esos valores. |
| `hoy()` | Fecha de hoy en Lima. |

---

## 7. Después de cada cambio

1. Detén el backend (Ctrl+C) y vuelve a ejecutar `.\iniciar.ps1` (compila de nuevo).
2. El frontend (`npm start`) se actualiza solo al guardar.
3. Prueba el caso **malo** (debe salir tu mensaje) y el caso **bueno** (debe guardar).
4. Si el compilador marca error, casi siempre falta un `;`, un paréntesis o el `import java.math.BigDecimal;`.
