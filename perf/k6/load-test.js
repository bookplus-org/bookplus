import http from 'k6/http';
import { check, sleep, group } from 'k6';

// Prueba de carga del camino de lectura más caliente de la tienda (catálogo),
// a través de la API Gateway. Simula usuarios navegando, buscando y abriendo libros.
export const options = {
  stages: [
    { duration: '30s', target: 20 }, // rampa hasta 20 usuarios virtuales
    { duration: '1m',  target: 20 }, // sostiene la carga
    { duration: '20s', target: 0 },  // baja
  ],
  thresholds: {
    http_req_failed:   ['rate<0.01'],   // < 1% de errores
    http_req_duration: ['p(95)<800'],   // p95 por debajo de 800 ms
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export default function () {
  let bookId;

  group('browse catalog', () => {
    const cats = http.get(`${BASE}/categories`);
    check(cats, { 'categories 200': (r) => r.status === 200 });

    const list = http.get(`${BASE}/books?page=0&size=20`);
    check(list, { 'books list 200': (r) => r.status === 200 });

    // Extrae un id de libro del listado para encadenar el detalle (estructura ApiResponse<Paged>).
    try {
      const data = list.json('data');
      const content = data && (data.content || data.items || data);
      if (Array.isArray(content) && content.length > 0) {
        bookId = content[0].id;
      }
    } catch (e) { /* respuesta inesperada: el check anterior ya lo refleja */ }
  });

  group('search', () => {
    const res = http.get(`${BASE}/books/search?q=clean&page=0&size=10`);
    check(res, { 'search 200': (r) => r.status === 200 });
  });

  if (bookId) {
    group('book detail', () => {
      const detail = http.get(`${BASE}/books/${bookId}`);
      check(detail, { 'book detail 200': (r) => r.status === 200 });

      const reviews = http.get(`${BASE}/books/${bookId}/reviews?page=0&size=10`);
      check(reviews, { 'reviews 200': (r) => r.status === 200 });
    });
  }

  sleep(1);
}
