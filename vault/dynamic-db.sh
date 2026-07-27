#!/bin/sh
# Configura el motor de secretos DINÁMICOS de base de datos de Vault para el
# auth-service. En vez de una contraseña fija, Vault GENERA credenciales de
# PostgreSQL temporales bajo demanda (TTL 1h) y las revoca al expirar el lease.
# Lo ejecuta el contenedor vault-db-init cuando Vault y auth-postgres están listos.
set -e

export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"

echo "Esperando a que Vault responda en $VAULT_ADDR ..."
until vault status >/dev/null 2>&1; do
  sleep 2
done

# 1) Habilita el motor "database" (idempotente).
vault secrets enable -path=database database 2>/dev/null || true

# 2) Configura la conexión de Vault a auth-postgres usando un usuario administrador.
#    {{username}}/{{password}} son plantillas: Vault los sustituye al conectarse.
vault write database/config/auth-postgres \
  plugin_name=postgresql-database-plugin \
  allowed_roles="auth-role" \
  connection_url="postgresql://{{username}}:{{password}}@auth-postgres:5432/auth_db?sslmode=disable" \
  username="auth_user" \
  password="auth_pass"

# 3) Define el rol que emite credenciales efímeras: crea un usuario de PostgreSQL
#    con contraseña aleatoria y caducidad, con permisos DML sobre el esquema.
vault write database/roles/auth-role \
  db_name=auth-postgres \
  creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT USAGE ON SCHEMA public TO \"{{name}}\"; GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\";" \
  revocation_statements="ALTER ROLE \"{{name}}\" NOLOGIN;" \
  default_ttl="1h" \
  max_ttl="24h"

echo "Secretos dinámicos listos: pide credenciales con 'vault read database/creds/auth-role'."
