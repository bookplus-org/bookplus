# CI/CD doble: GitHub Actions **y** Jenkins a la vez (como una empresa)

Este proyecto corre **dos** sistemas de CI/CD sobre el **mismo repo**, igual que muchas
empresas (sobre todo banca) que mantienen Jenkins on-premise mientras adoptan GitHub Actions:

- **GitHub Actions** — corre en los servidores de GitHub. Ya funciona en cada push/PR.
- **Jenkins** — corre en **tu** máquina (ahora local; mañana, un servidor). Descubre los
  cambios escaneando el repo (multibranch) y, en producción, por webhook.

Ambos ejecutan el **mismo pipeline** (build, test, SonarCloud+Quality Gate, imágenes a GHCR,
Trivy, deploy). Son intercambiables a propósito: es una demostración de dominio de las dos
herramientas líderes del sector.

> **Autenticación profesional**: Jenkins publica a GHCR con una **GitHub App** (tokens
> efímeros de 1 h que se renuevan solos), no con un PAT personal. GitHub Actions usa su
> `GITHUB_TOKEN` automático. Resultado: **ningún humano rota tokens a mano**. Ver
> `SECRETS-MANAGEMENT.md` para el porqué.

---

## Estado: qué está ya hecho y qué te toca a ti

| Pieza | Estado |
|-------|--------|
| Workflows de GitHub Actions (`.github/workflows/ci.yml`, `cd.yml`, `security.yml`) | ✅ Listos |
| `GITHUB_TOKEN` automático para GHCR (Actions) | ✅ Nativo, nada que hacer |
| `Jenkinsfile` + Shared Library + JCasC + compose | ✅ Listos y cableados a GitHub App |
| Credencial GitHub App en Jenkins (boot-safe) | ✅ Activada; arranca con key dummy |
| **Crear la GitHub App + su key** | ⏳ **Solo tú** (es tu cuenta) |
| **Subir la Shared Library a su propio repo** | ⏳ **Solo tú** |
| **Poner `SONAR_TOKEN` en los Secrets de GitHub** | ⏳ **Solo tú** |
| **Arrancar Jenkins en tu máquina** | ⏳ **Solo tú** |

Lo de la columna ⏳ no es cuestión de permisos míos: son secretos y acciones que viven en
**tus** cuentas de GitHub/SonarCloud y en **tu** ordenador.

---

## Parte A — Dejar GitHub Actions verde

1. **Rota el token de SonarCloud expuesto** (lo pegaste en el chat: empieza por `d48293…`).
   SonarCloud → *My Account → Security* → revoca ese token y **genera uno nuevo**.
2. GitHub → tu repo `bookplus` → *Settings → Secrets and variables → Actions → New repository
   secret*:
   - `SONAR_TOKEN` = el token nuevo.
   - `SONAR_ORGANIZATION` = `dhuarocc` (si tu workflow lo usa como secret; si va en
     `sonar-project.properties`, no hace falta).
3. Haz un push (o reabre un PR). En la pestaña **Actions** los workflows deben pasar.
   - GHCR **no** necesita ningún secret: Actions usa el `GITHUB_TOKEN` automático.

---

## Parte B — Crear la GitHub App (una sola vez, nivel profesional)

1. GitHub → *Settings → Developer settings → **GitHub Apps** → New GitHub App*.
   - **GitHub App name**: `bookplus-ci` (o el que quieras).
   - **Homepage URL**: cualquiera (p. ej. la URL del repo).
   - **Webhook**: desmarca *Active* (no lo necesitas para esto).
   - **Permissions → Repository**:
     - *Packages*: **Read and write**  ← para publicar imágenes en GHCR.
     - *Contents*: **Read-only**  ← para que Jenkins lea el código (SCM).
     - *Metadata*: **Read-only** (se marca solo).
   - **Where can this app be installed**: *Only on this account*.
   - Crea la App.
