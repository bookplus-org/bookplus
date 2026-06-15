# BookPlus — Cómo correr el proyecto (local con Docker)

Todo está preconfigurado. **No necesitas generar claves ni crear bases de datos a mano.**

## 1. Requisitos
- Docker Desktop (con Docker Compose v2) corriendo.
- Make (en Windows: Git Bash o WSL). *Opcional:* si no tienes `make`, abajo están los comandos `docker compose` equivalentes.

## 2. Arrancar todo (un solo paso)

Las claves RSA ya están en el archivo `.env`, así que **no hace falta `make keys`**.

```bash
make up        # construye imágenes y levanta TODO el stack
make ps        # ver estado/salud de los contenedores
```

Equivalente sin `make`:

```bash
docker compose -f docker-compose.full.yml up -d --build
docker compose -f docker-compose.full.yml ps
```

Espera ~60–90 s a que todos los servicios estén `healthy`. Luego abre:

- **Frontend:** http://localhost:4200
- **API Gateway:** http://localhost:8080
- **MailHog (correos de prueba):** http://localhost:8025
- **Zipkin (trazas):** http://localhost:9411

## 3. Usuario administrador (ya viene en el seed)

```
Usuario / email:  admin@bookplus.com   (o usuario: admin)
Contraseña:       Admin123!
Rol:              SUPERADMIN
```

> En la pantalla de login, el campo acepta el correo `admin@bookplus.com` o el usuario `admin`.
> Cámbiala antes de cualquier uso real.

## 4. Otros comandos útiles

```bash
make logs                 # logs de todo (Ctrl+C para salir)
make logs-order-service   # logs de un servicio concreto
make smoke                # prueba automática contra el gateway
make restart              # reinicia sin reconstruir
make down                 # parar y eliminar contenedores
make clean                # down + borra volúmenes (reinicio limpio)
```

Equivalentes sin `make`:

```bash
docker compose -f docker-compose.full.yml logs -f
docker compose -f docker-compose.full.yml logs -f order-service
docker compose -f docker-compose.full.yml down
docker compose -f docker-compose.full.yml down -v   # + volúmenes
```

---

## 5. pgAdmin 4 — ¿necesito crear las bases de datos?

**No.** Cada microservicio trae su propio contenedor PostgreSQL que:
1. crea su base de datos automáticamente al arrancar, y
2. ejecuta las migraciones (Flyway) con todas las tablas y datos semilla.

Solo usarás pgAdmin para **conectarte y mirar** los datos. Ya expuse los puertos de cada base al host para que pgAdmin pueda conectarse.

### Conexiones en pgAdmin (botón *Register → Server*)

En cada servidor, pestaña **General → Name** (el que quieras) y pestaña **Connection**:

| Base de datos | Host | Puerto | Maintenance DB | Usuario | Contraseña |
|---|---|---|---|---|---|
| auth | localhost | 5433 | auth_db | auth_user | auth_pass |
| catalog | localhost | 5434 | catalog_db | catalog_user | catalog_pass |
| inventory | localhost | 5435 | inventory_db | inventory_user | inventory_pass |
| order | localhost | 5436 | order_db | order_user | order_pass |
| payment | localhost | 5437 | payment_db | payment_user | payment_pass |
| notification | localhost | 5438 | notification_db | notification_user | notification_pass |
| report | localhost | 5439 | report_db | report_user | report_pass |

> En **Maintenance database** pon el nombre de la base (p. ej. `auth_db`).
> Host siempre `localhost` (las bases corren en Docker, pero publiqué los puertos a tu PC).
> Si el puerto **5432** de tu PC ya lo usa tu PostgreSQL local, no hay conflicto: usé 5433–5439.

### ¿Y si prefieres pgAdmin dentro de Docker?
No es necesario; con las conexiones de arriba basta. (Si lo quisieras, se podría añadir un contenedor de pgAdmin al compose, pero no hace falta para correr el proyecto.)

---

## 6. Resumen de puertos publicados a tu PC

| Servicio | Puerto host |
|---|---|
| Frontend (Angular + Nginx) | 4200 |
| API Gateway | 8080 |
| admin-bff (Swagger) | 8089 |
| MailHog UI / SMTP | 8025 / 1025 |
| Kafka | 9092 |
| Zipkin | 9411 |
| PostgreSQL (auth → report) | 5433–5439 |

Los demás microservicios (8081–8088) corren dentro de la red de Docker y se acceden a través del **gateway (8080)**.

---

## 7. Si algo falla
- Un servicio no llega a `healthy`: `make logs-<servicio>`; normalmente es la BD o Kafka aún iniciando. Reintenta.
- Empezar de cero: `make clean && make up`.
- El pedido no aparece tras comprar: es asíncrono (Kafka); espera unos segundos y refresca “Mis pedidos”.
