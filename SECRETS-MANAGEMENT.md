# Gestión de secretos en CI/CD (cómo se hace en una empresa de verdad)

Un token personal (PAT) que caduca y hay que renovar a mano **no es** como se gestiona en
producción: no escala, es frágil y un humano olvidándose de rotarlo es un incidente esperando
a ocurrir. Las empresas serias usan **credenciales efímeras y de rotación automática**. Aquí
está el modelo, de menos a más profesional, y cómo aplica a este proyecto.

## El principio

> No guardes secretos de larga vida. Que la identidad de la máquina/pipeline obtenga un
> **token de corta vida** justo cuando lo necesita, y que caduque solo.

## Nivel 1 — PAT con caducidad (lo mínimo / portafolio)

Un Personal Access Token con expiración. Cuando caduca, GitHub avisa por email y hay que
**regenerarlo a mano**. Sirve para un proyecto personal, pero **no se usa en empresa**: no hay
rotación automática y el secreto es de larga vida.

## Nivel 2 — OIDC / Workload Identity Federation (el estándar moderno)

La CI **no guarda ningún secreto**. En cada ejecución pide un token **efímero** (vive minutos)
al proveedor (GitHub, AWS, GCP, Azure) mediante una **relación de confianza basada en
identidad** (OIDC). Nada que rotar, nada que filtrar.

- **Ya lo usas sin saberlo**: el workflow de GitHub Actions publica imágenes a GHCR con el
  `GITHUB_TOKEN` automático — un token efímero que GitHub crea y caduca en cada run. Por eso en
  tus *Secrets* de GitHub **no hay** ningún token de GHCR: no hace falta. Eso ES esta práctica.
- Para desplegar a la nube (AWS/GCP) desde la CI, se configura **OIDC federation**: el rol de
  destino confía en la identidad del workflow; cero claves guardadas.

## Nivel 3 — GitHub App (lo profesional para Jenkins)

Jenkins, al estar fuera de GitHub, no tiene un `GITHUB_TOKEN` automático. La forma profesional
**no es** un PAT personal, sino una **GitHub App**:

- Creas una App (una identidad de máquina, no tuya) con permiso `packages: write` y la instalas
  en la organización/repo.
- En Jenkins registras una credencial de tipo **GitHub App** (App ID + clave privada).
- Jenkins **acuña automáticamente tokens de instalación de 1 hora** en cada build y los renueva
  solo. El único secreto de larga vida es la clave privada de la App, que vive en el gestor de
  secretos, no en manos de una persona.

### Cómo activarlo en este proyecto

1. GitHub → *Settings → Developer settings → GitHub Apps → New GitHub App*.
   - Permisos del repositorio: **Packages: Read and write** (y *Contents: Read* para el SCM).
   - Crea la App, genera una **private key** (`.pem`) y anota el **App ID**.
   - *Install App* en tu cuenta/repo `bookplus`.
2. Coloca el `.pem` en `jenkins/casc/github-app.pem` (gitignored) y rellena en el `.env`:
   `GITHUB_APP_ID=...`.
3. Descomenta el bloque `gitHubApp` de `jenkins/casc/jenkins.yaml` (ya está preparado) y
   reinicia Jenkins.
4. En el `Jenkinsfile`, pásale la credencial:
   ```groovy
   bookplusPipeline(registry: 'ghcr.io/dhuarocc', ghcrCredentialsId: 'github-app')
   ```
   El pipeline ya está parametrizado (`ghcrCredentialsId`, por defecto `ghcr`), así que cambiar
   a la App es **una línea**. A partir de ahí, **nadie renueva tokens**.

## Nivel 4 — Gestor de secretos con rotación (Vault)

Para credenciales que no son de GitHub (BD, APIs, claves de cifrado), un **gestor de secretos**
como **HashiCorp Vault** (ya integrado en el backend) emite credenciales **dinámicas** de corta
vida y las **rota automáticamente**. Jenkins las pide al vuelo con `withVault` (plugin ya
instalado y configurado en el JCasC, ver `JENKINS-EXPERT.md`); nadie las teclea.

## Resumen — qué usar y cuándo

| Escenario | Solución profesional |
|-----------|----------------------|
| GitHub Actions → GHCR | `GITHUB_TOKEN` automático (ya lo usas) |
| GitHub Actions → nube | OIDC federation (sin secretos) |
| Jenkins → GHCR / SCM | **GitHub App** (tokens efímeros de 1 h) |
| Secretos de runtime (BD, APIs) | **Vault** (dinámicos + rotación) |
| Portafolio / local | PAT con caducidad (lo mínimo aceptable) |

La regla de oro: **el humano no renueva nada**. Si en tu pipeline hay un secreto que alguien
tiene que rotar a mano cada X meses, todavía no es nivel producción.

## En este repositorio

- GitHub Actions ya está en Nivel 2 (token automático para GHCR).
- Jenkins está preparado para Nivel 3 (credencial **GitHub App** lista para activar) y Nivel 4
  (plugin de **Vault** configurado por JCasC).
- El PAT del `.env` es solo el Nivel 1 para arrancar rápido en local; en cuanto crees la GitHub
  App, cambia `ghcrCredentialsId` a `github-app` y olvídate de rotar tokens.
