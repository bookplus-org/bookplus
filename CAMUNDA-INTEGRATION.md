# Integración de Camunda 7 (orquestación BPMN de la saga)

Camunda es un motor de procesos **BPMN**: permite modelar procesos de negocio largos como
diagramas estándar (BPMN 2.0) y ejecutarlos con visibilidad, reintentos, compensaciones y
auditoría. Es muy habitual en banca para flujos como apertura de cuenta, préstamos o pagos.

En BookPlus se añade en **order-service** como **orquestación de la saga de compra**,
de forma **aditiva**: el flujo por eventos (Kafka) sigue funcionando igual; Camunda aporta
una vista de proceso modelada y ejecutable de la parte de decisión/compensación.

## Qué se ha añadido

- **Dependencia** `camunda-bpm-spring-boot-starter` 7.22 en order-service.
- **Proceso** `src/main/resources/processes/purchase-saga.bpmn` — se despliega solo al
  arrancar (`camunda.bpm.auto-deployment-enabled: true`).
- **Delegates** (adaptadores de entrada que conectan el motor con los casos de uso):
  - `confirmPaymentDelegate` → `UpdateOrderStatusUseCase.confirmPayment(orderId)`.
  - `cancelOrderDelegate` → `CancelOrderUseCase.cancelAsAdmin(orderId, reason)` (compensación).
- **Tests** de los delegates (`PurchaseSagaDelegatesTest`).

## El modelo BPMN

```
(Resultado de pago) → <¿Pago aprobado?>
                          │ sí → [Confirmar pago del pedido] → (Pedido confirmado)
                          │ no → [Cancelar pedido (compensación)] → (Pedido cancelado)
```

Las cajas son *service tasks* que invocan los delegates; el rombo es un *gateway exclusivo*
que decide según la variable de proceso `paymentApproved`.

## Cómo arrancar una instancia (demo)

Con order-service en marcha (y su base de datos), Camunda crea sus tablas y despliega el
proceso. Para lanzar una instancia, por ejemplo desde el shell de Groovy de Camunda o por API:

```java
runtimeService.startProcessInstanceByKey("purchaseSaga", Map.of(
    "orderId", "ORD-123",
    "paymentApproved", true   // o false para ver la rama de compensación
));
```

Para verlo gráficamente y operarlo (Cockpit, Tasklist), se puede añadir el starter
`camunda-bpm-spring-boot-starter-webapp` y entrar en `http://localhost:8085/camunda`.

## Notas

- La ejecución de jobs en background va desactivada (`job-execution.enabled: false`)
  porque el proceso de demo es síncrono; en producción se activa para continuaciones
  asíncronas, timers y reintentos.
- En producción, Camunda usaría su propia base de datos y se modelarían también los pasos
  de reservar stock y cobrar como tareas, convirtiendo toda la saga en un único proceso
  observable y auditable.
