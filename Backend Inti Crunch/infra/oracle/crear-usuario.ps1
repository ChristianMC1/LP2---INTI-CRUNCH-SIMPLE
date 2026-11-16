# Crea el usuario INTICRUNCH_ORACLE en la base de trabajo (XEPDB1 o FREEPDB1, se detecta sola).
# Ejecutar en PowerShell, desde esta carpeta:  .\crear-usuario.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
. (Join-Path $PSScriptRoot 'base-oracle.ps1')
$base = Obtener-BaseOracle
Write-Host "Base de trabajo de Oracle: $base"
sqlplus / as sysdba "@crear-usuario.sql" $base