2. En la página de la App, anota el **App ID** (un número).
3. Baja a *Private keys* → **Generate a private key**. Se descarga un `.pem` (formato PKCS#1).
4. **Install App** (menú izquierdo) → instálala en tu cuenta y dale acceso al repo `bookplus`
   (y, si la separas, también a `bookplus-jenkins-shared-lib`).

### Convertir la key a PKCS#8 (el plugin de Jenkins lo exige)

GitHub entrega la key como `-----BEGIN RSA PRIVATE KEY-----` (PKCS#1). El plugin de Jenkins
necesita `-----BEGIN PRIVATE KEY-----` (PKCS#8). Conviértela una vez:

```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in bookplus-ci.2026-xx-xx.private-key.pem \
  -out github-app.pem
```

Guarda `github-app.pem` **FUERA del repo** (p. ej. `C:\Users\W11\.bookplus\github-app.pem`).
Nunca lo subas a git.

---

## Parte C — Apuntar Jenkins a la GitHub App

En tu `.env` (junto a `docker-compose.jenkins.yml`):

```ini
GITHUB_APP_ID=123456                                  # el App ID que anotaste
GITHUB_APP_KEY_FILE=/c/Users/W11/.bookplus/github-app.pem   # ruta a tu .pem PKCS#8 (Git Bash)
```

- Mientras `GITHUB_APP_KEY_FILE` esté vacío, Jenkins arranca con una **key dummy**
  (`jenkins/casc/github-app.pem.example`): no autentica, pero **no rompe el arranque**.
  En cuanto pongas la ruta real, Jenkins acuña tokens de 1 h con tu App.
- El `Jenkinsfile` ya usa `ghcrCredentialsId: 'github-app'`. Para volver al PAT, cámbialo a
  `'ghcr'` y rellena `GHCR_USER`/`GHCR_TOKEN`.

---

## Parte D — Shared Library en su propio repo (profesional)

El JCasC referencia la librería en
`https://github.com/dhuarocc/bookplus-jenkins-shared-lib.git`. Súbela una vez:

```bash
cd jenkins-shared-library
git init -b main
git add .
git commit -m "feat: BookPlus Jenkins shared library"
git remote add origin https://github.com/dhuarocc/bookplus-jenkins-shared-lib.git
git push -u origin main
```

(Crea antes el repo vacío `bookplus-jenkins-shared-lib` en GitHub.) Si la App también tiene
acceso a ese repo, Jenkins la clona sin más credenciales.

---

## Parte E — Arrancar Jenkins y ver los dos verdes

```bash
# 1) Arranca Jenkins (imagen propia con CLI de Docker y plugins ya incluidos)
docker compose -p bookplus-jenkins -f docker-compose.jenkins.yml up -d --build

# 2) Abre http://localhost:8080  (usuario: admin / contraseña: la de JENKINS_ADMIN_PASSWORD)
#    Debe entrar SIN asistente, con el job 'bookplus' y la librería ya configurados.

# 3) Logs de arranque (verifica "Jenkins is fully up and running")
MSYS_NO_PATHCONV=1 docker logs -f bookplus-jenkins

# Parar:  docker compose -p bookplus-jenkins -f docker-compose.jenkins.yml down
#         (añade -v para borrar el volumen y forzar un arranque limpio)
```

En la UI: el job **`bookplus`** escanea el repo (multibranch) y crea un pipeline por rama.
Con *Scan Repository Now* lo fuerzas al instante en vez de esperar el escaneo periódico.

A partir de aquí, **cada push** dispara GitHub Actions (en la nube) **y** Jenkins (en tu
máquina, al siguiente escaneo): los dos sistemas, el mismo pipeline.

---

## De local a producción (lo que cambia mañana)

- **Webhooks en vez de escaneo**: con Jenkins en un servidor con URL pública, añade un webhook
  en el repo (`https://TU-JENKINS/github-webhook/`) para builds instantáneos. En local puedes
  simularlo con un túnel (ngrok/cloudflared); por eso ahora usamos escaneo periódico, que
  funciona sin exponer nada.
- **Descubrir Pull Requests**: el `branchSources` actual usa `git { }` (descubre ramas). Para
  descubrir **PRs** cámbialo por la fuente `github { }` con `credentialsId('github-app')` — ya
  tienes la credencial lista.
- **Agentes dedicados**: en producción Jenkins reparte builds en agentes, no en el contenedor
  local.

---

## Checklist final

- [ ] Token de SonarCloud rotado y puesto como secret en GitHub.
- [ ] GitHub Actions verde en la pestaña *Actions*.
- [ ] GitHub App creada, instalada, key convertida a PKCS#8 y fuera del repo.
- [ ] `.env` con `GITHUB_APP_ID` y `GITHUB_APP_KEY_FILE`.
- [ ] Shared Library subida a `bookplus-jenkins-shared-lib`.
- [ ] `docker compose ... up -d --build` → Jenkins arranca sin asistente.
- [ ] Job `bookplus` escanea y construye.
- [ ] `.env` y tu `github-app.pem` real **no** aparecen en `git status`.
