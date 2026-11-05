/**
 * Shared auth helper — obtains a JWT access token for a given credential pair.
 * Imported by every k6 scenario that needs an authenticated session.
 */
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Login and return { accessToken, refreshToken }.
 * Fails the VU iteration if the login call does not succeed.
 */
export function login(email, password) {
  const res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  check(res, {
    'login 200': (r) => r.status === 200,
    'login has accessToken': (r) => {
      try { return !!JSON.parse(r.body).data.accessToken; } catch { return false; }
    },
  });

  const body = JSON.parse(res.body);
  return {
    accessToken:  body?.data?.accessToken  ?? '',
    refreshToken: body?.data?.refreshToken ?? '',
  };
}

/** Return standard bearer + JSON headers. */
export function authHeaders(token) {
  return {
    headers: {
      'Content-Type':  'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}
