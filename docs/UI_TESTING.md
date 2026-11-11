# UI Testing with Mock Data

This document explains how to run and test the SmartProcure frontend entirely
without a backend, using local in-memory mock data.

---

## Overview

When mock mode is enabled the application:

- Loads **realistic dummy data** for every screen from `frontend/src/mocks/`
- Simulates network latency (≈ 350 ms per request)
- Supports full **CRUD** — create, update, delete, and status transitions
- Supports **pagination, sorting, filtering, and keyword search**
- Accepts **any credentials** on the Login / Register screens
- Provides **canned AI chat responses** without Ollama

No backend, database, Redis, or Kafka installation is required.

---

## Enabling Mock Mode

### Option A — `.env` file (recommended)

The file `frontend/.env` ships with mock mode already enabled:

```env
VITE_USE_MOCK_DATA=true
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Option B — inline environment variable

```bash
VITE_USE_MOCK_DATA=true npm run dev
```

### Disabling mock mode (use real backend)

Edit `frontend/.env`:

```env
VITE_USE_MOCK_DATA=false
```

Then start the backend as described in the main README.

---

## Running the Application in Mock Mode

```bash
cd frontend
npm install          # first time only
npm run dev
```

Open **http://localhost:5173** in your browser.

- Log in with **any username and password** (e.g. `admin` / `admin`).
- All navigation, tables, detail views, forms, and actions work immediately.

---

## Mock Data Folder Structure

```
frontend/src/mocks/
├── utils.ts                   # delay(), paginate(), nextId(), date helpers
├── auth.mock.ts               # login / register — accepts any credentials
├── vendors.mock.ts            # 20 vendors (APPROVED / PENDING / SUSPENDED / BLACKLISTED)
├── purchaseRequests.mock.ts   # 15 PRs (DRAFT → SUBMITTED → APPROVED / REJECTED / CANCELLED)
├── approvals.mock.ts          # 12 approval steps (PENDING / APPROVED / REJECTED / DELEGATED)
├── quotations.mock.ts         # 8 RFQs + 12 quotations across all statuses
├── purchaseOrders.mock.ts     # 12 POs across all lifecycle statuses
├── invoices.mock.ts           # 10 invoices across all statuses
└── ai.mock.ts                 # keyword-matched chat responses + similar-vendor results
```

Each file exports a mock API object whose methods have **exactly the same
signature** as the corresponding real `api/*.ts` module.

---

## How API Switching Works

Every `api/*.ts` file contains a one-line switch:

```ts
// frontend/src/api/vendors.ts  (example)
const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const vendorsApi = USE_MOCK ? mockVendorsApi : {
  list: (params) => client.get('/vendors', { params }).then(r => r.data.data),
  ...
}
```

Vite evaluates `import.meta.env` at **build time**, so when mock mode is off
the mock modules are fully tree-shaken from the production bundle.

---

## What You Can Test

### Authentication
| Action | Mock behaviour |
|--------|----------------|
| Login  | Any username + password succeeds; returns `ROLE_ADMIN` |
| Register | Any valid form succeeds; returns `ROLE_USER` |
| Empty fields | Returns a validation error message |

### Vendors (20 records)
| Interaction | Supported |
|-------------|-----------|
| List with pagination (10/page) | ✅ |
| Filter by status | ✅ |
| Keyword search (name, email, city) | ✅ |
| Sort by any column | ✅ |
| View detail page | ✅ |
| Approve / Suspend / Blacklist | ✅ (updates in-memory) |
| Create new vendor | ✅ |

### Purchase Requests (15 records)
| Interaction | Supported |
|-------------|-----------|
| List, filter, search, sort, paginate | ✅ |
| View detail with line items | ✅ |
| Submit DRAFT → SUBMITTED | ✅ |
| Cancel DRAFT or SUBMITTED | ✅ |
| Create new PR with line items | ✅ |

### Approvals (12 records)
| Interaction | Supported |
|-------------|-----------|
| My pending approvals list | ✅ (5 pending) |
| All approvals with status filter | ✅ |
| Approve with comments | ✅ |
| Reject with comments | ✅ |

### RFQs & Quotations (8 RFQs, 12 quotations)
| Interaction | Supported |
|-------------|-----------|
| List RFQs, filter by status | ✅ |
| View RFQ detail + quotation list | ✅ |
| Publish / Close RFQ | ✅ |
| Submit a new quotation | ✅ |
| Compare quotations | ✅ |
| Select winning quotation | ✅ (marks others rejected, RFQ → AWARDED) |

### Purchase Orders (12 records)
| Interaction | Supported |
|-------------|-----------|
| List, filter, search, sort, paginate | ✅ |
| View detail with line items + delivery qty | ✅ |
| Issue / Acknowledge / Deliver / Close / Cancel | ✅ |
| Partial delivery (updates per-item deliveredQty) | ✅ |
| Create new PO | ✅ |

### Invoices (10 records)
| Interaction | Supported |
|-------------|-----------|
| List, filter, search, sort, paginate | ✅ |
| View detail with line items | ✅ |
| Start Review / Verify / Dispute / Mark Paid / Cancel | ✅ |
| Create new invoice | ✅ |

### Dashboard
| Widget | Supported |
|--------|-----------|
| KPI counts (vendors, PRs, POs, invoices, approvals) | ✅ |
| Recent POs table | ✅ |
| Recent Invoices table | ✅ |
| Pending Approvals list | ✅ |
| Activity Trend chart (static demo data) | ✅ |
| Status Distribution chart (static demo data) | ✅ |

### AI Chat
| Feature | Mock behaviour |
|---------|----------------|
| General question | Returns onboarding message |
| Mentions "vendor" or "recommend" | Returns top-3 vendor recommendations |
| Mentions "spend" or "budget" | Returns YTD spend summary |
| Mentions "approv" or "pending" | Lists the 5 pending approval requests |
| Mentions "order" or "po" | Returns PO status summary |
| Vendor similarity search | Returns 5 pre-ranked vendors with scores |

---

## Modifying Mock Data

### Add a new vendor

Open `frontend/src/mocks/vendors.mock.ts` and append to the `vendors` array:

```ts
{
  id: 21, name: 'My New Vendor', email: 'contact@mynewvendor.com',
  phone: '+1-555-9999', address: '1 New Street', city: 'New York',
  country: 'USA', taxId: 'US-TAX-021', registrationNumber: 'REG-MN-021',
  category: 'Technology', currency: 'USD', status: 'PENDING',
  rating: undefined, createdAt: daysAgo(1), updatedAt: daysAgo(1),
},
```

### Change pagination default page size

In `mocks/utils.ts` change the default `size` parameter in `paginate()`:

```ts
const { page = 0, size = 20, ... } = opts   // was 10
```

### Change simulated delay

In `mocks/utils.ts`:

```ts
export const delay = (ms = 150) => ...      // was 350
```

---

## Switching Back to the Real Backend

1. Edit `frontend/.env`:
   ```env
   VITE_USE_MOCK_DATA=false
   ```
2. Start all backend services (PostgreSQL, Redis, Kafka, Spring Boot).
3. Restart the Vite dev server (`npm run dev`).

No component or API code needs to change — the switch is transparent.

---

## Limitations

| Area | Note |
|------|------|
| Mock state is in-memory | All changes reset on browser refresh |
| AI responses are canned | Real Ollama LLM not invoked |
| Dashboard charts | Activity trend and status distribution use static demo data |
| File uploads | `analyzeDocument` AI endpoint is not mocked |
| Auth tokens | Mock JWT tokens are not cryptographically valid |
| Pagination total counts | Reflect filtered in-memory arrays, not a real DB |
