export type Role = 'USER' | 'REPARTIDOR' | 'ADMIN' | 'SUPERADMIN';

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

/** Token + identity bundle returned by auth-service on login/register/refresh. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  email: string;
  /** Backend roles, e.g. ["ROLE_USER", "ROLE_ADMIN"]. */
  roles: string[];
  emailVerified?: boolean;
}

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  roles: Role[];
  emailVerified: boolean;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

/** Normalizes backend roles (`ROLE_ADMIN`) to UI roles (`ADMIN`). */
export function normalizeRoles(roles: readonly string[] | undefined): Role[] {
  return (roles ?? [])
    .map((r) => r.replace(/^ROLE_/, '').toUpperCase())
    .filter(
      (r): r is Role =>
        r === 'USER' || r === 'REPARTIDOR' || r === 'ADMIN' || r === 'SUPERADMIN',
    );
}
