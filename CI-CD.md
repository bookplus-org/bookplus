# BookPlus — CI/CD con GitHub Actions

Esta guía documenta el pipeline de **Integración Continua (CI)** y **Despliegue Continuo
(CD)** del proyecto, montado sobre **GitHub Actions**.

---

## 1. ¿Qué hace cada parte?

| Fase | Archivo | Cuándo se ejecuta | Qué hace |
|------|---------|-------------------|----------|
| **CI** | `.github/workflows/ci.yml` | en cada `push` y `pull_request` | Compila, ejecuta **tests** de los 8 servicios Java, mide **cobertura (JaCoCo)**, construye el **frontend** y (opcional) analiza calidad con **SonarQube**. |
| **CD** | `.github/workflows/cd.yml` | `push` a `main`/`master`, tags `v*`, o a mano | Construye las **11 imágenes Docker** y las publica en **GHCR** (ghcr.io). Si configuras SSH, **despliega** en tu servidor. |

Archivos de apoyo:

- `docker-compose.deploy.yml` — igual que `docker-compose.full.yml` pero usando **imágenes del
  registro** (ghcr) en vez de construir localmente. Es el que corre en el servidor.

---

## 2. CI — Integración Continua

No requiere ninguna configuración: funciona en cuanto subes el repo a GitHub.

- **Job `test`** — matriz que corre `mvn test` en cada uno de los 8 servicios con JDK 21 y
  caché de Maven. Sube el reporte de cobertura como *artifact* descargable desde la corrida.
- **Job `frontend`** — `npm ci` + `npm run build` del frontend Angular.
- **Job `sonar`** — análisis SonarQube por servicio. **Solo se activa si defines los secretos**
  `SONAR_TOKEN` y `SONAR_HOST_URL` (ver §4); si no, se omite sin fallar.

Verás el resultado (✓/✗) en la pestaña **Actions** del repo y en cada Pull Request.

---

## 3. CD — Build, publicación y despliegue

### 3.1 Build & push (automático, sin secretos extra)

El job `build-push` construye las 11 imágenes **multi-arquitectura (linux/amd64 + linux/arm64)**
y las publica en **GitHub Container Registry**:

```
ghcr.io/<tu-usuario>/bookplus-<servicio>:latest
ghcr.io/<tu-usuario>/bookplus-<servicio>:<sha-del-commit>
```

Usa el `GITHUB_TOKEN` que GitHub provee automáticamente (con permiso `packages: write`). No
necesitas crear nada para esta parte.

> **Multi-arquitectura:** las mismas imágenes funcionan en **Oracle Cloud (ARM/arm64)** y en
> **AWS u otros x86 (amd64)** sin recompilar. Docker descarga automáticamente la variante
> correcta para cada servidor. (La construcción arm64 emula con QEMU, así que el job tarda algo
> más; es normal.)

> Tras la primera publicación, entra en tu perfil de GitHub → **Packages** y marca los
> paquetes como públicos o da acceso al servidor de despliegue, según prefieras.

### 3.2 Deploy por SSH (opcional)

El job `deploy` se conecta a tu servidor por SSH y actualiza el stack con
`docker-compose.deploy.yml`. **Solo se ejecuta si defines los secretos de despliegue** (§4).

Lo que hace en el servidor:

```bash
cd <DEPLOY_PATH>
git pull --ff-only
docker login ghcr.io ...
export REGISTRY=ghcr.io/<tu-usuario>
export TAG=latest
docker compose -f docker-compose.deploy.yml pull
docker compose -f docker-compose.deploy.yml up -d
```

---

## 4. Secretos a configurar en GitHub

En el repo: **Settings → Secrets and variables → Actions → New repository secret**.

### Para SonarCloud / SonarQube (opcional, habilita el job `sonar`)

| Secreto | Valor |
|---------|-------|
| `SONAR_TOKEN` | Token generado en SonarCloud (My Account → Security) o tu SonarQube |
| `SONAR_ORGANIZATION` | **Solo SonarCloud:** el *key* de tu organización en sonarcloud.io |
| `SONAR_HOST_URL` | Opcional. Por defecto `https://sonarcloud.io`; ponlo solo si usas un SonarQube propio |

> Para análisis en CI conviene **SonarCloud** (gratis para repos **públicos**): el runner de
> GitHub lo alcanza por internet sin abrir nada. Pasos: entra a https://sonarcloud.io con tu
> cuenta de GitHub → crea una **organización** → crea un proyecto por cada servicio (o
> impórtalos) cuyo *project key* coincida con el `sonar.projectKey` del `pom.xml`
> (`bookplus-<servicio>`) → genera un token y guárdalo como `SONAR_TOKEN`.

### Para el despliegue por SSH (opcional, habilita el job `deploy`)

| Secreto | Valor |
|---------|-------|
| `DEPLOY_HOST` | IP o dominio del servidor |
| `DEPLOY_USER` | Usuario SSH |
| `DEPLOY_SSH_KEY` | Clave **privada** SSH (el servidor debe tener la pública en `authorized_keys`) |
| `DEPLOY_PATH` | Ruta del repo clonado en el servidor (donde está `docker-compose.deploy.yml`) |
| `GHCR_USER` | Tu usuario de GitHub (para `docker login` a ghcr en el servidor) |
| `GHCR_TOKEN` | Un Personal Access Token con scope `read:packages` |

---

## 5. Prerrequisitos del servidor (para el deploy)

1. Tener **Docker** y **Docker Compose** instalados.
2. Clonar el repo en `DEPLOY_PATH` (`git clone ...`).
3. Crear el archivo **`.env`** en esa carpeta con las variables que usa el compose (claves
   JWT, contraseñas de BD, etc.) **más**:
   ```env
   REGISTRY=ghcr.io/<tu-usuario>
   TAG=latest
   ```
4. La primera vez, hacer `docker login ghcr.io` con tu usuario y un token `read:packages`
   (el workflow también lo hace en cada deploy).

---

## 6. Flujo de trabajo típico

```text
git push  ──►  CI (tests + cobertura + sonar)
                 │  ✓ todo verde
   merge a main ─┘
                 ▼
              CD: build & push de imágenes a GHCR
                 │
                 ▼  (si hay secretos SSH)
              deploy: el servidor hace pull + up -d
```

- Para una **release versionada**, crea un tag: `git tag v1.0.0 && git push --tags`. El CD
  publicará las imágenes con ese commit y desplegará.
- Para desplegar a mano: pestaña **Actions → CD → Run workflow** (`workflow_dispatch`).

---

## 7. Recomendación de adopción

1. **Empieza por CI** (ya funciona al subir el repo): te da tests + cobertura automáticos en
   cada cambio, riesgo cero.
2. Añade **SonarCloud** cuando quieras el tablero de calidad (define `SONAR_TOKEN` /
   `SONAR_HOST_URL`).
3. Activa **build & push** (automático) para tener imágenes versionadas en GHCR.
4. Activa el **deploy** solo cuando tengas el servidor listo y los secretos SSH configurados.

> Relacionado: la guía de pruebas/cobertura local y SonarQube manual está en
> **`CALIDAD-Y-PRUEBAS.md`**.
