-- Ejecutar con SQL*Plus como SYSDBA. Crea un usuario NUEVO, nunca reemplaza uno existente.
-- Uso:  sqlplus / as sysdba "@crear-usuario.sql" XEPDB1      (Oracle XE 21c)
--       sqlplus / as sysdba "@crear-usuario.sql" FREEPDB1    (Oracle Free 23ai / 26ai)
-- O mas facil:  .\crear-usuario.ps1   (detecta solo cual es tu base)
SET ECHO OFF
SET VERIFY OFF
SET DEFINE ON
WHENEVER SQLERROR EXIT SQL.SQLCODE
ALTER SESSION SET CONTAINER = &1;
ACCEPT inti_password CHAR PROMPT 'Elige la clave local de INTICRUNCH_ORACLE (sin comillas dobles ni &): ' HIDE
CREATE USER INTICRUNCH_ORACLE IDENTIFIED BY "&inti_password"
  DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA 500M ON USERS;
UNDEFINE inti_password
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE TRIGGER TO INTICRUNCH_ORACLE;
PROMPT Usuario INTICRUNCH_ORACLE creado en &1..
PROMPT Conserva la clave localmente. No la pegues en el chat ni la subas a Git.
EXIT
