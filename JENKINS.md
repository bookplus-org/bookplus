# Jenkins — pipeline alternativo (complemento de GitHub Actions)

El CI/CD principal del proyecto es **GitHub Actions** (workflows CI, CD y Security). Este
`Jenkinsfile` ofrece **el mismo pipeline expresado en Jenkins**, porque en muchos entornos
corporativos —especialmente banca— Jenkins sigue siendo el estándar. No reemplaza a GitHub
Actions: demuestra que el pipeline es **portable** y que el flujo está como *pipeline-as-code*
también en la herramienta más extendida del sector.

## Etapas del pipeline (`Jenkinsfile`)

1. **Checkout** — descarga el código.
2. **Build & Test** — `mvn test` de los 9 microservicios dentro de un contenedor
   `maven:3.9-eclipse-temurin-21`; publica los resultados JUnit.
3. **SonarCloud** *(solo `main`)* — análisis de calidad con `sonar:sonar` y el token guardado
   en Jenkins.
4. **Package** — empaqueta los JAR (`-DskipTests`).
5. **Docker build & push** *(solo `main`)* — construye y publica las imágenes en GHCR,
   etiquetadas con el hash corto del commit.
6. **Trivy scan** *(solo `main`)* — escaneo de vulnerabilidades de las imágenes.
7. **Deploy** *(solo `main`)* — paso manual (`input`) que pide aprobación antes de desplegar.

Equivalencia con GitHub Actions:

| GitHub Actions            | Jenkins (`Jenkinsfile`)        |
|---------------------------|--------------------------------|
| workflow **CI**           | etapas Build & Test, SonarCloud |
| workflow **CD**           | etapas Package, Docker build & push, Deploy |
| workflow **Security**     | etapa Trivy scan               |

## Cómo levantarlo en local

```bash
# 1) Arrancar Jenkins (imagen propia con el CLI de Docker incluido)
docker compose -f docker-compose.jenkins.yml up -d --build

# 2) Contraseña inicial de administrador
docker exec bookplus-jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# 3) Abrir http://localhost:8080, completar el asistente (los plugins ya vienen preinstalados).
```

`jenkins/Dockerfile` extiende `jenkins/jenkins:lts-jdk21` añadiendo el **CLI de Docker** y los
plugins necesarios (Pipeline, Docker Pipeline, Git, Credentials Binding, JUnit). El compose
monta el **socket de Docker** del host para que el pipeline pueda construir imágenes.

## Configuración necesaria en Jenkins

- **Credenciales**:
  - `ghcr` (Username/Password) — usuario y token con permiso `write:packages` para GHCR.
  - `sonar-token` (Secret text) — token de SonarCloud.
- **Job**: *New Item → Pipeline → Pipeline script from SCM*, apuntando a este repositorio y al
  `Jenkinsfile` de la raíz. (O *Multibranch Pipeline* para construir todas las ramas/PRs.)

## Verificación

Esto es **infraestructura/configuración** (no hay test unitario que lo cubra). Se verifica
levantando Jenkins con el compose y ejecutando el job: las etapas deben pasar igual que en
GitHub Actions. Para producción, Jenkins iría en un servidor con agentes dedicados, no en el
contenedor local de prueba.

## Nota

Mantener **dos** sistemas de CI en paralelo en un proyecto real es redundante; aquí coexisten
solo como demostración. En un equipo se elegiría uno (GitHub Actions o Jenkins) según las
políticas de la organización.
