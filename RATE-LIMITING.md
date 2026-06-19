# Rate limiting (Bucket4j)

Los endpoints de autenticación son el blanco favorito de ataques de **fuerza bruta** y
**relleno de credenciales** (credential stuffing): un atacante prueba miles de
contraseñas por segundo. El rate limiting acota cuántas peticiones acepta el servicio por
cliente y ventana de tiempo, convirtiendo un ataque masivo en algo inviable.

## Cómo funciona — token bucket

Usamos **Bucket4j** con el algoritmo *token bucket*: cada cliente tiene un "cubo" con una
capacidad de fichas que se rellena de forma continua. Cada petición consume una ficha;
mientras quedan, pasa; al agotarse, se rechaza hasta el siguiente relleno. Es más flexible
que un contador fijo porque permite ráfagas cortas sin penalizar el uso normal.

Por defecto: **10 peticiones por minuto** por combinación de **IP + ruta** (configurable).

## Dónde está aplicado

En auth-service, sobre los endpoints sensibles y públicos:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

El resto del tráfico no se ve afectado.

## La integración

- **`AuthRateLimiter`** (`@Component`): mantiene un `ConcurrentHashMap<clave, Bucket>` en
  memoria. La clave es `IP:ruta`, de modo que cada endpoint tiene su propio presupuesto por
  IP. Expone `tryConsume(clave)` → `true`/`false`.
- **`RateLimitFilter`** (`OncePerRequestFilter`): se ejecuta **antes** del filtro JWT, solo
  para los POST de las rutas limitadas (`shouldNotFilter` descarta el resto). Si el cubo
  está vacío responde **429 Too Many Requests** con cabecera `Retry-After`, sin llegar al
  controller. Respeta `X-Forwarded-For` para obtener la IP real detrás del gateway.
- **Configuración** en `application.yml` (`security.rate-limit.capacity` y
  `period-seconds`), sobreescribible por variables de entorno.

## Verificación

`AuthRateLimiterTest` (unit, sin Spring ni base de datos) comprueba que el cubo permite
hasta la capacidad y luego limita, y que cada clave de cliente es independiente. Corre en
el `mvn test` normal.

## Siguiente nivel

- **Rate limiting distribuido**: con varias réplicas de auth-service, mover los cubos a
  **Redis** (Bucket4j tiene backend para Redis, ya presente en el servicio) para que el
  límite sea global y no por instancia.
- **Subir el límite al API Gateway**: aplicar un rate limit de borde para todo el sistema,
  además del específico de auth.
- **Backoff progresivo / bloqueo temporal** de IPs reincidentes.
