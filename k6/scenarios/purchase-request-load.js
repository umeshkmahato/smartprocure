/**
 * Phase 16 — Purchase-Request CRUD load test
 *
 * Run:
 *   k6 run -e BASE_URL=http://localhost:8080 \
 *          -e SCENARIO=load \
 *          k6/scenarios/purchase-request-load.js
 */
import http          from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { login, authHeaders } from '../helpers/auth.js';

// ── Custom metrics ─────────────────────────────────────────────────────────
const prCreateErrors  = new Rate('pr_create_errors');
const prCreateLatency = new Trend('pr_create_duration_ms', true);
const prListLatency   = new Trend('pr_list_duration_ms',   true);
const prCreated       = new Counter('pr_total_created');

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 20 },
        { duration: '4m',  target: 60 },
        { duration: '2m',  target: 60 },
        { duration: '1m',  target: 0  },
      ],
    },
  },
  thresholds: {
    http_req_failed:       ['rate<0.01'],
    http_req_duration:     ['p(95)<800'],
    pr_create_errors:      ['rate<0.01'],
    pr_create_duration_ms: ['p(95)<600'],
    pr_list_duration_ms:   ['p(95)<400'],
  },
};

// ── VU setup: authenticate once per VU ────────────────────────────────────
export function setup() {
  return login(
    __ENV.TEST_EMAIL    || 'alice@smartprocure.dev',
    __ENV.TEST_PASSWORD || 'Test@1234!',
  );
}

export default function (tokens) {
  const hdrs = authHeaders(tokens.accessToken);

  // ── 1. Create purchase request ──────────────────────────────────────────
  const payload = JSON.stringify({
    title:          `Load Test PR — VU ${__VU} iter ${__ITER}`,
    description:    'Automated load test purchase request',
    requiredByDate: '2025-12-31',
    items: [
      { description: 'Office chairs',  quantity: 10, estimatedUnitPrice: 150.00, unit: 'PCS' },
      { description: 'Standing desks', quantity:  5, estimatedUnitPrice: 450.00, unit: 'PCS' },
    ],
  });

  const createStart = Date.now();
  const createRes = http.post(
    `${BASE}/api/v1/purchase-requests`,
    payload,
    { ...hdrs, tags: { name: 'pr_create' } },
  );
  prCreateLatency.add(Date.now() - createStart);

  const ok = check(createRes, {
    'PR create: 201':  (r) => r.status === 201,
    'PR create: id':   (r) => !!JSON.parse(r.body)?.data?.id,
  });
  prCreateErrors.add(!ok);
  if (!ok) { sleep(1); return; }

  prCreated.add(1);
  const prId = JSON.parse(createRes.body).data.id;

  sleep(0.3);

  // ── 2. Fetch the created PR ──────────────────────────────────────────────
  const getRes = http.get(
    `${BASE}/api/v1/purchase-requests/${prId}`,
    { ...hdrs, tags: { name: 'pr_get' } },
  );
  check(getRes, { 'PR get: 200': (r) => r.status === 200 });

  sleep(0.3);

  // ── 3. List my PRs ───────────────────────────────────────────────────────
  const listStart = Date.now();
  const listRes = http.get(
    `${BASE}/api/v1/purchase-requests/my?page=0&size=10`,
    { ...hdrs, tags: { name: 'pr_list_my' } },
  );
  prListLatency.add(Date.now() - listStart);
  check(listRes, { 'PR list: 200': (r) => r.status === 200 });

  sleep(1);
}
