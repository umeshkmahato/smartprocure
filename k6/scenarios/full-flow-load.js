/**
 * Phase 16 — Full end-to-end procurement flow load test
 *
 * Flow per VU iteration:
 *   1. Login (Procurement Officer)
 *   2. Create Purchase Request
 *   3. Initiate Approval Workflow
 *   4. Login (Approver / Manager)
 *   5. Approve the PR
 *   6. Create RFQ linked to the PR
 *   7. Submit Vendor Quotation
 *   8. Issue Purchase Order
 *   9. Submit Invoice
 *  10. Verify Invoice
 *
 * Run:
 *   k6 run -e BASE_URL=http://localhost:8080 \
 *          -e SCENARIO=load \
 *          k6/scenarios/full-flow-load.js
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { login, authHeaders } from '../helpers/auth.js';

// ── Custom metrics ─────────────────────────────────────────────────────────
const flowErrors       = new Rate('flow_errors');
const flowDuration     = new Trend('flow_e2e_duration_ms', true);
const flowsCompleted   = new Counter('flow_completed_total');
const stepLatency      = new Trend('flow_step_duration_ms', true);

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// Accounts must exist in seed data
const OFFICER_EMAIL    = __ENV.OFFICER_EMAIL    || 'alice@smartprocure.dev';
const OFFICER_PASSWORD = __ENV.OFFICER_PASSWORD || 'Test@1234!';
const APPROVER_EMAIL   = __ENV.APPROVER_EMAIL   || 'bob@smartprocure.dev';
const APPROVER_PASSWORD= __ENV.APPROVER_PASSWORD|| 'Test@1234!';
const VENDOR_ID        = __ENV.VENDOR_ID        || '1';

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '1m',
      env: { SCENARIO: 'smoke' },
      tags: { scenario: 'smoke' },
    },
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 10 },
        { duration: '5m', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '1m', target: 0  },
      ],
      tags: { scenario: 'load' },
    },
  },
  thresholds: {
    http_req_failed:       ['rate<0.02'],
    http_req_duration:     ['p(95)<1500'],
    flow_errors:           ['rate<0.02'],
    flow_e2e_duration_ms:  ['p(95)<12000'],  // full flow < 12 s p95
    flow_step_duration_ms: ['p(95)<1000'],
  },
};

// ── Helpers ────────────────────────────────────────────────────────────────
function step(name, fn) {
  const start = Date.now();
  const result = fn();
  stepLatency.add(Date.now() - start, { step: name });
  return result;
}

function post(url, body, hdrs, tag) {
  return http.post(url, JSON.stringify(body), {
    ...hdrs,
    tags: { name: tag },
  });
}

function patch(url, body, hdrs, tag) {
  return http.patch(url, JSON.stringify(body), {
    ...hdrs,
    tags: { name: tag },
  });
}

// ── Main VU function ───────────────────────────────────────────────────────
export default function () {
  const flowStart = Date.now();
  let   hasError  = false;

  // ── Step 1: Login as Procurement Officer ──────────────────────────────────
  let officerToken, approverToken;

  group('1_login_officer', () => {
    const tokens = step('login_officer', () =>
      login(OFFICER_EMAIL, OFFICER_PASSWORD),
    );
    officerToken = tokens.accessToken;
    if (!officerToken) { hasError = true; }
  });
  if (hasError) { flowErrors.add(1); return; }

  const oHdrs = authHeaders(officerToken);
  sleep(0.3);

  // ── Step 2: Create Purchase Request ──────────────────────────────────────
  let prId;
  group('2_create_pr', () => {
    const res = step('pr_create', () =>
      post(`${BASE}/api/v1/purchase-requests`, {
        title:          `E2E Flow — VU${__VU} iter${__ITER}`,
        description:    'Full-flow k6 test',
        requiredByDate: '2025-12-31',
        items: [
          { description: 'Server rack',    quantity: 2,  estimatedUnitPrice: 2500.00, unit: 'PCS' },
          { description: 'Network switch', quantity: 4,  estimatedUnitPrice:  800.00, unit: 'PCS' },
        ],
      }, oHdrs, 'pr_create'),
    );
    const ok = check(res, {
      'PR create 201': (r) => r.status === 201,
      'PR has id':     (r) => !!JSON.parse(r.body)?.data?.id,
    });
    if (ok) { prId = JSON.parse(res.body).data.id; }
    else     { hasError = true; }
  });
  if (hasError) { flowErrors.add(1); return; }
  sleep(0.3);

  // ── Step 3: Initiate Approval Workflow ────────────────────────────────────
  group('3_initiate_approval', () => {
    const res = step('approval_initiate', () =>
      post(`${BASE}/api/v1/approvals/purchase-requests/${prId}/initiate`,
        {}, oHdrs, 'approval_initiate'),
    );
    const ok = check(res, { 'Approval initiate 201/200': (r) => r.status === 201 || r.status === 200 });
    if (!ok) { hasError = true; }
  });
  if (hasError) { flowErrors.add(1); return; }
  sleep(0.3);

  // ── Step 4: Login as Approver ─────────────────────────────────────────────
  group('4_login_approver', () => {
    const tokens = step('login_approver', () =>
      login(APPROVER_EMAIL, APPROVER_PASSWORD),
    );
    approverToken = tokens.accessToken;
    if (!approverToken) { hasError = true; }
  });
  if (hasError) { flowErrors.add(1); return; }

  const aHdrs = authHeaders(approverToken);
  sleep(0.3);

  // ── Step 5: Fetch pending approvals and approve ───────────────────────────
  let approvalId;
  group('5_approve_pr', () => {
    const pendRes = step('approval_pending_list', () =>
      http.get(`${BASE}/api/v1/approvals/my/pending?page=0&size=5`,
        { ...aHdrs, tags: { name: 'approval_pending' } }),
    );
    check(pendRes, { 'Pending list 200': (r) => r.status === 200 });

    // find the approval step for our PR
    try {
      const items = JSON.parse(pendRes.body)?.data?.content ?? [];
      const mine  = items.find((a) => a.purchaseRequestId === prId);
      if (mine) { approvalId = mine.id; }
    } catch (_) { /* ok to skip */ }

    if (!approvalId) { return; } // approval may already be auto-resolved in test seed

    const decisionRes = step('approval_decide', () =>
      post(`${BASE}/api/v1/approvals/${approvalId}/decide`, {
        decision: 'APPROVED',
        comment:  'Approved via k6 load test',
      }, aHdrs, 'approval_decide'),
    );
    check(decisionRes, { 'Approve 200': (r) => r.status === 200 });
  });
  sleep(0.3);

  // ── Step 6: Create RFQ ────────────────────────────────────────────────────
  let rfqId;
  group('6_create_rfq', () => {
    const res = step('rfq_create', () =>
      post(`${BASE}/api/v1/rfqs`, {
        purchaseRequestId:  prId,
        title:              `RFQ for PR-${prId}`,
        description:        'k6 load test RFQ',
        submissionDeadline: '2025-11-30T23:59:59',
        currency:           'USD',
      }, oHdrs, 'rfq_create'),
    );
    const ok = check(res, {
      'RFQ create 201': (r) => r.status === 201,
      'RFQ has id':     (r) => !!JSON.parse(r.body)?.data?.id,
    });
    if (ok) { rfqId = JSON.parse(res.body).data.id; }
    else     { hasError = true; }
  });
  if (hasError) { flowErrors.add(1); return; }
  sleep(0.3);

  // ── Step 7: Submit Vendor Quotation ───────────────────────────────────────
  group('7_submit_quotation', () => {
    const res = step('quotation_submit', () =>
      post(`${BASE}/api/v1/rfqs/${rfqId}/quotations/${VENDOR_ID}`, {
        currency:       'USD',
        validUntil:     '2025-12-15',
        deliveryDays:   14,
        notes:          'k6 test quotation',
        lineItems: [
          { description: 'Server rack',    quantity: 2,  unitPrice: 2400.00, unit: 'PCS' },
          { description: 'Network switch', quantity: 4,  unitPrice:  780.00, unit: 'PCS' },
        ],
      }, oHdrs, 'quotation_submit'),
    );
    check(res, { 'Quotation submit 201': (r) => r.status === 201 });
  });
  sleep(0.3);

  // ── Step 8: Issue Purchase Order ──────────────────────────────────────────
  let poId;
  group('8_issue_po', () => {
    const res = step('po_create', () =>
      post(`${BASE}/api/v1/purchase-orders`, {
        purchaseRequestId: prId,
        vendorId:          parseInt(VENDOR_ID, 10),
        currency:          'USD',
        deliveryAddress:   '123 Procurement St, New York, NY 10001',
        requiredDeliveryDate: '2026-01-15',
        notes:             'k6 load test PO',
      }, oHdrs, 'po_create'),
    );
    const ok = check(res, {
      'PO create 201': (r) => r.status === 201,
      'PO has id':     (r) => !!JSON.parse(r.body)?.data?.id,
    });
    if (ok) { poId = JSON.parse(res.body).data.id; }
  });
  sleep(0.3);

  // ── Step 9: Submit Invoice ────────────────────────────────────────────────
  let invoiceId;
  group('9_submit_invoice', () => {
    if (!poId) { return; }
    const res = step('invoice_create', () =>
      post(`${BASE}/api/v1/invoices`, {
        purchaseOrderId: poId,
        vendorId:        parseInt(VENDOR_ID, 10),
        invoiceNumber:   `INV-K6-${__VU}-${__ITER}`,
        invoiceDate:     '2026-01-10',
        dueDate:         '2026-02-10',
        currency:        'USD',
        lineItems: [
          { description: 'Server rack',    quantity: 2, unitPrice: 2400.00 },
          { description: 'Network switch', quantity: 4, unitPrice:  780.00 },
        ],
      }, oHdrs, 'invoice_create'),
    );
    const ok = check(res, {
      'Invoice create 201': (r) => r.status === 201,
      'Invoice has id':     (r) => !!JSON.parse(r.body)?.data?.id,
    });
    if (ok) { invoiceId = JSON.parse(res.body).data.id; }
  });
  sleep(0.3);

  // ── Step 10: Verify Invoice ───────────────────────────────────────────────
  group('10_verify_invoice', () => {
    if (!invoiceId) { return; }
    const res = step('invoice_verify', () =>
      post(`${BASE}/api/v1/invoices/${invoiceId}/verify`, {}, oHdrs, 'invoice_verify'),
    );
    check(res, { 'Invoice verify 200': (r) => r.status === 200 });
  });

  // ── Record flow completion ────────────────────────────────────────────────
  flowDuration.add(Date.now() - flowStart);
  flowsCompleted.add(1);
  flowErrors.add(hasError ? 1 : 0);

  sleep(1);
}
