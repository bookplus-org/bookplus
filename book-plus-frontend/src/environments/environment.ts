/** Development environment. API calls go through the Angular dev-server proxy → gateway:8080. */
export const environment = {
  production: false,
  apiBaseUrl: '/api/v1',
} as const;
