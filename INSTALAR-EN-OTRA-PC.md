# Instalar Inti Crunch en otra PC (desde GitHub)

En la otra PC necesitas **4 cosas**: los programas, la base Oracle, Keycloak (inicio de sesión) y el código.
Sigue los pasos en orden.

---

## 1. Programas que hay que instalar

| Programa | Versión | Para qué |
|---|---|---|
| **Java JDK** | 21 | Ejecutar el backend |
| **Maven** | 3.9 o más | Compilar el backend (`mvn`) |
| **Node.js** | 24 | Ejecutar el frontend (`npm`) |
| **Oracle Database XE** | 21c | La base de datos |
| **Docker Desktop** | la última | Keycloak |
| **Git** | la última | Clonar el proyecto |

Para revisar que quedaron instalados, abre PowerShell y ejecuta `java -version`, `mvn -v`, `node -v`, `docker -v` y `git --version`.

---

## 2. Base de datos Oracle

Al instalar **Oracle XE 21c** te pide una clave para el administrador (SYS). Anótala.
La instalación crea la base **XE** y, dentro de ella, la base de trabajo **XEPDB1**: ahí van las tablas de Inti Crunch.

### 2.1 Comprobar que Oracle está escuchando

```powershell
lsnrctl status
```

Debe aparecer `El servicio "xepdb1" tiene 1 instancia(s)`. Si **no** aparece (error ORA-12514 al iniciar el backend):

```powershell
sqlplus / as sysdba
```
```sql
ALTER SYSTEM SET LOCAL_LISTENER='(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))' SCOPE=BOTH;
ALTER SYSTEM REGISTER;
EXIT;
```

Si el servicio `OracleOraDB21Home1TNSListener` está detenido, inícialo en PowerShell **como administrador**:
`Start-Service OracleOraDB21Home1TNSListener`.

> **Ojo:** si Docker tiene otro Oracle usando el puerto 1521 (por ejemplo de otro curso), detenlo con `docker stop <nombre>`.

### 2.2 Crear el usuario INTICRUNCH_ORACLE

Desde la carpeta del backend:

```powershell
cd "Backend Inti Crunch\infra\oracle"
sqlplus / as sysdba "@crear-usuario.sql"
```

Te pide la clave nueva de `INTICRUNCH_ORACLE`. Esa es la clave que escribirás al iniciar el backend.

### 2.3 Cargar los datos (vienen en el repositorio)

El repositorio trae **todos los datos** de la PC original (productos, pedidos, clientes, caja, compras…)
en `Backend Inti Crunch/infra/oracle/datos/inticrunch.dmp`.

En PowerShell **como administrador**, después del paso 2.2:

```powershell
cd "Backend Inti Crunch\infra\oracle"
Set-ExecutionPolicy -Scope Process Bypass
.\importar-datos.ps1
```

No pide clave (entra como administrador de Windows). Termina con `completed with 1 error(s)`:
ese único error es `ORA-31684 ... already exists` (el usuario ya lo creaste) y es normal.

> **Base vacía (opcional):** si prefieres empezar sin datos, en lugar de `importar-datos.ps1` ejecuta
> `sqlplus INTICRUNCH_ORACLE@localhost:1521/XEPDB1 "@esquema.sql"` y al terminar escribe `EXIT`.

### 2.4 Actualizar los datos del repositorio (en la PC original)

Cada vez que quieras llevar los datos más recientes, en la PC original y en PowerShell **como administrador**:

```powershell
cd "C:\LP2\LP2 - INTI CRUNCH SIMPLE\Backend Inti Crunch\infra\oracle"
Set-ExecutionPolicy -Scope Process Bypass
.\exportar-datos.ps1
```

Eso reemplaza `datos\inticrunch.dmp`. Luego súbelo: `git add .`, `git commit -m "Datos actualizados"`, `git push`.

---

## 3. Keycloak (inicio de sesión)

No hay que instalar nada aparte: `Backend Inti Crunch/docker-compose.yml` lo crea solo.
Con Docker Desktop abierto, `iniciar.ps1` lo enciende y la primera vez carga
`infra/keycloak/inti-crunch-realm.json`, que trae **las mismas cuentas y contraseñas de la PC original**:

| Usuario | Para qué |
|---|---|
| `admin1` | Administrador |
| `supervisor1` | Supervisor |
| `cajero1` | Caja |
| `operaciones1` | Cocina y delivery |
| `mercader1` | Compras / abastecimiento |
| `cliente1` | Cliente |
| `60554840` | Cliente (entra con su DNI) |

Las contraseñas son las mismas que usas hoy. Como los usuarios conservan su mismo código,
coinciden con los colaboradores y clientes que vienen en los datos de Oracle.

> Las cuentas que se creen **después** solo quedan en la PC donde se crearon.
> Para volver a copiarlas, pide que se exporte otra vez el realm de Keycloak.

---

## 4. Clonar y ejecutar

```powershell
git clone https://github.com/<tu-usuario>/<tu-repositorio>.git
cd "<tu-repositorio>"
```

**Backend** (primera terminal):

```powershell
cd "Backend Inti Crunch"
.\iniciar.ps1
```

Si PowerShell no deja ejecutar scripts, ejecuta antes:
`Set-ExecutionPolicy -Scope Process Bypass` (solo vale para esa ventana).

Está listo cuando aparece `Started AplicacionIntiCrunch`.

**Frontend** (segunda terminal; la primera vez descarga las librerías):

```powershell
cd "Frontend Inti Crunch"
npm install
npm start
```

Abre **http://localhost:4200** (clientes) y **http://localhost:4200/gestion/resumen** (personal).

---

## 5. Si algo falla

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| `ORA-12514 ... XEPDB1 no está registrado` | Oracle no escucha esa base | Paso 2.1 |
| `ORA-01017 usuario/contraseña no válidos` | Clave equivocada | Escríbela de nuevo (paso 2.2 si la olvidaste) |
| `ORA-00942 la tabla o vista no existe` | No se cargaron las tablas | Paso 2.3 |
| `Keycloak no responde en el puerto 8180` | Docker cerrado | Abre Docker Desktop y repite |
| `Port 8080 was already in use` | Ya hay un backend abierto | Ciérralo (Ctrl+C) o reinicia la PC |
| `Schema-validation: missing column` | La base es antigua | Ejecuta las líneas `ALTER TABLE` del final de `esquema.sql` |
