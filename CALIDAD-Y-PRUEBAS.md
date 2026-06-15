# BookPlus — Pruebas, Cobertura (JaCoCo) y Calidad (SonarQube)

Esta guía documenta toda la infraestructura de **pruebas automatizadas**, **cobertura de
código** y **análisis de calidad** del proyecto BookPlus, y cómo usarla correctamente con
todos sus comandos.

> Plataforma de referencia de los comandos: **Windows + Git Bash (MINGW64)** con **Docker
> Desktop**. No se necesita instalar Maven ni Java en la máquina: todo corre dentro de
> contenedores Maven desechables.

---

## 1. Resumen de lo implementado

| Pieza | Qué es | Dónde |
|-------|--------|-------|
| **Tests unitarios/de dominio** | JUnit 5 + AssertJ + Mockito en los 8 servicios Java | `book-plus-<svc>/src/test/...` |
| **Tests de la saga de compra** | Máquina de estados del pedido + compensaciones | `book-plus-order-service/.../OrderTest.java`, `CreateOrderUseCaseImplTest.java` |
| **JaCoCo** | Mide la cobertura cuando corren los tests | Plugin en cada `book-plus-<svc>/pom.xml` |
| **Propiedades Sonar** | `projectKey`, ruta del reporte JaCoCo y exclusiones | `<properties>` de cada `pom.xml` |
| **`run-coverage.sh`** | Corre tests + cobertura de los 8 servicios y da un resumen PASS/FAIL | Raíz del proyecto |
| **`docker-compose.sonar.yml`** | Levanta SonarQube + su base de datos | Raíz del proyecto |

Servicios cubiertos (8): `api-gateway`, `auth-service`, `cart-service`, `catalog-service`,
`inventory-service`, `notification-service`, `order-service`, `payment-service`.

> Los servicios `admin-bff` y `report-service` no tienen tests todavía, por lo que no se
> incluyeron en el tooling de cobertura.

---

## 2. Conceptos clave

### 2.1 ¿Por qué el build de Docker NO ejecuta los tests?

Los `Dockerfile` compilan con `-Dmaven.test.skip=true` para que un test roto no bloquee la
creación de la imagen. Por eso la cobertura y la calidad se miden en una **fase aparte**
(local o en CI), no durante `docker compose build`.

### 2.2 Tipos de cobertura que mide JaCoCo

Al abrir el reporte HTML verás varias columnas; todas se miden por defecto:

- **Instructions** — instrucciones de bytecode ejecutadas (la métrica más fina; columna principal).
- **Branches** — ramas de cada `if`/`switch`/ternario cubiertas (la más valiosa para lógica como la máquina de estados del pedido).
- **Lines** — líneas tocadas (el típico "% de cobertura").
- **Methods / Classes** — métodos y clases con al menos una instrucción ejecutada.
- **Cxty (Complexity)** — complejidad ciclomática cubierta.

### 2.3 ¿Qué es la "saga de compra" que prueban los tests?

Una **saga** coordina una operación que toca varios microservicios (cada uno con su propia
BD) mediante eventos, en vez de una transacción única. La saga de compra:

```
cart.checked-out → order.created → (inventory reserva stock)
                                 → (payment confirma pago) → order.payment.confirmed
                                 → (inventory descuenta stock) → notification envía correos
```

Lo que mantiene la coherencia: el **transactional outbox** (el pedido y su evento se guardan
en la misma transacción) y la **compensación** (si algo falla, se emite `order.cancelled` y
el inventario libera la reserva; en un reembolso se emite `order.refunded` y se repone stock).

Los tests del `order-service` verifican el camino feliz completo y las compensaciones
(cancelación, reembolso con/sin reposición, transiciones inválidas).

---

## 3. Ejecutar los tests + cobertura

### 3.1 Todos los servicios de una vez (recomendado)

Desde la raíz del proyecto, en Git Bash:

```bash
bash run-coverage.sh
```

Esto recorre los 8 servicios, ejecuta `mvn test` en un contenedor Maven desechable
reutilizando la caché de dependencias (volumen `bookplus-m2`) y al final imprime:

```
  PASS: api-gateway auth-service cart-service catalog-service inventory-service notification-service order-service payment-service
  FAIL: (ninguno)
```

La primera corrida descarga dependencias (lento); las siguientes son rápidas gracias a la caché.

### 3.2 Un solo servicio

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "C:/proyecto-book-plus/book-plus-order-service:/app" \
  -v bookplus-m2:/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn test
