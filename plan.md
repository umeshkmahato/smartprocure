# SmartProcure — Phases 11–16 Implementation Plan

> **Status:** Approved — implementation in progress  
> **Objective:** Deliver a production-ready Java 21/Spring Boot system with event-driven
> notifications, containerised deployment, comprehensive observability, GraalVM native
> support, and demonstrable security and performance validation.

---

## Guiding principles

| Principle | Approach |
|---|---|
| Production readiness | Non-root containers, health checks, resource limits, secrets via env |
| Maintainability | Follow existing conventions; minimal, focused changes to existing files |
| Security | OWASP Top-10 checklist, JWT hardening, rate limiting, input validation |
| Measurable performance | JMH benchmarks (Phase 11), k6 load tests (Phase 16) |
| No unnecessary complexity | No Kubernetes (Docker Compose covers scope); no new frameworks |
| Backward compatibility | All changes additive; existing API contracts preserved |

---

## What already exists (not duplicated)

| Item | Location |
|---|---|
| `spring.threads.virtual.enabled=true` | `backend/src/main/resources/application.yml` |
| `VirtualThreadConfig` (Tomcat + @Async VT beans) | `backend/.../config/VirtualThreadConfig.java` |
| Kafka producer/consumer configuration | `application.yml` |
| Backend Dockerfile (multi-stage, non-root, health) | `backend/Dockerfile` |
| Frontend Dockerfile (multi-stage, Nginx) | `frontend/Dockerfile` |
| `docker-compose.yml` (infra + health checks) | project root |
| `docker-compose.app.yml` (backend + frontend) | project root |
| `docker-compose.observability.yml` (Prom + Grafana + OTel) | project root |
| Micrometer Prometheus registry | `backend/pom.xml` |
| OTel tracing + exporter dependencies | `backend/pom.xml` |
| Testcontainers (Kafka, PostgreSQL) | `backend/pom.xml` |

---

## Phase 11 — Virtual Threads: JMH Benchmark

**Goal:** Prove virtual-thread throughput improvement with a reproducible benchmark.

Virtual threads are already enabled in `application.yml` and `VirtualThreadConfig`.
This phase adds a standalone JMH Maven module.

### Files to create

| File | Purpose |
|---|---|
| `benchmarks/pom.xml` | Standalone Maven module; JMH 1.37 dependency |
| `benchmarks/src/main/java/.../VirtualVsPlatformThreadBenchmark.java` | Throughput + latency benchmark: virtual vs platform threads under simulated I/O load (50 ms sleep) |
| `benchmarks/README.md` | How to build and run benchmarks; sample output |

### How to run

```bash
cd benchmarks
mvn package -q
java -jar target/benchmarks.jar -rf json -rff results.json
```

### Expected output

JMH JSON report comparing:
- **Platform threads** (fixed pool of 200) — thread starvation under high concurrency
- **Virtual threads** (`newVirtualThreadPerTaskExecutor`) — scales to thousands of concurrent I/O tasks

---

## Phase 12 — Kafka Event-Driven Architecture

**Goal:** Decouple domain events from synchronous processing; drive async notifications.

### Domain events (backend publishes)

| Trigger | Event class | Kafka topic |
|---|---|---|
| `VendorService.approve()` | `VendorApprovedEvent` | `sp.vendor.approved` |
| `PurchaseOrderService.issue()` | `PurchaseOrderIssuedEvent` | `sp.po.issued` |
| `InvoiceService.verify()` | `InvoiceVerifiedEvent` | `sp.invoice.verified` |

Each event carries: `eventId` (UUID, for idempotency), `occurredAt`, domain-specific payload.

### Files to create — backend

