# Integración de HashiCorp Vault (gestión de secretos)

Vault almacena los secretos (contraseñas de base de datos, claves, tokens) **cifrados** y
los entrega a los servicios bajo demanda, en lugar de tenerlos en texto plano en variables
de entorno o en el `docker-compose`. Es una práctica estándar y casi obligatoria en banca
y grandes empresas.

En BookPlus se integra de forma **opt-in** y **sin romper nada**: el arranque normal sigue
usando las variables de entorno; Vault se activa solo cuando se quiere.

## Qué se ha añadido

- **Servicio `vault`** (modo dev) en `docker-compose.full.yml`, puerto `8200`.
  UI/API en `http://localhost:8200`, token raíz `bookplus-root`.
- **Servicio `vault-init`**: ejecuta `vault/seed.sh` para cargar los secretos en Vault
  una vez que está listo, y termina.
- **auth-service** integrado con **Spring Cloud Vault**, activo solo con el perfil `vault`
  (`application-vault.yml`). Lee `spring.datasource.username` / `password` desde la ruta
  KV `secret/bookplus/auth`.

## Cómo probarlo en local

1. Levanta Vault y carga los secretos:

   ```bash
   docker compose -f docker-compose.full.yml up -d vault vault-init
   ```

   Comprueba en la UI (`http://localhost:8200`, token `bookplus-root`) que existe
   `secret/bookplus/auth` con el usuario y la contraseña.

2. Arranca auth-service con el perfil `vault`. En `docker-compose.full.yml`, en el
   servicio `auth-service`, añade:

   ```yaml
   environment:
     SPRING_PROFILES_ACTIVE: vault
     VAULT_URI: http://vault:8200
     VAULT_TOKEN: bookplus-root
   depends_on:
     vault-init:
       condition: service_completed_successfully
   ```

   Al arrancar, auth-service tomará las credenciales de la base de datos **desde Vault**
   en vez de las variables de entorno.

## Cómo funciona

`spring-cloud-starter-vault-config` + `spring.config.import: vault://` hacen que, en el
arranque, Spring descargue las claves de `secret/bookplus/auth` y las inyecte como
propiedades de configuración, sobreescribiendo los valores por defecto. Como el `import`
vive solo en `application-vault.yml`, sin ese perfil el servicio se comporta igual que antes.

## Camino a producción

- Sustituir el **modo dev** (memoria, token raíz fijo) por Vault con almacenamiento
  persistente y *unseal* real.
- Autenticación por **AppRole** o **Kubernetes auth** en vez de token estático.
- **Rotación dinámica** de credenciales de base de datos (Vault genera usuarios temporales).
- Extender la integración al resto de servicios (cada uno con su ruta `secret/bookplus/<servicio>`).
