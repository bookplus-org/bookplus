# Feature flags (Togglz)

Los *feature flags* permiten **activar o desactivar funciones en caliente, sin desplegar**.
Es una herramienta clave en equipos que entregan rápido: habilitar una función solo para
algunos usuarios (canary), apagar al instante algo que da problemas (kill-switch) o separar
el *deploy* del *release*. Lo integramos con **Togglz** en catalog-service.

## Cómo funciona

- Las funciones se declaran en un enum **`FeatureFlags`** que implementa `Feature` (Togglz).
  Cada constante es una bandera, con su etiqueta y su estado por defecto.
- En tiempo de ejecución, el código pregunta `featureManager.isActive(FeatureFlags.X)` y se
  comporta según el estado **actual** de la bandera.
- El estado se cambia desde la **consola de administración** (`/togglz-console`) o por API,
  y tiene efecto **inmediato**, sin reiniciar ni redeplegar.

## Qué está aplicado

Una bandera **`BOOK_SEARCH`** (activada por defecto) que actúa como **kill-switch** de la
búsqueda: en `SearchBooksUseCaseImpl`, si la bandera está desactivada, la búsqueda devuelve
un resultado vacío **sin llegar a Elasticsearch**. Así, si ES se degrada o satura, un
operador puede apagar la búsqueda al instante para proteger el resto del catálogo, y volver a
encenderla cuando se estabilice — todo sin desplegar.

Es **aditivo**: por defecto la bandera está activa, así que el comportamiento actual no
cambia.

## La integración

- **Dependencias**: `togglz-spring-boot-starter` y `togglz-console`.
- **`FeatureFlags`** (enum `Feature`) en `shared.featureflags`.
- **Configuración** en `application.yml`: `togglz.feature-enums` apunta al enum y se habilita
  la consola en `/togglz-console`.
- **`FeatureManager`** (bean autoconfigurado por el starter) se inyecta en el caso de uso y
  decide el comportamiento. Por defecto el estado vive en memoria; para producción con varias
  réplicas se usaría un `StateRepository` compartido (JDBC o Redis) para que la bandera sea
  global.

## Verificación

`SearchBooksFeatureFlagTest` (unit, sin Spring ni Elasticsearch) construye un `FeatureManager`
en memoria y comprueba: con la bandera activa la búsqueda delega en el puerto; con la bandera
desactivada devuelve vacío y **no** toca el puerto. Corre en el `mvn test` normal.

## Buenas prácticas / siguiente nivel

- **Proteger la consola** en producción (autenticación o acceso solo por red interna). En dev
  está abierta para facilitar las pruebas.
- **StateRepository compartido** (Redis/JDBC) para que las banderas sean consistentes entre
  réplicas.
- **Estrategias de activación** de Togglz: por porcentaje de usuarios (canary), por rol, por
  ventana horaria, etc.
- **Higiene de flags**: retirar las banderas cuando la función se estabiliza, para no
  acumular deuda técnica.
