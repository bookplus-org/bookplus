# Integración de Keycloak (OAuth2 / OIDC)

Keycloak es un **proveedor de identidad** estándar de la industria. Hoy BookPlus emite
sus propios JWT desde `auth-service` (con JJWT) y los valida en el gateway. Keycloak
permite delegar toda esa responsabilidad a un servicio especializado, ganando SSO,
refresh tokens, login social, MFA y una consola de administración de usuarios.

Este documento describe la **ruta de migración**. El JWT propio actual sigue funcionando;
Keycloak se añade como infraestructura para poder hacer el cambio de forma controlada.

## Qué se ha añadido

- **Servicio `keycloak`** en `docker-compose.full.yml` (puerto `8180`), en modo `start-dev`.
- **Realm `bookplus`** (`keycloak/bookplus-realm.json`) que se importa al arrancar, con:
  - Los roles del dominio: `ROLE_USER`, `ROLE_REPARTIDOR`, `ROLE_EDITOR`, `ROLE_ADMIN`, `ROLE_SUPERADMIN`.
  - Un cliente público `bookplus-frontend` (la SPA Angular).
  - Un cliente `bookplus-gateway` (bearer-only, para validar tokens).
  - Un usuario admin de ejemplo (`admin@bookplus.dev` / `admin123`).

Consola: `http://localhost:8180` (admin / admin).
Issuer del realm: `http://localhost:8180/realms/bookplus`.

## Ruta de cutover (cuando se decida migrar)

1. **Gateway como Resource Server.** Cambiar la validación del gateway para que use el
   JWKS de Keycloak en lugar de la clave propia:

   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://keycloak:8080/realms/bookplus
   ```

   Spring descarga las claves públicas del realm automáticamente y valida la firma.

2. **Servicios internos.** Los que ya son Resource Server (cart, payment, etc.) solo
   necesitan el mismo `issuer-uri`. Mapear el claim de roles de Keycloak (`realm_access.roles`)
   a las autoridades de Spring con un `JwtAuthenticationConverter`.

3. **Frontend.** Sustituir el login propio por el flujo OIDC *Authorization Code + PKCE*
   contra el cliente `bookplus-frontend` (p. ej. con `angular-oauth2-oidc` o `keycloak-js`).

4. **auth-service.** Pasa a ser opcional: la emisión de tokens la hace Keycloak. Puede
   mantenerse solo para datos de perfil propios, o retirarse.

5. **Migración de usuarios.** Importar los usuarios existentes al realm (vía la Admin API
   o un proveedor de federación de usuarios apuntando a la base actual de `auth-service`).

## Por qué hacerlo por fases

Cambiar la identidad de golpe rompería login, gateway y todos los servicios a la vez.
Teniendo Keycloak ya levantado, se puede validar el flujo en local (obtener un token del
realm y llamar al gateway) antes de retirar el JWT propio. La migración se valida de
extremo a extremo con el stack en marcha.
