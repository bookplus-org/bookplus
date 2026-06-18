# Change Data Capture (CDC) con Debezium

**CDC** captura los cambios de una base de datos (inserts/updates/deletes) leyendo su log de
transacciones y los publica como eventos, **sin tocar el código de la aplicación** y sin
hacer polling. Es una pieza clave en banca y grandes empresas para integrar sistemas (incluso
legados) y alimentar pipelines de datos en tiempo real.

En BookPlus se aplica al **patrón Outbox**: order-service ya escribe sus eventos de dominio en
la tabla `outbox_events` dentro de la misma transacción que el agregado. Debezium captura esas
inserciones por CDC y las publica a Kafka — la forma "pro" del outbox, que sustituye al
*polling* del `OutboxRelay`.

## Qué se ha añadido

- **`kafka-connect`** (imagen `debezium/connect`) en `docker-compose.full.yml`: el runtime de
  Kafka Connect con los conectores Debezium, REST API en el puerto `8083`.
- **`order-postgres`** arranca con `wal_level=logical` para permitir la replicación lógica que
  Debezium necesita (vía `pgoutput`).
- **`debezium/order-outbox-connector.json`**: el conector con el **Outbox Event Router**, que
  mapea las columnas de `outbox_events` (`id`, `aggregate_id`, `event_type`, `payload`,
  `created_at`) y **enruta cada evento al topic indicado en la columna `topic`** (p. ej.
  `order.created`, `order.status.changed`). Así los consumidores actuales no cambian.
- **`debezium/register-connector.sh`**: registra el conector en Kafka Connect.

## Cómo activarlo

```bash
# 1) Levantar el stack (incluye kafka-connect)
docker compose -f docker-compose.full.yml up -d

# 2) Registrar el conector
sh debezium/register-connector.sh
```

A partir de ahí, cada fila nueva en `outbox_events` se publica automáticamente a su topic de
Kafka por CDC.

## Importante: no ejecutar el relay y Debezium a la vez

El `OutboxRelay` (scheduler que hace polling de la tabla y publica a Kafka) y Debezium hacen
**lo mismo**. Para usar CDC, hay que **desactivar el relay** (p. ej. con un flag de
configuración o un perfil) y dejar que Debezium sea el único publicador, evitando eventos
duplicados. Es un cambio de estrategia, no de los consumidores.

## Por qué es mejor que el polling

- **Sin latencia de polling** ni carga periódica sobre la base de datos.
- **No pierde eventos**: lee el log de transacciones, no una consulta que podría saltarse filas.
- **Desacoplado**: la aplicación solo escribe en su tabla; la publicación es responsabilidad de
  la infraestructura. El mismo mecanismo sirve para integrar sistemas legados sin modificarlos.
