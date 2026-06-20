# Bloqueo de cuenta tras intentos fallidos

El **rate limiting por IP** (Bucket4j) ya frena a un atacante que martillea desde una IP. Pero
un ataque de fuerza bruta serio usa **muchas IPs** (botnet) contra **una misma cuenta**. El
bloqueo de cuenta cierra ese hueco: cuenta los fallos **por cuenta**, no por IP, y la bloquea
temporalmente al superar un umbral. Es un control clásico de banca, complementario al rate
limiting.

## Cómo funciona

En auth-service, `LoginAttemptService` (respaldado por **Redis**):

- En cada **login fallido** incrementa un contador por cuenta (`login:attempts:<id>`) con una
  ventana temporal (TTL). Al alcanzar **N intentos** (5 por defecto), pone una marca de
  bloqueo (`login:locked:<id>`) durante un periodo (15 min por defecto) y reinicia el contador.
- En cada **login** se comprueba primero si la cuenta está bloqueada; si lo está, se rechaza
  sin siquiera verificar la contraseña.
- Un **login correcto** limpia contadores y bloqueo.

Todo vive en Redis con TTL, así que la ventana y el bloqueo **caducan solos** y el límite es
**global** entre réplicas de auth.

Por seguridad, cuando la cuenta está bloqueada se devuelve el mismo error de credenciales
inválidas (no se revela el motivo exacto), para no dar pistas al atacante.

## La integración

- **`LoginAttemptStore`** (puerto) + **`RedisLoginAttemptStore`** (adaptador): contadores con
  TTL en Redis.
- **`LoginAttemptService`** (`@Component`): la lógica de umbral/bloqueo, configurable por
  `security.lockout.*` (max-attempts, window-seconds, lock-seconds).
- **`AuthenticateUserUseCaseImpl`**: comprueba el bloqueo al entrar, registra el fallo al
  fallar la contraseña y limpia al acertar.

## Verificación

`LoginAttemptServiceTest` (unit, con un store en memoria, sin Redis) comprueba que la cuenta
se bloquea al alcanzar el máximo de fallos, que un login correcto reinicia el contador y que
cuentas distintas son independientes. Corre en el `mvn test` normal.

## Siguiente nivel

- Responder **423 Locked** o **429** con `Retry-After` cuando la cuenta está bloqueada (mejor
  UX para usuarios legítimos), aceptando revelar el estado de bloqueo.
- **Backoff progresivo** (cada bloqueo dura más) y notificar al usuario por email de los
  intentos sospechosos.
