import { AuthUser, Role } from './auth.models';

interface JwtClaims {
  sub?: string;
  email?: string;
  username?: string;
  preferred_username?: string;
  roles?: string[] | string;
  authorities?: string[] | string;
  scope?: string;
  exp?: number;
}

function base64UrlDecode(segment: string): string {
  const padded = segment.replace(/-/g, '+').replace(/_/g, '/');
  const pad = padded.length % 4 ? '='.repeat(4 - (padded.length % 4)) : '';
  return decodeURIComponent(
    atob(padded + pad)
      .split('')
      .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
      .join(''),
  );
}

export function decodeJwt(token: string): JwtClaims | null {
  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }
  try {
    return JSON.parse(base64UrlDecode(parts[1])) as JwtClaims;
  } catch {
    return null;
  }
}

function normalizeRoles(claims: JwtClaims): Role[] {
  const raw = claims.roles ?? claims.authorities ?? claims.scope ?? [];
  const list = Array.isArray(raw) ? raw : String(raw).split(/[\s,]+/);
  return list
    .map((r) => r.replace(/^ROLE_/, '').toUpperCase())
    .filter(
      (r): r is Role =>
        r === 'USER' || r === 'REPARTIDOR' || r === 'ADMIN' || r === 'SUPERADMIN',
    );
}

export function userFromToken(token: string): AuthUser | null {
  const claims = decodeJwt(token);
  if (!claims?.sub) {
    return null;
  }
  return {
    id: claims.sub,
    email: claims.email ?? '',
    username: claims.username ?? claims.preferred_username ?? claims.email ?? claims.sub,
    roles: normalizeRoles(claims),
    emailVerified: true,
  };
}

/** True when the token is absent or its `exp` claim is in the past. */
export function isTokenExpired(token: string, skewSeconds = 30): boolean {
  const claims = decodeJwt(token);
  if (!claims?.exp) {
    return false;
  }
  return Date.now() / 1000 > claims.exp - skewSeconds;
}
