# Fase 0 — Preparación del backend para el frontend Angular

Estado de los puntos previos a construir el frontend. Fecha: 30/05/2026.

## ✅ Hecho (cambios aplicados en el código)

### 1. Formato de error homogéneo (RFC 7807 ProblemDetail)
Todos los servicios devuelven ahora el mismo contrato de error, de modo que el frontend
puede implementar un único `ErrorInterceptor`.

- **URI base unificada**: todos los handlers usan `https://bookplus.com/errors/<tipo>`
  (antes cart, notification, order y payment usaban `api.bookplus.com`).
- **`timestamp` presente en todas las respuestas de error** (antes faltaba en cart,
  notification, order y payment).
- **Errores de validación** siguen exponiendo el mapa `errors` (campo → mensaje) de forma
  consistente en todos los servicios.
- **notification-service**: se añadió el manejador de validación (`400`) que antes no existía.

Forma del payload de error que recibirá el frontend:

```json
{
  "type": "https://bookplus.com/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "timestamp": "2026-05-30T12:34:56.789Z",
  "errors": { "email": "must be a valid email", "password": "must not be blank" }
}
```

### 2. CORS del gateway configurable por entorno
- `CorsConfig` ya no usa `*` fijo: lee `gateway.cors.allowed-origins`
  (variable `CORS_ALLOWED_ORIGINS`), con valor por defecto `http://localhost:4200`
  (dev server de Angular).
- Añadido en `application.yml` del gateway, en `.env.example` y en `docker-compose.full.yml`.
- En producción basta con definir `CORS_ALLOWED_ORIGINS=https://app.bookplus.com`.

### 3. Contratos OpenAPI: gateway declarado como servidor
- Los 9 `OpenApiConfig` declaran ahora el servidor `http://localhost:8080` (API Gateway),
  de modo que cada Swagger UI y cada documento `/v3/api-docs` apuntan a la URL real que
  consumirá el frontend. Antes ningún servicio lo declaraba.

### 4. Script de verificación (smoke test)
- Nuevo `scripts/smoke-test.sh` (+ target `make smoke`) que valida contra el gateway:
  registro, login, ruta protegida con/sin token (401 + ProblemDetail), formato de error
  unificado (type/title/status/timestamp + URI `bookplus.com`) y la cabecera CORS para el
  origen de Angular. Requiere `curl` y `jq`.

## 🔎 Verificación final (1 comando — requiere tu Docker/Maven)

Los cambios de código están aplicados y revisados estáticamente. La verificación en tiempo
de ejecución no se pudo hacer en el entorno de edición (sin JDK 21 / Maven / Docker), así que
queda este único paso para ti, ya automatizado:

```bash
make keys     # solo la primera vez
make up       # levanta el stack (Spring Boot + Flyway, ~60-90s)
make ps       # confirma que todo está healthy
make smoke    # ejecuta scripts/smoke-test.sh contra el gateway
```

`make smoke` valida automáticamente: registro, login, JWT con/sin token (401 + ProblemDetail),
formato de error unificado y CORS para `http://localhost:4200`. Si todo pasa, imprime
**"Fase 0 verificada ✔"**.

Opcionalmente, generar los modelos TypeScript del frontend a partir de los
`/v3/api-docs` (ahora ya apuntan al gateway) para congelar el contrato.
