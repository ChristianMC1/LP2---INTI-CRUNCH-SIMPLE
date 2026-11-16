# Averigua el nombre de la base de trabajo de Oracle en esta PC:
#   Oracle XE 21c            -> XEPDB1
#   Oracle Free 23ai / 26ai  -> FREEPDB1
# Lo usan los demas scripts (no hace falta ejecutarlo solo).
function Obtener-BaseOracle {
    if ($env:ORACLE_PDB) { return $env:ORACLE_PDB.ToUpper() }   # para forzar otro nombre si hiciera falta
    $estado = (lsnrctl status) -join "`n"
    if ($estado -match '"xepdb1"') { return 'XEPDB1' }
    if ($estado -match '"freepdb1"') { return 'FREEPDB1' }
    throw 'Oracle no muestra XEPDB1 ni FREEPDB1. Ejecuta "lsnrctl status" y revisa el paso 2.1 de INSTALAR-EN-OTRA-PC.md.'
}
