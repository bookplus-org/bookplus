# BookPlus — Microservices Platform

E-commerce de libros construido con **DDD + Arquitectura Hexagonal + Spring Boot 3 + Java 21**.

---

## Arquitectura

```
                        ┌─────────────────────────────────────┐
                        │          API Gateway :8080           │
                        │  JWT validation · Rate limiting      │
                        │  CORS · Request logging              │
                        └──────────────┬──────────────────────┘
                                       │ HTTP (static routes)
          ┌──────────────┬─────────────┼────────────┬──────────────┐
          ▼              ▼             ▼            ▼              ▼
   auth :8081     catalog :8082  cart :8084   order :8085  payment :8086
   JWT/RS256      Books+Search   Redis-only   PostgreSQL   PostgreSQL
   PostgreSQL     Redis+ES                    Outbox       Outbox
          │              │             │            │              │
          └──────────────┴─────────────┴────────────┴──────────────┘
                                       │
                              ┌────────▼────────┐
                              │   Kafka KRaft   │  (single broker, KRaft mode)
                              │   apache/kafka  │
                              │   :9092         │
                              └────────┬────────┘
                                       │
          ┌──────────────┬─────────────┼────────────┬──────────────┐
          ▼              ▼             ▼            ▼              ▼
  inventory :8083 notification:8087 report:8088 admin-bff:8089
  PostgreSQL      PostgreSQL+Mail   PostgreSQL  BFF/proxy
  Reservations    Email delivery    Dashboards  ADMIN only
```

### Microservicios

| # | Servicio | Puerto | Tecnología clave |
|---|---|---|---|
| 1 | **api-gateway** | 8080 | Spring Cloud Gateway, Redis (rate limit) |
| 2 | **auth-service** | 8081 | JWT RS256, BCrypt, PostgreSQL |
| 3 | **catalog-service** | 8082 | PostgreSQL, Redis (cache), Elasticsearch 8 |
| 4 | **inventory-service** | 8083 | PostgreSQL, reservas con TTL |
| 5 | **cart-service** | 8084 | Redis (primary storage), Kafka |
| 6 | **order-service** | 8085 | PostgreSQL, Outbox Pattern, Idempotency |
| 7 | **payment-service** | 8086 | PostgreSQL, Outbox Pattern |
| 8 | **notification-service** | 8087 | PostgreSQL, JavaMailSender, MailHog |
| 9 | **report-service** | 8088 | PostgreSQL, CSV/PDF export |
| 10 | **admin-bff** | 8089 | WebClient proxy/aggregator |

### Patrones aplicados

- **DDD** — Aggregates, Value Objects, Domain Events, Ports & Adapters
- **Hexagonal Architecture** — dominio sin dependencias de infraestructura
- **Transactional Outbox** — garantía de publicación de eventos (order-service)
- **Idempotent Consumers** — tabla `processed_events` en consumers Kafka
- **Event-Driven** — Kafka KRaft (sin Zookeeper), `apache/kafka:3.8.0`
- **JWT RS256** — clave RSA compartida entre todos los servicios

---

## Prerrequisitos

- **Docker Desktop** 4.x+ con Docker Compose v2
- **Make** (incluido en macOS/Linux; en Windows usa Git Bash o WSL)
- **OpenSSL** (para generación de claves)
- **Java 21 + Maven 3.9** (solo si quieres compilar localmente)

---

## Quickstart

### 1. Clonar y entrar al directorio

```bash
git clone https://github.com/tu-org/book-plus.git
cd book-plus
```

### 2. Generar el par de claves RSA para JWT

```bash
make keys
```

Esto ejecuta `scripts/generate-keys.sh` y escribe las claves en `.env`.

### 3. Levantar el stack completo

```bash
make up
```

Los servicios tardan ~60-90 segundos en iniciar (Spring Boot + Flyway migrations).

### 4. Verificar que todo está sano

```bash
make ps
```

Todos los servicios deben aparecer en estado `healthy`.

### 5. Probar la API

```bash
# Registrar un usuario
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"david","email":"david@example.com","password":"Secret123!"}' | jq

# Login y obtener token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"david@example.com","password":"Secret123!"}' | jq -r '.accessToken')

# Listar libros (público)
curl -s http://localhost:8080/api/v1/books | jq

# Ver carrito
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/cart | jq
```

---

## URLs de interés

| Recurso | URL |
|---|---|
| Frontend (Angular + Nginx) | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Swagger catalog-service | http://localhost:8082/swagger-ui.html |
| Swagger order-service | http://localhost:8085/swagger-ui.html |
| Swagger payment-service | http://localhost:8086/swagger-ui.html |
| Swagger admin-bff | http://localhost:8089/swagger-ui.html |
| MailHog (emails en local) | http://localhost:8025 |
| Zipkin (distributed traces) | http://localhost:9411 |
| Elasticsearch | http://localhost:9200 |

---

## Comandos útiles

```bash
make logs              # tail todos los logs
make logs-order-service # tail solo order-service
make test              # ejecutar todos los tests
make test-catalog      # tests de catalog-service
make restart           # reiniciar sin rebuild
make down              # parar y eliminar contenedores
make clean             # down + eliminar volúmenes + mvn clean
```

---

## Tópicos Kafka

| Productor | Tópico | Consumidores |
|---|---|---|
| cart-service | `cart.checked-out` | order-service, notification-service |
| order-service | `order.created` | payment-service, inventory-service, notification-service, report-service |
| order-service | `order.cancelled` | payment-service, inventory-service, notification-service |
| order-service | `order.status.changed` | notification-service, report-service |
| payment-service | `payment.initiated` | order-service |
| payment-service | `payment.confirmed` | order-service, notification-service |
| payment-service | `payment.failed` | notification-service |
| payment-service | `payment.refunded` | notification-service |
| inventory-service | `inventory.stock.low-alert` | notification-service |

---

## Estructura del proyecto

```
book-plus/
├── book-plus-api-gateway/
├── book-plus-auth-service/
├── book-plus-catalog-service/
├── book-plus-inventory-service/
├── book-plus-cart-service/
├── book-plus-order-service/
├── book-plus-payment-service/
├── book-plus-notification-service/
├── book-plus-report-service/
├── book-plus-admin-bff/
├── docker-compose.full.yml   ← stack completo
├── .env.example              ← plantilla de variables
├── .gitignore
├── Makefile
└── scripts/
    └── generate-keys.sh
```

---

## Variables de entorno

Ver `.env.example` para la lista completa. Las más importantes:

| Variable | Descripción |
|---|---|
| `BOOKPLUS_JWT_PRIVATE_KEY_BASE64` | Clave privada RSA (solo auth-service) |
| `BOOKPLUS_JWT_PUBLIC_KEY_BASE64` | Clave pública RSA (todos los servicios) |
| `ADMIN_EMAIL` | Email para alertas de stock bajo |
| `ORDER_OUTBOX_POLL_MS` | Frecuencia del OutboxRelay (default 5000ms) |

---

## Licencia

MIT © BookPlus Engineering Team
