import { isTokenExpired, userFromToken } from './jwt.util';

/** Builds an unsigned JWT (header.payload.signature) for testing claim parsing. */
function fakeJwt(claims: Record<string, unknown>): string {
  const b64 = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64({ alg: 'none' })}.${b64(claims)}.sig`;
}

describe('jwt.util', () => {
  it('extracts the user and normalizes ROLE_ prefixes', () => {
    const token = fakeJwt({ sub: 'u1', email: 'a@b.com', roles: ['ROLE_USER', 'ROLE_ADMIN'] });
    const user = userFromToken(token);
    expect(user).toEqual({ id: 'u1', email: 'a@b.com', username: 'a@b.com', roles: ['USER', 'ADMIN'] });
  });

  it('returns null for a malformed token', () => {
    expect(userFromToken('not-a-jwt')).toBeNull();
  });

  it('detects an expired token', () => {
    const expired = fakeJwt({ sub: 'u1', exp: Math.floor(Date.now() / 1000) - 100 });
    const valid = fakeJwt({ sub: 'u1', exp: Math.floor(Date.now() / 1000) + 3600 });
    expect(isTokenExpired(expired)).toBeTrue();
    expect(isTokenExpired(valid)).toBeFalse();
  });
});
