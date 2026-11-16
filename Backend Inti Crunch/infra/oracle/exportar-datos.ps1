# Exporta TODOS los datos de Inti Crunch (esquema INTICRUNCH_ORACLE) a infra\oracle\datos\inticrunch.dmp
# Ejecutar en la PC ORIGINAL, en PowerShell COMO ADMINISTRADOR, desde esta carpeta:  .\exportar-datos.ps1
# No pide clave: crea un usuario temporal IC_COPIA (clave aleatoria), exporta con el y lo borra al final.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. (Join-Path $PSScriptRoot 'base-oracle.ps1')
$base = Obtener-BaseOracle   # XEPDB1 (Oracle XE) o FREEPDB1 (Oracle Free)
Write-Host "Base de trabajo de Oracle: $base"
$clave = 'Ic' + [guid]::NewGuid().ToString('N').Substring(0, 24)

function Ejecutar-ComoAdmin([string]$sql) {
    $salida = ("`n" + $sql) | sqlplus -S / as sysdba   # la primera linea vacia evita problemas de codificacion
    if ($salida -match 'ORA-') { $salida; throw 'Oracle rechazo el comando. Abre PowerShell como administrador y vuelve a intentarlo.' }
    return $salida
}

# 1. Usuario temporal para exportar y carpeta donde Oracle deja el archivo.
$salida = Ejecutar-ComoAdmin @"
SET HEADING OFF FEEDBACK OFF PAGESIZE 0 LINESIZE 400
ALTER SESSION SET CONTAINER = $base;
BEGIN EXECUTE IMMEDIATE 'DROP USER IC_COPIA CASCADE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
CREATE USER IC_COPIA IDENTIFIED BY "$clave" DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, DATAPUMP_EXP_FULL_DATABASE TO IC_COPIA;
GRANT READ, WRITE ON DIRECTORY DATA_PUMP_DIR TO IC_COPIA;
SELECT directory_path FROM dba_directories WHERE directory_name = 'DATA_PUMP_DIR';
EXIT;
"@
$carpetaOracle = ($salida | Where-Object { $_ -match '^\s*[A-Za-z]:\\' } | Select-Object -Last 1).Trim()
Write-Host "Carpeta de Oracle: $carpetaOracle"

$parametros = Join-Path $env:TEMP 'inticrunch-exportar.par'
try {
    # 2. Exportar el esquema completo.
    @(
        "userid=IC_COPIA/$clave@localhost:1521/$base",
        'schemas=INTICRUNCH_ORACLE',
        'directory=DATA_PUMP_DIR',
        'dumpfile=inticrunch.dmp',
        'logfile=exportar.log',
        'reuse_dumpfiles=yes'
    ) | Set-Content -Path $parametros -Encoding Ascii
    expdp parfile="$parametros"
    if ($LASTEXITCODE -ne 0) { throw 'La exportacion fallo. Revisa el mensaje anterior.' }

    # 3. Copiar el archivo al proyecto (para subirlo a GitHub).
    New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot 'datos') | Out-Null
    Copy-Item (Join-Path $carpetaOracle 'inticrunch.dmp') (Join-Path $PSScriptRoot 'datos\inticrunch.dmp') -Force
    Write-Host 'Listo: infra\oracle\datos\inticrunch.dmp (subelo a GitHub con git add / commit / push).'
} finally {
    Remove-Item $parametros -ErrorAction SilentlyContinue
    Ejecutar-ComoAdmin "ALTER SESSION SET CONTAINER = $base;`nDROP USER IC_COPIA CASCADE;`nEXIT;" | Out-Null
}
