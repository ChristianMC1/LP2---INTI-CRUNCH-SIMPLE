# Inti Crunch — versión simple

Misma aplicación y mismo diseño que el proyecto original, con el backend reducido a lo esencial:
**solo Oracle**, sin Swagger, sin PostgreSQL, sin scripts de migración ni pruebas automáticas.
Cada regla de negocio está marcada en el código con `// REGLA ...`.

- **Guía para agregar, cambiar o quitar reglas:** [GUIA-REGLAS-DE-NEGOCIO.md](GUIA-REGLAS-DE-NEGOCIO.md)
- **Lista de todas las reglas:** [Backend Inti Crunch/REGLAS.md](<Backend Inti Crunch/REGLAS.md>)
- **Instalar en otra PC (clonando desde GitHub):** [INSTALAR-EN-OTRA-PC.md](INSTALAR-EN-OTRA-PC.md)

## Cómo iniciar

> No ejecutes este proyecto y el original al mismo tiempo: los dos usan el puerto 8080 y la misma base de Oracle.

1. Abre Docker Desktop (para Keycloak, el inicio de sesión).
2. Backend, en una terminal:
   ```powershell
   cd "C:\LP2\LP2 - INTI CRUNCH SIMPLE\Backend Inti Crunch"
   .\iniciar.ps1
   ```
   Pide la clave de `INTICRUNCH_ORACLE`. Queda en http://localhost:8080/api/v1
3. Frontend, en otra terminal:
   ```powershell
   cd "C:\LP2\LP2 - INTI CRUNCH SIMPLE\Frontend Inti Crunch"
   npm start
   ```
   Abre http://localhost:4200 (página de clientes) y http://localhost:4200/gestion/resumen (panel del personal).

## Estructura

```
Backend Inti Crunch/
  iniciar.ps1                    ← arranca todo
  REGLAS.md                      ← lista de reglas de negocio
  infra/oracle/esquema.sql       ← tablas de Oracle (solo para una base nueva)
  src/main/resources/application.yml   ← conexión a Oracle y Keycloak
  src/main/java/pe/inticrunch/
    <modulo>/controller/   rutas de la API (sin reglas)
    <modulo>/service/      REGLAS DE NEGOCIO
    <modulo>/repository/   consultas a Oracle con JPA
    <modulo>/entity/       una clase por tabla
    configuracion/         seguridad, errores, datos de la empresa
    operaciones/           panel de control, listas y ayudantes (Validaciones, Filas)

Frontend Inti Crunch/src/app/
  features/operaciones/module-config.ts   ← TODOS los formularios y tablas del panel
  features/operaciones/workspace.*        ← pantalla del panel
  features/portal/                        ← página pública, carrito y reservas
  features/<modulo>/<modulo>.service.ts   ← llamadas a la API
  shared/form-dialog.component.ts         ← ventana de formulario (muestra los errores)
  core/                                   ← sesión (Keycloak), API y utilidades
```

Módulos: caja, catalogo, clientes, compras, identidad (cuenta), inventario, operaciones,
pedidos, personal, preparacion (cocina y delivery) y reservas.
