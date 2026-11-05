/**
 * Phase 16 — Auth load test
 *
 * Scenarios
 *  • smoke   : 1 VU × 30 s  — basic sanity
 *  • load    : ramp to 50 VUs over 2 m, hold 5 m, ramp down
 *  • stress  : ramp to 150 VUs — find the breaking point
 *
 * Run:
 *   k6 run -e BASE_URL=http://localhost:8080 \
 *          -e SCENARIO=load \
 *          k6/scenarios/auth-load.js
 */
import http   from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { login, authHeaders } from '../helpers/auth.js';

// ── Custom metrics ────────────────────────────────────────────────────────
const loginErrorRate   = new Rate('auth_login_errors');
const loginDuration    = new Trend('auth_login_duration_ms', true);
const refreshDuration  = new Trend('auth_refresh_duration_ms', true);

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// ── Scenario selector ─────────────────────────────────────────────────────
const SCENARIOS = {
  smoke: {
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
    },
  },
  load: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 20  },
        { duration: '3m',  target: 50  },
        { duration: '2m',  target: 50  },
        { duration: '1m',  target: 0   },
      ],
    },
  },
  stress: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m',  target: 50  },
        { duration: '3m',  target: 100 },
        { duration: '2m',  target: 150 },
        { duration: '2m',  target: 0   },
      ],
    },
  },
};

export const options = {
  scenarios: SCENARIOS[__ENV.SCENARIO ?? 'load'],
  thresholds: {
    http_req_failed:          ['rate<0.01'],          // <1 % errors
    http_req_duration:        ['p(95)<500'],           // 95 % of requests under 500 ms
    auth_login_errors:        ['rate<0.01'],
    auth_login_duration_ms:   ['p(95)<400'],
    auth_refresh_duration_ms: ['p(95)<300'],
  },
};

// One registered test user is expected to exist (seed data / fixture)
const TEST_EMAIL    = __ENV.TEST_EMAIL    || 'alice@smartprocure.dev';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'Test@1234!';

export default function () {
  // ── 1. Login ──────────────────────────────────────────────────────────────
  const loginStart = Date.now();
  const loginRes = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_login' } },
  );
  loginDuration.add(Date.now() - loginStart);

  const loginOk = check(loginRes, {
    'login: status 200':       (r) => r.status === 200,
    'login: has accessToken':  (r) => !!JSON.parse(r.body)?.data?.accessToken,
  });
  loginErrorRate.add(!loginOk);
  if (!loginOk) { sleep(1); return; }

  const { accessToken, refreshToken } = JSON.parse(loginRes.body).data;

  sleep(0.5);

  // ── 2. /me ────────────────────────────────────────────────────────────────
  const meRes = http.get(
    `${BASE}/api/v1/auth/me`,
    { ...authHeaders(accessToken), tags: { name: 'auth_me' } },
  );
  check(meRes, { 'GET /me: 200': (r) => r.status === 200 });

  sleep(0.5);

  // ── 3. Token refresh ──────────────────────────────────────────────────────
  const refreshStart = Date.now();
  const refreshRes = http.post(
    `${BASE}/api/v1/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'auth_refresh' } },
  );
  refreshDuration.add(Date.now() - refreshStart);
  check(refreshRes, { 'refresh: status 200': (r) => r.status === 200 });

  sleep(1);
}
