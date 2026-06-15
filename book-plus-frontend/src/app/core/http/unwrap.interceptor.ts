import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { map } from 'rxjs';

/**
 * Some services (auth, catalog, cart, inventory) wrap their payloads in an
 * `ApiResponse<T>` envelope `{ success, message, data, timestamp }`, while others
 * (order, payment, report, notification) return the payload directly.
 *
 * This interceptor unwraps the envelope transparently so every data service can
 * type its responses as the inner `T`. Detection is based on the envelope's
 * discriminating `success` boolean to avoid touching raw payloads.
 */
export const unwrapInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    map((event) => {
      if (event instanceof HttpResponse && isEnvelope(event.body)) {
        return event.clone({ body: event.body.data });
      }
      return event;
    }),
  );

interface ApiEnvelope {
  success: boolean;
  data: unknown;
}

function isEnvelope(body: unknown): body is ApiEnvelope {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as ApiEnvelope).success === 'boolean' &&
    'data' in body
  );
}
