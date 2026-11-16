# Instalar Inti Crunch en otra PC (paso a paso)

Sigue los pasos **en este orden**. Cada paso dice **dónde** escribir, **qué** escribir y **qué debes ver**.

| Paso | Qué haces | Cuánto se hace |
|---|---|---|
| 1 | Instalar los programas | Una sola vez |
| 2 | Clonar el proyecto desde GitHub | Una sola vez |
| 3 | Revisar que Oracle está listo | Una sola vez |
| 4 | Crear el usuario de Oracle | Una sola vez |
| 5 | Cargar los datos | Una sola vez |
| 6 | Abrir Docker Desktop | Cada vez que uses el sistema |
| 7 | Iniciar el backend | Cada vez |
| 8 | Iniciar el frontend | Cada vez |
| 9 | Entrar al sistema | Cada vez |

---

## Paso 1 · Instalar los programas

| Programa | Versión | Para qué sirve |
|---|---|---|
| **Git** | la última | Descargar el proyecto de GitHub |
| **Java JDK** | 21 | Ejecutar el backend |
| **Maven** | 3.9 o más | Compilar el backend |
| **Node.js** | 24 | Ejecutar el frontend |
| **Docker Desktop** | la última | Inicio de sesión (Keycloak) |
| **Oracle Database** | **XE 21c** o **Free 23ai / 26ai** (cualquiera) | La base de datos |

Al instalar Oracle te pide una clave para el administrador (SYS): **anótala**.

**Comprobar:** abre **PowerShell** y escribe, uno por uno:

```powershell
git --version
java -version
mvn -v
node -v
docker -v
```

Cada uno debe mostrar un número de versión. Si alguno dice "no se reconoce", vuelve a instalar ese programa.

---

## Paso 2 · Clonar el proyecto desde GitHub

En **PowerShell** (normal):

```powershell
mkdir C:\Proyectos
cd C:\Proyectos
git clone https://github.com/ChristianMC1/LP2---INTI-CRUNCH-SIMPLE.git
cd LP2---INTI-CRUNCH-SIMPLE
dir
```

**Debes ver** las carpetas `Backend Inti Crunch` y `Frontend Inti Crunch`, y los archivos `.md`.

> Desde aquí, la carpeta del proyecto es **`C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE`**.
> Si lo clonaste en otro lugar, cambia esa ruta en los comandos de abajo.

---

## Paso 3 · Revisar que Oracle está listo

Abre **PowerShell como administrador** (clic derecho en PowerShell → *Ejecutar como administrador*).
Usa **esta misma ventana** para los pasos 3, 4 y 5.

```powershell
lsnrctl status
```

**Debes ver**, al final, una de estas líneas con estado **READY**:

| Si tienes… | Debe aparecer |
|---|---|
| Oracle XE 21c | `El servicio "xepdb1" tiene 1 instancia(s)` |
| Oracle Free 23ai / 26ai | `El servicio "freepdb1" tiene 1 instancia(s)` |

- **Si aparece:** todo bien, pasa al **Paso 4**.
- **Si no aparece:** escribe esto y vuelve a ejecutar `lsnrctl status`:
  ```powershell
  sqlplus / as sysdba
  ```
  ```sql
  ALTER SYSTEM SET LOCAL_LISTENER='(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))' SCOPE=BOTH;
  ALTER SYSTEM REGISTER;
  EXIT;
  ```
- **Si dice que no hay listener:** busca el nombre de su servicio y enciéndelo:
  ```powershell
  Get-Service *TNSListener*
  Start-Service <el nombre que salió>
  ```

> Si otro Oracle de Docker usa el puerto 1521 (por ejemplo de otro curso), detenlo: `docker stop <nombre>`.

---

## Paso 4 · Crear el usuario de Oracle

En la **misma PowerShell de administrador**:

```powershell
cd "C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE\Backend Inti Crunch\infra\oracle"
Set-ExecutionPolicy -Scope Process Bypass
.\crear-usuario.ps1
```

**Debes ver:** `Base de trabajo de Oracle: XEPDB1` (o `FREEPDB1`) y luego te pide:

```
Elige la clave local de INTICRUNCH_ORACLE (sin comillas dobles ni &):
```

Escribe una clave (no se ve mientras escribes) y presiona Enter.
**Debes ver:** `Usuario INTICRUNCH_ORACLE creado en ...`

> **Anota esa clave.** La vas a escribir cada vez que inicies el backend (Paso 7).

---

## Paso 5 · Cargar los datos

En la **misma PowerShell de administrador** (sigues en la carpeta `infra\oracle`):

```powershell
.\importar-datos.ps1
```

No pide clave. Tarda menos de un minuto.
**Debes ver** al final: `completed with 1 error(s)` y `Importacion terminada`.
Ese único error es `ORA-31684 ... already exists` (el usuario ya existe del Paso 4) y **es normal**.

Ya puedes cerrar esta ventana de administrador.

---

## Paso 6 · Abrir Docker Desktop

Abre **Docker Desktop** y espera a que diga *Engine running* (abajo a la izquierda).
Docker se encarga del inicio de sesión (Keycloak); el Paso 7 lo enciende solo.

---

## Paso 7 · Iniciar el backend

Abre una **PowerShell normal** (terminal 1):

```powershell
cd "C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE\Backend Inti Crunch"
Set-ExecutionPolicy -Scope Process Bypass
.\iniciar.ps1
```

Qué pasa, en orden:

