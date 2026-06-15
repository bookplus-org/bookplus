/** Production environment. Set the gateway origin at build/deploy time. */
export const environment = {
  production: true,
  apiBaseUrl: '/api/v1',
} as const;
