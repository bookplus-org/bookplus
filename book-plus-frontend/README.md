# BookPlus — Frontend (Angular 21)

SPA de tienda y panel de administración para la plataforma de microservicios BookPlus.
Consume exclusivamente el **API Gateway** (`http://localhost:8080`).

## Stack

- **Angular 21** — standalone components, **signals**, control-flow `@if/@for/@switch`.
- **Angular Material** (M3) + **Tailwind CSS** (utilidades; preflight desactivado para no chocar con MDC).
- **HttpClient** con interceptores funcionales (JWT + refresh transparente, errores RFC 7807).
- **Reactive Forms** con validación cliente y mapeo de errores de servidor por campo.
- Guards funcionales: `authGuard`, `guestGuard`, `roleGuard('ADMIN')`.

## Arquitectura

```
src/app/
├── core/            # singletons: auth (store con signals, interceptors, guards, jwt),
│   │                #            http (error interceptor), notifications, models
│   ├── auth/
│   ├── http/
│   ├── models/
│   └── notifications/
├── shared/          # UI reutilizable (book-card), helpers de formularios, páginas (404)
├── layout/          # shells: store-layout, auth-layout, admin-layout
└── features/        # por dominio, lazy-loaded
    ├── auth/        # login, registro
    ├── catalog/     # listado + detalle (data service, modelos)
    ├── cart/        # carrito (store con signals + checkout)
    ├── orders/      # historial + detalle de pedidos
    └── admin/       # dashboard + secciones (gated por rol ADMIN)
```

Alias de TypeScript: `@core/*`, `@shared/*`, `@features/*`, `@env/*`.

## Requisitos

- Node.js 20+ y npm.
- El backend levantado (`make up` en la raíz del repo) para que el proxy alcance el gateway.

## Desarrollo

```bash
npm install
npm start          # ng serve en http://localhost:4200 (proxy → gateway:8080)
```

El `proxy.conf.json` redirige `/api` a `http://localhost:8080`, evitando CORS en desarrollo.
El gateway ya admite el origen `http://localhost:4200` (ver `CORS_ALLOWED_ORIGINS`).

## Scripts

```bash
npm start        # servidor de desarrollo
npm run build    # build de producción (dist/)
npm test         # tests unitarios (Karma + Jasmine)
npm run lint     # ESLint
npm run typecheck# chequeo de tipos sin emitir
npm run format   # Prettier
```

## Pantallas

Tienda (cliente):
- Catálogo: listado con búsqueda/filtros + paginación, y detalle con reseñas.
- Detalle de libro: ficha completa, reseñas y formulario para publicar reseña (autenticado).
- Carrito: edición de cantidades y subtotal.
- Checkout: formulario de dirección de envío + resumen → crea el pedido.
- Pedidos: historial y detalle con seguimiento de estado y cancelación.
- Cuenta: datos del usuario y accesos rápidos.
- Auth: login y registro.

Admin (rol ADMIN):
- Dashboard de ventas (report-service vía admin-bff).
- Catálogo: tabla con alta/edición (formulario) y borrado con confirmación.
- Categorías: alta y borrado.
- Inventario: tabla de stock con alerta de stock bajo y diálogo de ajuste.

## Estado

- ✅ Flujo cliente completo: auth → catálogo → carrito → checkout → pedidos → cuenta.
- ✅ Recuperación de contraseña (forgot/reset) y bandeja de notificaciones.
- ✅ Visor de previsualización (muestra PDF) en el detalle del libro.
- ✅ Panel admin: dashboard con gráfico + exportación CSV/PDF, CRUD de catálogo/categorías, ajuste de inventario.
- ✅ Núcleo: sesión con signals, refresh automático de token, manejo de errores unificado.
- 🚧 Pendiente: tests unitarios/e2e.

## Contrato con el backend (alineado con los DTOs reales)

- **Envoltura `ApiResponse<T>`**: auth/catalog/cart*/inventory envuelven en
  `{ success, message, data, timestamp }`; order/payment/report/notification devuelven
  el payload directo. El `unwrapInterceptor` desenvuelve el sobre transparentemente, así
  cada servicio del frontend tipa la respuesta como `T`.
- **Errores RFC 7807 ProblemDetail** con `type` bajo `https://bookplus.com/errors/…`,
  `timestamp` y, en validación, mapa `errors` (campo → mensaje) pintado inline.
- **Login** usa `usernameOrEmail`. La respuesta de auth incluye `userId/username/email/roles`,
  de donde se construye la sesión (sin decodificar el JWT).
- **Carrito**: `add` envía el ítem denormalizado (isbn, título, imagen, precio…) que pide
  cart-service. El checkout no lleva body y devuelve 204; la orden se crea por evento Kafka.
- **Gateway**: se añadieron las rutas `/api/v1/reports/**` y `/api/v1/admin/**` (antes el
  gateway no las enrutaba), necesarias para el dashboard y reportes.

### Pendiente en backend para el visor

El visor lee `book.previewUrl`. catalog-service aún no expone ese campo: para activarlo,
añadir `previewUrl` a `BookResponse` (y su origen en el dominio/almacenamiento de objetos).
Mientras tanto el visor muestra "sin vista previa disponible".