| File | Purpose |
|---|---|
| `events/VendorApprovedEvent.java` | Record with eventId, vendorId, vendorName, approvedBy |
| `events/PurchaseOrderIssuedEvent.java` | Record with eventId, poId, poNumber, vendorId, totalAmount |
| `events/InvoiceVerifiedEvent.java` | Record with eventId, invoiceId, invoiceNumber, poId, totalAmount |
| `events/KafkaEventPublisher.java` | Typed Kafka producer; structured logging + Micrometer counter per event type |
| `config/KafkaTopicConfig.java` | Programmatic topic + DLQ topic declarations |
| `db/migration/V19__processed_events.sql` | Idempotency table for backend self-consumption |

### Files to modify — backend services (minimal)

| File | Change |
|---|---|
| `VendorService.java` | One `publisher.publishVendorApproved(vendor)` call in `approve()` |
| `PurchaseOrderService.java` | One `publisher.publishPoIssued(po)` call in `issue()` |
| `InvoiceService.java` | One `publisher.publishInvoiceVerified(invoice)` call in `verify()` |

### notification-service (new standalone Spring Boot module)

| File | Purpose |
|---|---|
| `notification-service/pom.xml` | Spring Boot 3.4, Kafka, Mail, Actuator, Testcontainers |
| `notification-service/src/main/resources/application.yml` | Kafka consumer, mail, actuator, OTel config |
| `NotificationServiceApplication.java` | Main class |
| `consumer/VendorEventConsumer.java` | `@RetryableTopic` consumer; 3 retries with exponential backoff (1→2→4 s) |
| `consumer/PurchaseOrderEventConsumer.java` | Same pattern for PO events |
| `consumer/InvoiceEventConsumer.java` | Same pattern for invoice events |
| `consumer/DlqConsumer.java` | Logs + persists all DLQ messages for ops visibility |
| `service/NotificationEmailService.java` | Composes and sends email via Spring Mail |
| `service/IdempotencyService.java` | Redis-backed `eventId` deduplication (TTL 7 days) |
| `model/ProcessedEvent.java` | JPA entity for persistent idempotency log |

### Reliability guarantees

```
Publish:   acks=all, retries=3 (already in application.yml)
Consume:   @RetryableTopic — 3 retries, exponential backoff
           → DLQ topic (sp.vendor.approved.DLT etc.) on exhaustion
Idempotency: Redis SET NX (eventId) checked before processing
```

---

## Phase 13 — Docker: Production Hardening

**Goal:** Add notification-service container; apply resource limits across all services.

### Files to create / modify

| File | Change |
|---|---|
| `notification-service/Dockerfile` | Multi-stage (Maven builder + JRE 21 Alpine), non-root, health check — identical pattern to `backend/Dockerfile` |
| `docker-compose.app.yml` | Add `notification-service` service; add `mem_limit` + `cpus` to backend, frontend, notification-service |
| `.env.example` | Add `NOTIFICATION_SERVICE_PORT`, `NOTIFICATION_KAFKA_GROUP`, `NOTIFICATION_REDIS_HOST` |

### Resource defaults

| Service | mem_limit | cpus |
|---|---|---|
| backend | 512m | 1.0 |
| frontend | 64m | 0.25 |
| notification-service | 256m | 0.5 |

---

## Phase 14 — Observability: Dashboards + Alerts + Structured Logging

**Goal:** Full-stack visibility — structured logs, custom business metrics, Grafana dashboard, actionable alert rules.

### Files to create / modify

| File | Purpose |
|---|---|
| `backend/src/main/resources/logback-spring.xml` | JSON structured output via logstash-logback-encoder (already in pom); `traceId`/`spanId` MDC injection |
| `backend/.../config/MetricsConfig.java` | Custom Micrometer counters: `vendor.approved.total`, `po.issued.total`, `invoice.verified.total`, `kafka.events.published.total`, `kafka.events.consumed.total` |
| `docker/prometheus/alerts.yml` | Alert rules: high HTTP error rate (>5%), slow p99 (>1 s), Kafka consumer lag >1000 |
| `docker/prometheus/prometheus.yml` | Add notification-service scrape target |
| `docker/grafana/provisioning/dashboards/smartprocure.json` | Full dashboard: JVM heap, GC pause, HTTP p50/p99, Kafka produce/consume rate, business KPI counters |
| `docker/otel/otel-collector.yml` | Add debug + file exporter for local trace inspection |

