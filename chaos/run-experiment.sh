#!/usr/bin/env bash
# Experimento de caos: inyecta latencia (y opcionalmente un corte) en la conexión
# catalog-service -> Elasticsearch usando la API de Toxiproxy, para observar cómo
# actúa la resiliencia (Resilience4j: retry, circuit breaker, fallback) del catálogo.
#
# Requisitos: el overlay de caos levantado (ver docker-compose.chaos.yml).
set -euo pipefail

TOXIPROXY_API="${TOXIPROXY_API:-http://localhost:8474}"
PROXY="catalog-elasticsearch"

echo "==> Estado inicial del proxy '$PROXY':"
curl -s "$TOXIPROXY_API/proxies/$PROXY" || { echo "¿Está Toxiproxy arriba?"; exit 1; }
echo

echo "==> Inyectando 1500ms de latencia en las respuestas de Elasticsearch..."
curl -s -X POST "$TOXIPROXY_API/proxies/$PROXY/toxics" \
  -H "Content-Type: application/json" \
  -d '{"name":"latency_down","type":"latency","stream":"downstream","attributes":{"latency":1500,"jitter":300}}' >/dev/null
echo "    Toxic 'latency_down' añadido."
echo
echo "    Ahora lanza búsquedas contra el catálogo (p. ej. GET /api/v1/books/search?q=java)"
echo "    y observa en /actuator/health o en Grafana cómo el circuit breaker 'bookSearch'"
echo "    pasa a OPEN y responde con el fallback (resultado vacío) en vez de colgarse."
echo
read -r -p "Pulsa ENTER para RETIRAR el fallo y volver a la normalidad..." _

echo "==> Eliminando el toxic..."
curl -s -X DELETE "$TOXIPROXY_API/proxies/$PROXY/toxics/latency_down" >/dev/null
echo "    Conexión restablecida. Experimento terminado."