1. `Esperando a Keycloak en http://localhost:8180 ...` (la **primera vez** descarga Keycloak: unos minutos).
2. `Oracle: jdbc:oracle:thin:@//localhost:1521/XEPDB1` (o `FREEPDB1`).
3. `Clave de INTICRUNCH_ORACLE:` → escribe la clave del **Paso 4** y Enter.
4. Compila (la **primera vez** descarga librerías: unos minutos).
5. **Debes ver:** `Started AplicacionIntiCrunch in ... seconds`.

**No cierres esta ventana** mientras uses el sistema. Para apagar el backend: **Ctrl+C**.

---

## Paso 8 · Iniciar el frontend

Abre **otra PowerShell normal** (terminal 2):

```powershell
cd "C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE\Frontend Inti Crunch"
npm install
npm start
```

`npm install` solo hace falta **la primera vez** (descarga librerías, unos minutos).
**Debes ver:** `Local: http://localhost:4200/`. No cierres esta ventana.

---

## Paso 9 · Entrar al sistema

| Página | Dirección |
|---|---|
| Clientes (carta, pedidos, reservas) | http://localhost:4200 |
| Personal (panel de los módulos) | http://localhost:4200/gestion/resumen |

Cuentas (las mismas de la PC original, con **las mismas contraseñas**):

| Usuario | Rol |
|---|---|
| `admin1` | Administrador |
| `supervisor1` | Supervisor |
| `cajero1` | Caja |
| `operaciones1` | Cocina y delivery |
| `mercader1` | Compras / abastecimiento |
| `cliente1` | Cliente |
| `60554840` | Cliente (entra con su DNI) |

---

## La próxima vez (uso diario)

Solo los pasos **6, 7 y 8**, y en el 8 **sin** `npm install`:

1. Abre Docker Desktop.
2. Terminal 1: `cd "C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE\Backend Inti Crunch"` → `Set-ExecutionPolicy -Scope Process Bypass` → `.\iniciar.ps1`
3. Terminal 2: `cd "C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE\Frontend Inti Crunch"` → `npm start`

---

## Si algo falla

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| `... no se reconoce como nombre de un cmdlet` (`git`, `java`, `mvn`, `node`) | Falta instalar ese programa | Paso 1 |
| `No se puede cargar el archivo ... .ps1 porque la ejecución de scripts está deshabilitada` | PowerShell bloquea scripts | Ejecuta `Set-ExecutionPolicy -Scope Process Bypass` y repite |
| `Oracle no muestra XEPDB1 ni FREEPDB1` | Oracle no está escuchando | Paso 3 |
| `ORA-12514 ... no está registrado con el listener` | Igual que el anterior | Paso 3 |
| `ORA-01031: privilegios insuficientes` | La PowerShell no es de administrador | Abre PowerShell **como administrador** |
| `ORA-01920: el nombre de usuario ... está en conflicto` | El usuario ya existe (el Paso 4 ya se hizo) | Sigue con el Paso 5 |
| `ORA-30094 failed to find the time zone data file` | A tu Oracle le faltan archivos de zona horaria | Instala Oracle completo (no una versión recortada) |
| `ORA-01017: nombre de usuario/contraseña no válidos` | Clave equivocada en el Paso 7 | Escríbela de nuevo. Si la olvidaste, ver abajo |
| `Keycloak no responde en el puerto 8180` | Docker está cerrado | Abre Docker Desktop (Paso 6) y repite el Paso 7 |
| `Port 8080 was already in use` | Ya hay un backend abierto | Ciérralo con Ctrl+C o reinicia la PC |

**Olvidé la clave de INTICRUNCH_ORACLE.** En PowerShell **como administrador**
(usa `XEPDB1` o `FREEPDB1` según tu Oracle):

```powershell
sqlplus / as sysdba
```
```sql
ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER USER INTICRUNCH_ORACLE IDENTIFIED BY "NuevaClave2026";
ALTER USER INTICRUNCH_ORACLE ACCOUNT UNLOCK;
EXIT;
```

---

## Extra A · Bajar los últimos cambios del proyecto

Si en la PC original se subieron cambios nuevos, en esta PC:

```powershell
cd C:\Proyectos\LP2---INTI-CRUNCH-SIMPLE
git pull
```

Luego inicia de nuevo el backend (Paso 7).

## Extra B · Llevar datos más recientes (desde la PC original)

En la **PC original**, en PowerShell **como administrador**:

```powershell
cd "C:\LP2\LP2 - INTI CRUNCH SIMPLE\Backend Inti Crunch\infra\oracle"
Set-ExecutionPolicy -Scope Process Bypass
.\exportar-datos.ps1
cd "C:\LP2\LP2 - INTI CRUNCH SIMPLE"
git add .
git commit -m "Datos actualizados"
git push
```

En **esta PC**: `git pull` y vuelve a cargar los datos. Para reemplazarlos por completo, primero borra el usuario
(en PowerShell de administrador: `sqlplus / as sysdba`, luego `ALTER SESSION SET CONTAINER = FREEPDB1;`
—o `XEPDB1`—, `DROP USER INTICRUNCH_ORACLE CASCADE;` y `EXIT;`) y repite los **Pasos 4 y 5**.

## Extra C · Empezar con la base vacía (sin datos)

En lugar del Paso 5, en la carpeta `infra\oracle` (usa `XEPDB1` o `FREEPDB1` según tu Oracle):

```powershell
sqlplus INTICRUNCH_ORACLE@localhost:1521/FREEPDB1 "@esquema.sql"
```

Te pide la clave del Paso 4. Al terminar escribe `EXIT`.
Las cuentas del Paso 9 funcionan, pero la carta estará vacía: crea los productos en **Ventas y pedidos → Carta**.
