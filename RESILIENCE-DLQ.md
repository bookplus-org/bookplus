# Dead Letter Queue (DLQ) en los consumidores Kafka

Kafka entrega "al menos una vez", pero si un mensaje **no se puede procesar** (un "mensaje
veneno": payload corrupto, un bug, una dependencia caída de forma permanente), un consumidor
ingenuo lo reintenta en bucle infinito y **bloquea la partición**, deteniendo todo lo demás.

La **Dead Letter Queue** resuelve esto: se reintenta un número acotado de veces y, si sigue
fallando, el mensaje se **aparta** a un topic de "cartas muertas" para inspeccionarlo después,
sin bloquear la cola ni perder el evento.

## Cómo está implementado

En cada servicio con consumidores (**order, catalog, inventory, payment, notification**) el
`KafkaConfig` define un `DefaultErrorHandler` con un `DeadLetterPublishingRecoverer`:

- **Reintentos**: 3, con 1 segundo de espera (`FixedBackOff(1000, 3)`).
- **Destino**: si tras los reintentos sigue fallando, el mensaje se publica en el topic
  `<topic>.DLT` (p. ej. `cart.checked-out.DLT`, `order.payment.confirmed.DLT`).
- **Alcance**: se aplica a **todos** los consumidores del servicio a la vez, vía
  `factory.setCommonErrorHandler(...)` — no hay que anotar cada listener.

El payload se republica con un `KafkaTemplate` de valor JSON, de modo que el evento queda
legible en la DLQ.

## Qué consigue (robustez)

- Un mensaje defectuoso **no detiene** el procesamiento del resto de la cola.
- El evento **no se pierde**: queda en la DLQ para diagnóstico o reproceso manual.
- Combinado con el **IdempotencyGuard** (procesar dos veces no duplica efectos) y el **outbox
  transaccional** (no se pierden eventos al publicar), cierra el ciclo de mensajería fiable.

## Operación

Para inspeccionar mensajes en una DLQ:

```bash
docker compose -f docker-compose.full.yml exec kafka \
  /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic cart.checked-out.DLT --from-beginning
```

## Siguiente nivel

- **Alerta** cuando entra algo en una DLQ (regla en Prometheus/Alertmanager sobre el lag/volumen
  del topic `.DLT`).
- **Reproceso** automatizado: un consumidor que lee la DLQ y reintenta tras corregir la causa.
