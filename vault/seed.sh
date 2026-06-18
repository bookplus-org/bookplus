#!/bin/sh
# Carga los secretos de BookPlus en Vault (KV v2, motor "secret/").
# Lo ejecuta el contenedor vault-init una vez Vault está listo.
set -e

export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"

echo "Esperando a que Vault responda en $VAULT_ADDR ..."
until vault status >/dev/null 2>&1; do
  sleep 2
done

put() {
  svc="$1"; user="$2"; pass="$3"
  echo "  secret/bookplus/$svc"
  vault kv put "secret/bookplus/$svc" \
    spring.datasource.username="$user" \
    spring.datasource.password="$pass" >/dev/null
}

echo "Cargando secretos de la base de datos de cada servicio..."
put auth         bookplus            bookplus123
put catalog      catalog_user        catalog_pass
put inventory    inventory_user      inventory_pass
put order        order_user          order_pass
put payment      payment_user        payment_pass
put notification notification_user   notification_pass
put report       report_user         report_pass

echo "Secretos cargados correctamente."
