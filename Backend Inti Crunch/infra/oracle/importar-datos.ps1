# Importa los datos de Inti Crunch desde infra\oracle\datos\inticrunch.dmp
# Ejecutar en la PC NUEVA, en PowerShell COMO ADMINISTRADOR, desde esta carpeta:  .\importar-datos.ps1
# Antes: haber creado el usuario con crear-usuario.sql (y NO haber ejecutado esquema.sql).
# No pide clave: crea un usuario temporal IC_COPIA (clave aleatoria), importa con el y lo borra al final.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. (Join-Path $PSScriptRoot 'base-oracle.ps1')
$base = Obtener-BaseOracle   # XEPDB1 (Oracle XE) o FREEPDB1 (Oracle Free)
Write-Host "Base de trabajo de Oracle: $base"
$archivo = Join-Path $PSScriptRoot 'datos\inticrunch.dmp'
if (-not (Test-Path $archivo)) { throw 'No existe datos\inticrunch.dmp' }
$clave = 'Ic' + [guid]::NewGuid().ToString('N').Substring(0, 24)

function Ejecutar-ComoAdmin([string]$sql) {
    $salida = ("`n" + $sql) | sqlplus -S / as sysdba   # la primera linea vacia evita problemas de codificacion
    if ($salida -match 'ORA-') { $salida; throw 'Oracle rechazo el comando. Abre PowerShell como administrador y vuelve a intentarlo.' }
    return $salida
}

# 1. Usuario temporal para importar y copia del archivo a la carpeta de Oracle.
$salida = Ejecutar-ComoAdmin @"
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 LINESIZE 400
ALTER SESSION SET CONTAINER = $base;
BEGIN EXECUTE IMMEDIATE 'DROP USER IC_COPIA CASCADE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
CREATE USER IC_COPIA IDENTIFIED BY "$clave" DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, DATAPUMP_IMP_FULL_DATABASE TO IC_COPIA;
GRANT READ, WRITE ON DIRECTORY DATA_PUMP_DIR TO IC_COPIA;
SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';
EXIT;
"@
$carpetaOracle = ($salida | Where-Object { $_ -match '^\s*[A-Za-z]:\\' } | Select-Object -Last 1).Trim()
New-Item -ItemType Directory -Force $carpetaOracle | Out-Null
Copy-Item $archivo (Join-Path $carpetaOracle 'inticrunch.dmp') -Force
Write-Host "Archivo copiado a: $carpetaOracle"

$parametros = Join-Path $env:TEMP 'inticrunch-importar.par'
try {
    # 2. Importar. El aviso "ORA-31684 ... INTICRUNCH_ORACLE already exists" es normal: el usuario ya lo creaste.
    @(
        "userid=IC_COPIA/$clave@localhost:1521/$base",
        'schemas=INTICRUNCH_ORACLE',
        'directory=DATA_PUMP_DIR',
        'dumpfile=inticrunch.dmp',
        'logfile=importar.log'
    ) | Set-Content -Path $parametros -Encoding Ascii
    impdp parfile="$parametros"
    Write-Host 'Importacion terminada. Si el unico error fue ORA-31684 (el usuario ya existe), todo esta bien.'
} finally {
    Remove-Item $parametros -ErrorAction SilentlyContinue
    Ejecutar-ComoAdmin "ALTER SESSION SET CONTAINER = $base;`nDROP USER IC_COPIA CASCADE;`nEXIT;" | Out-Null
}
