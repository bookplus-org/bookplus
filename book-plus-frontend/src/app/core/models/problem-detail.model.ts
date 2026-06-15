/**
 * RFC 7807 ProblemDetail — the unified error contract returned by every
 * BookPlus microservice through the gateway.
 *
 * Example:
 * {
 *   "type": "https://bookplus.com/errors/validation-error",
 *   "title": "Validation Error",
 *   "status": 400,
 *   "detail": "Validation failed",
 *   "timestamp": "2026-05-30T12:34:56.789Z",
 *   "errors": { "email": "must be a valid email" }
 * }
 */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  /** Field-level validation messages, present on 400 validation errors. */
  errors?: Record<string, string>;
}

export function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    'title' in value &&
    'status' in value &&
    typeof (value as ProblemDetail).status === 'number'
  );
}
