# Pruebas de carga con k6

Las pruebas de carga miden el comportamiento del sistema **bajo concurrencia**: cuántas
peticiones por segundo aguanta, la latencia (p95, p99) y la tasa de error según sube la
carga. Son requisitos **no funcionales** obligatorios en banca y grandes empresas (SLAs,
capacity planning).

[k6](https://k6.io) es una herramienta moderna de carga: los escenarios se escriben en
JavaScript y se ejecutan desde un único binario o contenedor.

## Qué se ha añadido

- **Script** `perf/k6/load-test.js` — simula usuarios navegando el catálogo a través de la
  API Gateway: lista categorías, lista libros, busca, y abre el detalle y las reseñas de un
  libro. Define:
  - **Stages**: rampa hasta 20 usuarios virtuales, sostiene 1 min, baja.
  - **Thresholds** (criterios de aprobado/fallo): `http_req_failed < 1%` y `p95 < 800 ms`.
    Si no se cumplen, k6 termina con código de error (útil en CI).
- **Servicio `k6`** en `docker-compose.full.yml`, bajo el perfil `perf` para que no arranque
  con el stack normal.

## Cómo ejecutarlo

Con el stack levantado:

```bash
# Opción A — dentro de la red de Docker (apunta al gateway interno)
docker compose -f docker-compose.full.yml --profile perf run --rm k6

# Opción B — k6 local contra el gateway publicado
k6 run -e BASE_URL=http://localhost:8080/api/v1 perf/k6/load-test.js
```

## Cómo leer el resultado

k6 imprime un resumen con, entre otras:

- `http_reqs` — total y peticiones/segundo (throughput).
- `http_req_duration` — avg, p90, **p95**, p99 (latencia).
- `http_req_failed` — % de peticiones con error.
- `checks` — % de validaciones superadas.
- El estado de cada **threshold** (✓/✗). Si alguno falla, el exit code ≠ 0.

## Siguiente paso

- Añadir un escenario autenticado (login → añadir al carrito → checkout) para medir el
  camino de escritura del saga.
- Ejecutar k6 en el pipeline de CI con thresholds como “gate” de rendimiento, y/o enviar
  las métricas a Prometheus/Grafana para ver la latencia en tiempo real durante la prueba.
