# Mutation testing con PIT (PITest)

La **cobertura** dice qué líneas ejecutan los tests, pero no si esos tests realmente
**detectan errores**. El *mutation testing* lo mide: introduce pequeños cambios ("mutantes")
en el código de producción —cambiar un `>` por `>=`, un `&&` por `||`, devolver `null`…— y
re-ejecuta los tests. Si un test falla, el mutante queda "matado" (bien: tus tests detectan
ese fallo). Si todos pasan, el mutante "sobrevive" (mal: hay un agujero en tus tests).

El **mutation score** (% de mutantes matados) es una medida mucho más honesta de la calidad
de las pruebas que la cobertura. Pocos equipos lo usan, así que dominarlo diferencia un perfil.

## Cómo está configurado

En `book-plus-order-service` (plugin `pitest-maven` + `pitest-junit5-plugin`), acotado al
código de negocio con tests directos fuertes:

- `com.bookplus.order.domain.policy.*` — la política de reembolsos (`RefundPolicy`...).
- `com.bookplus.order.application.coupon.*` — `CouponService`.
- `com.bookplus.order.application.refund.*` — `RefundDecisionService`.

No se ejecuta en el `mvn test` normal (no afecta al pipeline); se lanza bajo demanda.

## Cómo ejecutarlo

```bash
cd book-plus-order-service
mvn -q test-compile org.pitest:pitest-maven:mutationCoverage
```

Al terminar imprime el resumen (mutantes generados / matados / score) y deja un informe
HTML navegable en `target/pit-reports/index.html`, donde se ve, línea a línea, qué mutantes
sobrevivieron — es decir, dónde reforzar los tests.

## Interpretación

- **Score alto** (p. ej. >80%): los tests detectan de verdad los cambios de comportamiento.
- **Mutantes supervivientes**: señalan aserciones que faltan o ramas no comprobadas. Son la
  guía para mejorar los tests (no se trata de llegar al 100%, sino de cerrar agujeros reales).

## Siguiente paso

Extender PIT al resto de servicios y fijar un umbral mínimo de mutation score en el CI
(`<mutationThreshold>`), de modo que el build falle si la calidad de los tests baja.
