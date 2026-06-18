#!/bin/sh
# Carga los secretos de BookPlus en Vault (KV v2, motor "secret/").
# Lo ejecuta el contenedor vault-init una vez Vault está listo.
set -e

export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"

echo "Esperando a que Vault responda en $VAULT_ADDR ..."
until vault status >/dev/null 2>&1; do
  sleep 2
done

echo "Cargando secretos de auth-service en secret/bookplus/auth ..."
vault kv put secret/bookplus/auth \
  spring.datasource.username=bookplus \
  spring.datasource.password=bookplus123

echo "Secretos cargados correctamente."
vault kv get secret/bookplus/auth
