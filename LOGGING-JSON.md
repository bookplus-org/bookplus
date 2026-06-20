# Logging estructurado en JSON + correlation ID

Los logs en texto plano son fáciles de leer para una persona, pero difíciles de **consultar**
a escala. Con varios microservicios y miles de líneas, necesitas filtrar por campos ("dame
todos los logs de esta petición", "errores del usuario X"). Para eso los logs deben ser
**estructurados** (JSON), y cada petición debe llevar un **identificador de correlación**.

## JSON con logstash-logback-encoder

Se añade `net.logstash.logback:logstash-logback-encoder` y un `logback-spring.xml` que, bajo
el perfil **`json`**, emite cada línea como un objeto JSON (timestamp, nivel, logger, mensaje,
y todo el MDC). En desarrollo (sin el perfil) se mantiene la consola legible de siempre. Así:

- En producción, **Promtail** recoge esos JSON y **Loki/Grafana** permiten consultarlos por
  campo (`level="ERROR"`, `correlationId="..."`).
- El JSON incluye automáticamente el `traceId`/`spanId` (de Micrometer Tracing) y el
  `correlationId`, enlazando logs con trazas.

Arranque en modo JSON: `-Dspring.profiles.active=json`.

## Correlation ID por petición

`CorrelationIdFilter` lee la cabecera `X-Correlation-Id` (o genera un UUID si no viene), la
mete en el **MDC** —de modo que **toda** línea de log de esa petición la incluye— y la
devuelve en la respuesta. Al terminar limpia el MDC (para no filtrar el valor a hilos
reutilizados). Con ese id puedes seguir una petición de punta a punta en Grafana.

## Verificación

`CorrelationIdFilterTest` (unit, con request/response simulados) comprueba que el filtro
genera o reutiliza el id, lo expone en el MDC durante la petición, lo devuelve en la respuesta
y lo limpia al final. Corre en el `mvn test` normal.

## Siguiente nivel

- Extender el `logback-spring.xml` JSON a todos los servicios y activar el perfil `json` en
  los contenedores.
- Propagar el `X-Correlation-Id` también en las llamadas salientes y en los eventos Kafka.
