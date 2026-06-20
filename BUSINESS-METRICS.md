# Métricas de negocio (Micrometer)

El stack ya exponía métricas **técnicas** (CPU, memoria, latencia, estado de los pools…) a
Prometheus/Grafana. Faltaban las métricas de **negocio**: las que cuentan lo que de verdad
importa al producto. Las añadimos con **Micrometer**, la misma fachada que ya usa el
proyecto, así que viajan por el mismo `/actuator/prometheus` hacia Grafana.

## Por qué importan

Una métrica técnica te dice si el servidor está sano; una de negocio te dice si el **negocio**
está sano. Permiten paneles y alertas como "han caído los pedidos con tarjeta un 40% en la
última hora" — una señal que a menudo detecta una avería (p. ej. la pasarela de pago) antes
que cualquier métrica técnica.

## Qué se mide

En order-service, al crear un pedido (`CreateOrderUseCaseImpl`) se registran:

- **`bookplus.orders.created`** — un **contador** de pedidos creados, etiquetado por
  `payment_method` (CARD, PAYPAL…). Permite ver el ritmo de pedidos y desglosarlo por método
  de pago.
- **`bookplus.orders.amount`** — un **distribution summary** (resumen de distribución) del
  importe de los pedidos, etiquetado por `currency`. Da el total acumulado, la media, el
  máximo y percentiles del ticket.

Ambas se exponen automáticamente en `/actuator/prometheus` y se pueden graficar/alertar en
Grafana (p. ej. `rate(bookplus_orders_created_total[5m])`).

## La integración

- **`OrderMetrics`** (`@Component`): encapsula los meters; recibe el `MeterRegistry`
  autoconfigurado por Spring Boot. Expone `recordOrderCreated(paymentMethod, currency,
  amount)`.
- **`CreateOrderUseCaseImpl`** lo invoca tras persistir el pedido y escribir los eventos en el
  outbox. La métrica es un efecto secundario observacional: no altera la lógica de negocio.

Micrometer cachea los meters por nombre + etiquetas, así que registrar repetidamente la misma
combinación reutiliza el mismo contador (no se crea uno nuevo por llamada).

## Verificación

`OrderMetricsTest` (unit, con un `SimpleMeterRegistry` en memoria, sin Spring) comprueba que
el contador cuenta por método de pago y que el summary acumula el importe correcto. Corre en
el `mvn test` normal.

## Siguiente nivel

- **Más métricas de negocio**: logins fallidos, cupones aplicados, ratio de aciertos de la
  caché, eventos en DLQ, etc.
- **Alertas en Grafana/Alertmanager** sobre desviaciones (caída de pedidos, pico de errores).
- **Exemplars**: enlazar puntos de la métrica con trazas (Zipkin/OTel) para saltar de un pico
  a la traza concreta que lo causó.
