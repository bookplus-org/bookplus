#!/bin/sh
# Registra los esquemas Avro en Apicurio Registry y fija la regla de compatibilidad.
# Requiere el stack levantado (schema-registry).
set -e

REGISTRY="${REGISTRY_URL:-http://localhost:8090}"
API="$REGISTRY/apis/registry/v2"
DIR="$(dirname "$0")/avro"

echo "Esperando a Apicurio en $REGISTRY ..."
until curl -sf "$API/groups/default/artifacts" >/dev/null 2>&1; do
  sleep 3
done

echo "Registrando esquema cart.checked-out-value ..."
curl -sf -X POST "$API/groups/default/artifacts" \
  -H "Content-Type: application/json; artifactType=AVRO" \
  -H "X-Registry-ArtifactId: cart.checked-out-value" \
  --data-binary @"$DIR/cart-checked-out.avsc" >/dev/null

# Compatibilidad hacia atrás: un esquema nuevo debe poder leer datos del anterior.
echo "Fijando regla de compatibilidad BACKWARD ..."
curl -sf -X POST "$API/groups/default/artifacts/cart.checked-out-value/rules" \
  -H "Content-Type: application/json" \
  -d '{"type":"COMPATIBILITY","config":"BACKWARD"}' >/dev/null || true

echo "Listo. Esquemas en $REGISTRY/ui"
