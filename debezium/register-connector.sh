#!/bin/sh
# Registra el conector Debezium en Kafka Connect (REST API en localhost:8083).
# Requiere el stack levantado (kafka-connect, order-postgres).
set -e

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"

echo "Esperando a Kafka Connect en $CONNECT_URL ..."
until curl -sf "$CONNECT_URL/connectors" >/dev/null 2>&1; do
  sleep 3
done

echo "Registrando order-outbox-connector ..."
curl -sf -X POST "$CONNECT_URL/connectors" \
  -H "Content-Type: application/json" \
  -d @"$(dirname "$0")/order-outbox-connector.json"

echo
echo "Estado del conector:"
curl -sf "$CONNECT_URL/connectors/order-outbox-connector/status"