### Dashboard panels

1. HTTP request rate + error rate
2. HTTP latency p50 / p99 / p999
3. JVM heap used / GC pause duration
4. Kafka messages produced / consumed per topic
5. Kafka consumer lag per group
6. Business KPIs: vendors approved, POs issued, invoices verified (counters)
7. Virtual thread pool — active thread count

---

## Phase 15 — GraalVM Native (notification-service)

**Goal:** Compile the notification-service to a native executable; document limitations.

The notification-service (built in Phase 12) is extended with GraalVM native image support.

### Files to create

| File | Purpose |
|---|---|
| `notification-service/src/main/resources/META-INF/native-image/reflect-config.json` | Reflection hints for Jackson, Spring, Kafka classes |
| `notification-service/src/main/resources/META-INF/native-image/resource-config.json` | Resource bundle hints (application.yml, logback) |
| `notification-service/src/main/resources/META-INF/native-image/native-image.properties` | Additional native-image build args |
| `native` Maven profile in `notification-service/pom.xml` | `spring-boot:build-image` with Paketo native buildpack |
| `notification-service/Dockerfile.native` | GraalVM CE 21 builder → distroless runtime; produces sub-100 ms startup |
| `docs/NATIVE_IMAGE_LIMITATIONS.md` | Known limitations: dynamic Kafka deserialiser class loading, Logback janino, workarounds |

### How to build

```bash
cd notification-service
mvn -Pnative package -DskipTests
./target/notification-service   # native binary — starts in <100 ms
```

---

## Phase 16 — Final Validation

**Goal:** Security audit, load tests, integration tests, go/no-go assessment.

### Files to create

| File | Purpose |
|---|---|
| `k6/load-test.js` | k6 script: ramp 0→100 VUs over 2 min; test vendor list, PO create, invoice verify |
| `k6/thresholds.js` | Pass/fail gates: p99 < 500 ms, error rate < 1%, throughput > 500 req/s |
| `docs/SECURITY_AUDIT.md` | OWASP Top-10 checklist; JWT, rate limiting, CORS, input validation, SQL injection, secrets findings |
| `docs/PERFORMANCE_REPORT.md` | JMH benchmark results; k6 load test results; go/no-go verdict |
| `backend/src/test/.../KafkaEventPublishingIntegrationTest.java` | Testcontainers: publish event → verify on topic |
| `notification-service/src/test/.../NotificationConsumerIntegrationTest.java` | Testcontainers: consume event → verify email sent (MailHog) |

### Go/no-go gates

| Gate | Pass criterion |
|---|---|
| Unit tests | 100% pass, ≥70% line coverage |
| Integration tests | All pass with Testcontainers |
| k6 load test | p99 < 500 ms, error < 1% at 100 VUs |
| Security audit | No Critical/High unmitigated findings |
| Docker Compose | Full stack starts cleanly from cold: `docker compose up` |
| Native binary | Starts in < 100 ms, passes smoke test |

---

## Execution order

```
Phase 11 → Phase 12 → Phase 13 → Phase 14 → Phase 15 → Phase 16
```

Each phase is implemented and verified before the next begins.

---

## README phase tracker

| Phase | Description | Status |
|---|---|---|
| 11 — Virtual Threads | JMH benchmark module | ⏳ In Progress |
| 12 — Kafka Events | Event-driven notifications, retry, DLQ | ⏳ Pending |
| 13 — Docker | Production containers, resource limits | ⏳ Pending |
| 14 — Observability | Grafana dashboards, OTel, structured logging | ⏳ Pending |
| 15 — GraalVM Native | notification-service native executable | ⏳ Pending |
| 16 — Final Validation | Security audit, load tests, go/no-go | ⏳ Pending |
