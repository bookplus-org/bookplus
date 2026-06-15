# BookPlus — Despliegue en Oracle Cloud (Always Free) + Endurecimiento

Guía práctica para desplegar el stack en una instancia gratuita de **Oracle Cloud**
(ARM Ampere, hasta 4 vCPU / 24 GB RAM sin costo) usando Docker Compose, y para **exponerla con
seguridad** (o mantenerla privada).

> Tu CD ya publica imágenes **multi-arquitectura** (arm64 + amd64), así que las mismas imágenes
> sirven para Oracle (ARM) y para AWS (x86) el día que migres, sin recompilar.

---

## 1. Crear el servidor

1. Crea una cuenta en **https://cloud.oracle.com** (el tier *Always Free* no caduca).
2. **Compute → Instances → Create Instance**:
   - **Shape:** `VM.Standard.A1.Flex` (Ampere/ARM). Asigna **4 OCPU y 24 GB RAM** (entran en el free tier).
   - **Image:** Ubuntu 22.04 (LTS).
   - **SSH:** sube tu clave pública (o genera un par y guarda la privada).
3. Anota la **IP pública** de la instancia.

---

## 2. Abrir puertos (¡dos capas!)

Oracle bloquea el tráfico en **dos** sitios; hay que abrir en ambos. **Abre solo 80 y 443**
(web), nunca las bases de datos.

**a) Security List (firewall de Oracle, en la consola web):**
VCN → Subnet → Security List → *Add Ingress Rules*: permite `TCP 80` y `TCP 443` desde `0.0.0.0/0`.

**b) Firewall del sistema operativo (Ubuntu trae iptables que bloquea por defecto):**
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

---

## 3. Instalar Docker

```bash
sudo apt-get update && sudo apt-get install -y ca-certificates curl git
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker   # usar docker sin sudo
```

---

## 4. Desplegar el stack

```bash
git clone <URL_DE_TU_REPO> bookplus && cd bookplus

# Inicia sesión en GHCR para descargar las imágenes (token con scope read:packages)
echo "<TU_GHCR_TOKEN>" | docker login ghcr.io -u <tu-usuario> --password-stdin

# Crea el .env (ver §5) con las claves y:
#   REGISTRY=ghcr.io/<tu-usuario>
#   TAG=latest

docker compose -f docker-compose.deploy.yml pull
docker compose -f docker-compose.deploy.yml up -d
docker compose -f docker-compose.deploy.yml ps
```

A partir de aquí, el **CD de GitHub** puede hacer esto solo en cada push si configuras los
secretos SSH (ver `CI-CD.md`).

---

## 5. Endurecimiento de seguridad (IMPORTANTE antes de exponer)

En local nada de esto importa; en internet, sí. Mínimos imprescindibles:

### 5.1 Cambia TODOS los secretos por defecto
En el `.env`: contraseñas de los PostgreSQL, el par de claves **JWT** (genera uno nuevo, no
uses el del repo), credenciales de correo, etc. Las claves del repositorio son de desarrollo y
**son públicas**.

### 5.2 Expón solo el frontend y el gateway
Edita `docker-compose.deploy.yml` para que **solo** publiquen puerto al exterior el
`frontend` y el `api-gateway`. A todo lo demás (PostgreSQL, Kafka, Elasticsearch, Redis,
microservicios internos) **quítale el `ports:`** o bíndalo a localhost. Ejemplo para una BD:
```yaml
    ports:
      - "127.0.0.1:5436:5432"   # accesible solo desde el propio servidor, no desde internet
```
> Aunque el firewall ya bloquee esos puertos, esto es *defensa en profundidad*: si algún día
> abres un puerto por error, las BD siguen sin estar expuestas.

### 5.3 Quita las herramientas de desarrollo del acceso público
**MailHog** (8025) y **Zipkin** (9411) son para desarrollo; no deben ser accesibles desde
internet. Bíndalas a `127.0.0.1` o elimínalas del compose de producción. Para el correo real,
configura un SMTP de verdad en vez de MailHog.

### 5.4 Pon HTTPS (gratis con Let's Encrypt)
Lo más sencillo es poner **Caddy** como reverse proxy delante del frontend/gateway: obtiene y
renueva el certificado TLS automáticamente con solo indicar tu dominio. Alternativa clásica:
Nginx + Certbot. Necesitas un **dominio** apuntando a la IP del servidor.

### 5.5 Buenas prácticas de servidor
- Deshabilita el login SSH por contraseña (solo clave): en `/etc/ssh/sshd_config` pon
  `PasswordAuthentication no`.
- Mantén el sistema actualizado: `sudo apt-get update && sudo apt-get upgrade -y`.

---

## 6. Opción intermedia: en la nube pero PRIVADO (recomendado para aprender)

Si quieres probar el despliegue real sin abrirlo a todo internet, instala **Tailscale** (VPN
gratuita) en el servidor y en tu PC. Entras a la app por la IP privada de Tailscale; nadie más
puede verla. Así no necesitas abrir 80/443 al mundo ni configurar HTTPS todavía.

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```

Luego accede desde tu PC (con Tailscale instalado) a `http://<ip-tailscale-del-servidor>:4200`.

---

## 7. ¿"Exponer" = lo ve todo el mundo?

- **Local (`localhost`)** → privado total, solo tú.
- **Servidor + Tailscale/VPN** → en la nube, pero solo tú (sin abrir puertos al mundo). *Ideal para empezar.*
- **Servidor con 80/443 abiertos + dominio + HTTPS** → público en internet. Hazlo solo cuando
  hayas aplicado §5 (secretos cambiados, BD no expuestas, MailHog/Zipkin fuera, HTTPS puesto).

---

## 8. Migrar a AWS (u otro) más adelante

Es directo porque todo es Docker:
1. Crea el servidor nuevo, instala Docker, clona el repo, copia el `.env`.
2. `docker compose -f docker-compose.deploy.yml pull && up -d` (las imágenes multi-arch ya
   sirven tanto en x86 como en ARM).
3. **Migra los datos:** `pg_dump` de cada PostgreSQL en el viejo → `restore` en el nuevo.
4. Apunta el dominio a la IP nueva.

Nada de reescribir código: cambia el servidor, no la aplicación.
