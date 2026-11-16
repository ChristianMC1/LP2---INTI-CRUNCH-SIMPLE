# Inicia el backend de Inti Crunch (version simple) con Oracle.
# Uso (desde esta carpeta):  .\iniciar.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# 1. Keycloak (inicio de sesion) en el puerto 8180.
$baseKc = 'inticrunch-identidad-oracle-oracle-1'
$keycloakOracle = 'inticrunch-identidad-oracle-keycloak-1'
if (docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq $baseKc }) {
    # PC original: Keycloak guardado en su propio Oracle. Primero su base ("healthy") y luego Keycloak.
    docker start $baseKc | Out-Null
    Write-Host 'Esperando la base de datos de Keycloak ...'
    for ($i = 0; $i -lt 100; $i++) {
        if ((docker inspect -f '{{.State.Health.Status}}' $baseKc) -eq 'healthy') { break }
        Start-Sleep -Seconds 3
    }
    docker start $keycloakOracle | Out-Null
} else {
    # PC nueva: Keycloak de este proyecto (docker-compose.yml).
    docker compose up -d keycloak
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar Keycloak. Abre Docker Desktop y vuelve a intentarlo.' }
}
Write-Host 'Esperando a Keycloak en http://localhost:8180 ...'
$listo = $false
for ($i = 0; $i -lt 60; $i++) {
    try { Invoke-WebRequest 'http://localhost:8180/realms/inti-crunch/.well-known/openid-configuration' -UseBasicParsing -TimeoutSec 3 | Out-Null; $listo = $true; break }
    catch { Start-Sleep -Seconds 3 }
}
if (-not $listo) { throw 'Keycloak no responde en el puerto 8180. Revisa Docker Desktop.' }

# 2. Clave de Oracle (no se guarda en ningun archivo).
$clave = Read-Host 'Clave de INTICRUNCH_ORACLE' -AsSecureString
$env:ORACLE_DATABASE_PASSWORD = (New-Object System.Management.Automation.PSCredential('x', $clave)).GetNetworkCredential().Password

# 3. Compilar y ejecutar.
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el backend.' }
Write-Host 'Iniciando backend en http://localhost:8080/api/v1  (para detener: Ctrl+C)'
java -jar target\inti-crunch-api-0.1.0.jar
