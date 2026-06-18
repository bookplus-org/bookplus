# Contract testing con Pact (eventos del saga)

El *contract testing* verifica que el **contrato** entre un productor y un consumidor de
mensajes no se rompe, sin necesidad de levantar ambos servicios a la vez. Es esencial en
microservicios: evita que un cambio en cart-service rompa silenciosamente a order-service.

Aquí se aplica al evento **`cart.checked-out`**: cart-service lo **produce** y order-service
lo **consume**.

## Enfoque consumer-driven

1. El **consumidor** (order-service) define qué espera recibir y escribe un test Pact:
   `CartCheckedOutContractTest`. Al pasar, Pact **genera el contrato** (un JSON) en
   `target/pacts/order-service-cart-service.json`.
2. El **productor** (cart-service) verifica que el mensaje que realmente publica **cumple**
   ese contrato.

El contrato refleja la serialización real del evento: los value objects (`CartId`, `BookId`)
viajan como `{"value": "<uuid>"}` y `Money` como `{amount, currency}`.

## Lado consumidor (ya implementado y verificable)

`book-plus-order-service` → `src/test/java/.../contract/CartCheckedOutContractTest.java`.
Define el contrato y comprueba que el consumidor real (`CartEventConsumer`) procesa un
mensaje conforme y crea el pedido. Se ejecuta con `mvn test` y produce el pact.

## Lado productor (verificación en cart-service)

Sin un Pact Broker, el contrato generado se comparte copiándolo al productor:

```bash
# 1) Generar el contrato (lado consumidor)
cd book-plus-order-service && mvn -q test
# 2) Copiarlo a las pruebas del productor
mkdir -p ../book-plus-cart-service/src/test/resources/pacts
cp target/pacts/order-service-cart-service.json ../book-plus-cart-service/src/test/resources/pacts/
```

Test de verificación del productor (a añadir en cart, con `pact-provider junit5`):

```java
@Provider("cart-service")
@PactFolder("src/test/resources/pacts")
@IgnoreNoPactsToVerify
class CartCheckedOutProviderTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) { context.verifyInteraction(); }

    @BeforeEach
    void target(PactVerificationContext context) { context.setTarget(new MessageTestTarget()); }

    @PactVerifyProvider("a cart checked-out event")
    String produce() throws Exception {
        var event = /* construir un CartCheckedOutEvent real */;
        return objectMapper.writeValueAsString(event); // mismo ObjectMapper que el publisher
    }
}
```

Pact compara el mensaje producido contra el contrato (con type matchers); si cart cambiara
la forma del evento, la verificación fallaría.

## En producción: Pact Broker

El paso de copiar el JSON a mano lo automatiza un **Pact Broker** (o PactFlow): el consumidor
publica el contrato y el productor lo descarga y verifica en su pipeline de CI. Es el patrón
estándar en empresas con muchos microservicios.