```

Sustituye `book-plus-order-service` por el servicio que quieras.

> **Nota de Git Bash:** el prefijo `MSYS_NO_PATHCONV=1` evita que Git Bash convierta las rutas
> tipo Unix y confunda a Docker. La ruta del volumen usa formato Windows con barras normales
> (`C:/...`). El volumen `bookplus-m2` cachea las dependencias entre corridas.

### 3.3 Ver el reporte de cobertura

Tras correr los tests, cada servicio genera su reporte en:

```
book-plus-<servicio>/target/site/jacoco/index.html
```

Ábrelo en el navegador para ver la cobertura por paquete, clase y método, con todas las
métricas de la sección 2.2.

---

## 4. SonarQube — análisis de calidad consolidado

SonarQube añade, sobre la cobertura de JaCoCo, detección de **bugs**, **code smells**,
**vulnerabilidades** y un **Quality Gate** con umbrales configurables desde su interfaz.

### 4.1 Levantar SonarQube

```bash
docker compose -f docker-compose.sonar.yml up -d
```

Esto arranca `sonarqube` (puerto **9000**) y su PostgreSQL. Espera ~1-2 minutos a que
inicialice.

> **Linux/WSL2:** SonarQube embebe Elasticsearch y necesita un límite alto de mmap. Si no
> arranca, ejecuta una vez: `sudo sysctl -w vm.max_map_count=262144`.

### 4.2 Configuración inicial (una sola vez)

1. Abre **http://localhost:9000**
2. Entra con `admin` / `admin` (te pedirá cambiar la contraseña).
3. Genera un token: **My Account → Security → Generate Token**. Cópialo.

### 4.3 Analizar un servicio

Cada `pom.xml` ya trae las propiedades de Sonar (`sonar.projectKey`, la ruta del reporte
JaCoCo y las exclusiones de código generado). El análisis se lanza así (ejemplo
`order-service`):

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "C:/proyecto-book-plus/book-plus-order-service:/app" \
  -v bookplus-m2:/root/.m2 \
  -w /app \
  --network proyecto-book-plus_bookplus-net \
  maven:3.9-eclipse-temurin-21 \
  mvn clean test sonar:sonar \
    -Dsonar.host.url=http://sonarqube:9000 \
    -Dsonar.token=TU_TOKEN
```

- `clean test` ejecuta los tests y genera el reporte JaCoCo (que Sonar consume automáticamente vía la propiedad `sonar.coverage.jacoco.xmlReportPaths`).
- `--network proyecto-book-plus_bookplus-net` conecta el contenedor Maven a la red donde corre SonarQube. **Verifica el nombre exacto** de tu red con `docker network ls` (suele ser `<carpeta>_<red>`); si SonarQube y tu stack están en redes distintas, usa `--network` con la red de `docker-compose.sonar.yml` o publica el puerto y usa `http://host.docker.internal:9000`.
- Sustituye `TU_TOKEN` por el token generado en el paso 4.2.

Repite cambiando `book-plus-order-service` por cada servicio. Cada uno aparece en Sonar como
un proyecto independiente (`bookplus-<servicio>`).

### 4.4 Ver resultados y gobernar umbrales

En **http://localhost:9000** verás, por proyecto: % de cobertura (por tipo), bugs,
vulnerabilidades, code smells y duplicación. Los umbrales se gestionan desde el
**Quality Gate** ("Sonar way" por defecto, que exige cobertura sobre el *código nuevo*) en la
UI de Sonar — **no** hace falta tocar el build para enforzar calidad.

### 4.5 Apagar SonarQube

```bash
docker compose -f docker-compose.sonar.yml down
# Para borrar también sus datos:
docker compose -f docker-compose.sonar.yml down -v
```

---

## 5. Buenas prácticas y notas

- **No pongas un umbral rígido en Maven** (`jacoco:check`): rompería `mvn test` y es frágil.
  Gobierna la cobertura desde el Quality Gate de SonarQube.
- **Exclusiones de cobertura:** cada pom excluye `**/dto/**`, `**/config/**` y
  `**/*Application.java` (código generado/arranque) para que el porcentaje no salga
  distorsionado. Ajústalas en `<properties>` si lo necesitas.
- **Caché de dependencias:** mantén el volumen `bookplus-m2`; acelera enormemente las
  corridas. Para borrarlo: `docker volume rm bookplus-m2`.
- **CI:** en un pipeline, replica `run-coverage.sh` (o el comando por servicio) en una etapa
  separada del build de imágenes Docker, y publica el reporte de JaCoCo / envía a SonarQube.

---

## 6. Referencia rápida de comandos

```bash
# Tests + cobertura de TODOS los servicios
bash run-coverage.sh

# Tests + cobertura de UN servicio (ej. cart-service)
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "C:/proyecto-book-plus/book-plus-cart-service:/app" \
  -v bookplus-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn test

# Levantar / apagar SonarQube
docker compose -f docker-compose.sonar.yml up -d
docker compose -f docker-compose.sonar.yml down

# Analizar un servicio con SonarQube (tras generar token)
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "C:/proyecto-book-plus/book-plus-order-service:/app" \
  -v bookplus-m2:/root/.m2 -w /app \
  --network proyecto-book-plus_bookplus-net \
  maven:3.9-eclipse-temurin-21 \
  mvn clean test sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -Dsonar.token=TU_TOKEN

# Ver el nombre de la red de Docker (para --network)
docker network ls

# Reporte de cobertura (abrir en navegador)
#   book-plus-<servicio>/target/site/jacoco/index.html
```

---

## 7. Estado actual

Los **8 servicios** pasan sus tests y generan reporte de cobertura:

```
PASS: api-gateway auth-service cart-service catalog-service inventory-service
      notification-service order-service payment-service
```

- `order-service`: **16/16** tests verdes, incluida la saga de compra y sus compensaciones.
- JaCoCo activo en los 8 poms; propiedades de SonarQube configuradas en los 8.
- Tests heredados (que nunca se ejecutaban por el `skip` del build) alineados con el código actual.
