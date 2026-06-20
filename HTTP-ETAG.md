# Caché HTTP condicional (ETag / 304 Not Modified)

Cuando un cliente pide repetidamente un recurso que **no ha cambiado** (una ficha de libro,
una categoría), reenviar el cuerpo completo cada vez malgasta ancho de banda y trabajo de
serialización. La caché condicional con **ETag** evita ese reenvío.

## Cómo funciona

1. En la primera respuesta, el servidor calcula un **ETag** (un hash del cuerpo) y lo envía en
   la cabecera `ETag`.
2. El cliente guarda la respuesta y, la próxima vez, pregunta con `If-None-Match: <etag>`.
3. Si el contenido **no cambió**, el servidor responde **`304 Not Modified`** **sin cuerpo**.
   El cliente reutiliza su copia. Si cambió, responde `200` con el nuevo cuerpo y un ETag
   nuevo.

## La integración

En catalog-service, `EtagConfig` registra el `ShallowEtagHeaderFilter` de Spring para las
rutas de lectura del catálogo (`/api/v1/books/*`, `/api/v1/categories/*`). Es **transparente**:
los controllers no cambian; el filtro calcula el ETag a partir de la respuesta ya generada y
gestiona el `304` automáticamente.

Es un ETag "shallow" (calculado del cuerpo serializado): ahorra **ancho de banda** (no se
reenvía el cuerpo) aunque el servidor sí genere la respuesta. Combinado con la **caché de dos
niveles** (Caffeine + Redis), esa generación es además muy barata.

## Verificación

`EtagFilterTest` (unit, con request/response simulados) comprueba que la primera petición
devuelve `200` con `ETag`, y que una segunda con `If-None-Match` recibe `304` sin cuerpo.
Corre en el `mvn test` normal.

## Siguiente nivel

- **ETag fuerte basado en versión**: usar la versión/`updatedAt` del recurso para calcular el
  ETag sin tener que serializar el cuerpo (evita también el trabajo de generación).
- **Cache-Control** (`max-age`) para que el navegador ni pregunte durante un tiempo.
