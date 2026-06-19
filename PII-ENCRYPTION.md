# Cifrado de PII en reposo

La información personal identificable (PII) —nombre y dirección del destinatario de un
pedido— no debería quedar en texto plano en la base de datos: si alguien obtiene un volcado
o accede al disco, la lee directamente. Reglamentos como el RGPD exigen proteger estos
datos. Lo resolvemos cifrándolos **en reposo** de forma transparente para la aplicación.

## Cómo funciona

Usamos un **JPA `AttributeConverter`** (`CryptoConverter`) anotado con `@Convert` sobre los
campos sensibles. JPA lo invoca automáticamente:

- al **guardar**, cifra el valor antes de escribirlo en la columna;
- al **leer**, lo descifra antes de entregárselo a la entidad.

El resto del código (servicios, mappers, API) trabaja siempre con el texto plano y **no se
entera** del cifrado. En la base de datos, en cambio, solo hay ciphertext.

### Algoritmo

**AES-256-GCM**, cifrado *autenticado*: cada valor se cifra con un **IV aleatorio** que se
antepone al texto cifrado, y el conjunto se guarda en Base64. Dos consecuencias importantes:

- El mismo valor produce **textos cifrados distintos** cada vez (el IV cambia), lo que evita
  filtrar igualdades.
- GCM añade un tag de autenticación: si alguien **manipula** el dato en reposo, el descifrado
  **falla** en vez de devolver basura silenciosamente.

## Dónde está aplicado

En order-service, sobre la dirección de envío (PII que nunca se usa en cláusulas `WHERE`,
así que cifrarla no rompe ninguna consulta):

- `shipping_recipient_name`
- `shipping_street`

La migración **V18** amplía esas columnas a `TEXT` (en `orders` y en la tabla de auditoría
`orders_aud` de Envers), porque el ciphertext ocupa más que el texto plano.

## La clave

La clave AES (32 bytes, Base64) se sirve por la variable de entorno / propiedad
`PII_ENCRYPTION_KEY` — en producción la entrega **Vault** (ya integrado). Si no está, se usa
una clave de **desarrollo** embebida (solo para local/tests; nunca en producción).

```bash
# Producción: clave fuerte desde Vault / entorno
PII_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

## Verificación

- **Unit test** `CryptoConverterTest` (sin Spring ni BD, corre en el `mvn test` normal):
  ida y vuelta, ciphertext distinto por IV, null seguro y fallo ante manipulación.
- **Test de integración** `OrderAuditTrailIntegrationTest` (Testcontainers): además de la
  auditoría, comprueba que el valor **crudo** almacenado en `orders` **no** es el texto plano
  y que la entidad lo devuelve descifrado correctamente.

## Consideraciones

- **Datos existentes**: las filas previas en texto plano deben re-cifrarse con una migración
  de datos antes de activar el cifrado en un entorno con datos reales (en este proyecto la
  base arranca limpia, así que no aplica).
- **Búsquedas**: no se cifran campos usados en filtros (`WHERE`). Para buscar por un dato
  cifrado haría falta cifrado *determinista* o un índice ciego (*blind index*).

## Siguiente nivel

- **Rotación de claves** con versionado del prefijo del ciphertext (qué clave lo cifró).
- **Envelope encryption** con un KMS (la clave de datos se cifra con una clave maestra del
  proveedor de nube).
- Extender el cifrado a otra PII (p. ej. teléfono o documento) si el dominio lo incorpora.
