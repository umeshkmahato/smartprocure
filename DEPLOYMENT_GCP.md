# SmartProcure — GCP Production Deployment Guide

> **Target:** Google Kubernetes Engine (GKE Autopilot) + managed GCP services  
> **Region:** `us-central1` (adjust as needed)  
> **Prerequisites:** Google Cloud CLI (`gcloud`) v470+, `kubectl`, `helm` v3, Docker, Maven 3.9+

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [GCP Services Used](#2-gcp-services-used)
3. [Prerequisites & Project Setup](#3-prerequisites--project-setup)
4. [Push Images to Artifact Registry](#4-push-images-to-artifact-registry)
5. [Cloud SQL — PostgreSQL 16](#5-cloud-sql--postgresql-16)
6. [Memorystore — Redis](#6-memorystore--redis)
7. [Kafka on GKE (Strimzi)](#7-kafka-on-gke-strimzi)
8. [Secret Manager](#8-secret-manager)
9. [GKE Autopilot Cluster](#9-gke-autopilot-cluster)
10. [Kubernetes Manifests](#10-kubernetes-manifests)
11. [Ingress & TLS — Google-Managed Certificate](#11-ingress--tls--google-managed-certificate)
12. [Observability — Cloud Monitoring + Managed Prometheus](#12-observability--cloud-monitoring--managed-prometheus)
13. [DNS — Cloud DNS](#13-dns--cloud-dns)
14. [Environment Variables Reference](#14-environment-variables-reference)
15. [CI/CD — Cloud Build + Cloud Deploy](#15-cicd--cloud-build--cloud-deploy)
16. [Scaling & Cost Optimisation](#16-scaling--cost-optimisation)
17. [Runbook — Common Operations](#17-runbook--common-operations)

---

## 1. Architecture Overview

```
                     ┌──────────────────────────────────────────────────────┐
  Internet           │                   GCP Project                        │
     │               │                                                       │
     ▼               │  ┌──────────────┐    ┌──────────────────────────┐    │
 ┌────────┐  HTTPS   │  │  Cloud DNS   │    │   VPC (private)          │    │
 │  User  │─────────▶│  │              │    │                           │    │
 └────────┘          │  │  External IP │    │  ┌────────────────────┐  │    │
                     │  │  + GLB       │───▶│  │  GKE Autopilot     │  │    │
                     │  └──────────────┘    │  │                    │  │    │
                     │                      │  │  ┌──────────────┐  │  │    │
                     │                      │  │  │   backend    │  │  │    │
                     │                      │  │  │  (Deployment)│  │  │    │
                     │                      │  │  └──────┬───────┘  │  │    │
                     │                      │  │         │          │  │    │
                     │                      │  │  ┌──────▼───────┐  │  │    │
                     │                      │  │  │notification  │  │  │    │
                     │                      │  │  │  -service    │  │  │    │
                     │                      │  │  │  (native)    │  │  │    │
                     │                      │  │  └──────────────┘  │  │    │
                     │                      │  └────────┬───────────┘  │    │
                     │                      │           │               │    │
                     │                      │  ┌────────▼───────────┐  │    │
                     │                      │  │    Managed Data     │  │    │
                     │                      │  │  Cloud SQL PG 16    │  │    │
                     │                      │  │  Memorystore Redis  │  │    │
                     │                      │  │  Strimzi Kafka/GKE  │  │    │
                     │                      │  └─────────────────────┘  │    │
                     │                      └──────────────────────────┘    │
                     └──────────────────────────────────────────────────────┘
```

---

## 2. GCP Services Used

| GCP Service | Purpose | Tier / Size |
|-------------|---------|-------------|
| **Artifact Registry** | Container image registry | Standard |
| **GKE Autopilot** | Serverless Kubernetes runtime | Autopilot (auto-provisioned nodes) |
| **Cloud SQL (PostgreSQL 16)** | Primary database + pgvector | `db-g1-small` (prod: `db-n2-standard-2`) |
| **Memorystore (Redis 7)** | JWT refresh tokens, idempotency | 1 GB Basic / 5 GB Standard HA |
| **Strimzi on GKE** | Apache Kafka (domain events) | 3-broker cluster, 100 GB SSD each |
| **Cloud Load Balancing** | HTTPS Global Load Balancer | — |
| **Google-Managed Certificates** | TLS (auto-renewing) | — |
| **Cloud DNS** | DNS zone | — |
| **Secret Manager** | Credentials & API keys | — |
| **Cloud Monitoring** | Metrics, alerts, dashboards | — |
| **Managed Service for Prometheus** | Prometheus-compatible metrics | — |
| **Cloud Build** | CI pipeline | — |
| **Cloud Deploy** | CD pipeline with approval gates | — |
| **Cloud Storage** | Terraform state, invoice attachments | Standard |

---

## 3. Prerequisites & Project Setup

### 3.1 Install tools

```bash
# Google Cloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# kubectl (bundled with gcloud)
gcloud components install kubectl

# Helm 3
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### 3.2 Set project & defaults

```bash
PROJECT_ID="smartprocure-prod"
REGION="us-central1"
CLUSTER="sp-gke-prod"

gcloud config set project "$PROJECT_ID"
gcloud config set compute/region "$REGION"

# Enable required APIs
gcloud services enable \
  container.googleapis.com \
  sqladmin.googleapis.com \
  redis.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  clouddeploy.googleapis.com \
  monitoring.googleapis.com \
  dns.googleapis.com \
  servicenetworking.googleapis.com
```

### 3.3 Create a Workload Identity service account (least-privilege)

```bash
# Service account for GKE workloads
gcloud iam service-accounts create sp-workload-sa \
  --display-name "SmartProcure Workload SA"

# Grant Secret Manager access
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member "serviceAccount:sp-workload-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role "roles/secretmanager.secretAccessor"

# Grant Cloud SQL access
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member "serviceAccount:sp-workload-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role "roles/cloudsql.client"

# Grant Cloud Monitoring write (for custom metrics)
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member "serviceAccount:sp-workload-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role "roles/monitoring.metricWriter"
```

---

## 4. Push Images to Artifact Registry

```bash
PROJECT_ID="smartprocure-prod"
REGION="us-central1"
AR_REPO="${REGION}-docker.pkg.dev/${PROJECT_ID}/smartprocure"

# Create Artifact Registry repository
gcloud artifacts repositories create smartprocure \
  --repository-format docker \
  --location "$REGION" \
  --description "SmartProcure container images"

# Configure Docker auth
gcloud auth configure-docker "${REGION}-docker.pkg.dev"

# Build & push backend (JVM)
cd backend
docker build -t "${AR_REPO}/smartprocure-backend:latest" .
docker push "${AR_REPO}/smartprocure-backend:latest"

# Build & push notification-service (GraalVM native → distroless)
cd ../notification-service
docker build -f Dockerfile.native \
             -t "${AR_REPO}/notification-service:native" .
docker push "${AR_REPO}/notification-service:native"

# Build & push frontend
cd ../frontend
docker build -t "${AR_REPO}/smartprocure-frontend:latest" .
docker push "${AR_REPO}/smartprocure-frontend:latest"
```

> **Tag with git SHA for rollback:**
> ```bash
> SHA=$(git rev-parse --short HEAD)
> docker tag "${AR_REPO}/smartprocure-backend:latest" \
>            "${AR_REPO}/smartprocure-backend:${SHA}"
> docker push "${AR_REPO}/smartprocure-backend:${SHA}"
> ```

---

## 5. Cloud SQL — PostgreSQL 16

### 5.1 Create instance

```bash
gcloud sql instances create sp-postgres-prod \
  --database-version POSTGRES_16 \
  --tier db-g1-small \
  --region "$REGION" \
  --no-assign-ip \
  --network projects/${PROJECT_ID}/global/networks/default \
  --enable-google-private-path \
  --storage-type SSD \
  --storage-size 100GB \
  --storage-auto-increase \
  --backup-start-time 02:00 \
  --retained-backups-count 7 \
  --deletion-protection \
  --database-flags max_connections=200,shared_buffers=256MB
```

### 5.2 Create database and user

```bash
gcloud sql databases create smartprocure \
  --instance sp-postgres-prod

gcloud sql users create sp_app \
  --instance sp-postgres-prod \
  --password "$(openssl rand -base64 32)"
```

### 5.3 Enable pgvector extension

```bash
# Connect via Cloud SQL Auth Proxy (see §17)
psql "host=127.0.0.1 port=5432 dbname=smartprocure user=sp_app" <<SQL
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
SQL
```

### 5.4 Cloud SQL Auth Proxy as GKE sidecar

The backend Pod uses the **Cloud SQL Auth Proxy** sidecar pattern:

```yaml
# Added to backend Deployment (see §10 for full manifest)
- name: cloud-sql-proxy
  image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.11
  args:
    - "--structured-logs"
    - "--port=5432"
    - "${PROJECT_ID}:${REGION}:sp-postgres-prod"
  securityContext:
    runAsNonRoot: true
  resources:
    requests: { cpu: 100m, memory: 64Mi }
    limits:   { cpu: 500m, memory: 128Mi }
```

---

## 6. Memorystore — Redis

```bash
gcloud redis instances create sp-redis-prod \
  --size 1 \
  --region "$REGION" \
  --redis-version redis_7_0 \
  --tier basic \
  --network default \
  --enable-auth \
  --transit-encryption-mode SERVER_AUTHENTICATION

# Retrieve connection details
gcloud redis instances describe sp-redis-prod \
  --region "$REGION" \
  --format "value(host,port,authString)"
```

---

## 7. Kafka on GKE (Strimzi)

```bash
# Install Strimzi operator via Helm
helm repo add strimzi https://strimzi.io/charts/
helm repo update

helm install strimzi-kafka-operator strimzi/strimzi-kafka-operator \
  --namespace kafka \
  --create-namespace \
  --version 0.41.0
```

Apply the Kafka cluster CR:

```yaml
# k8s/kafka/kafka-cluster.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: sp-kafka
  namespace: kafka
spec:
  kafka:
    version: 3.7.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
        authentication:
          type: scram-sha-512
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: jbod
      volumes:
        - id: 0
          type: persistent-claim
          size: 100Gi
          class: premium-rwo
          deleteClaim: false
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 10Gi
      class: standard-rwo
      deleteClaim: false
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

```bash
kubectl apply -f k8s/kafka/kafka-cluster.yaml

# Create SmartProcure topics
kubectl apply -f - <<EOF
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: sp.vendor.events
  namespace: kafka
  labels:
    strimzi.io/cluster: sp-kafka
spec:
  partitions: 3
  replicas: 3
  config:
    retention.ms: 604800000   # 7 days
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: sp.purchase-order.events
  namespace: kafka
  labels:
    strimzi.io/cluster: sp-kafka
spec:
  partitions: 3
  replicas: 3
---
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: sp.invoice.events
  namespace: kafka
  labels:
    strimzi.io/cluster: sp-kafka
spec:
  partitions: 3
  replicas: 3
EOF
```

---

## 8. Secret Manager

```bash
PROJECT_ID="smartprocure-prod"

# Helper function
create_secret() {
  gcloud secrets create "$1" \
    --replication-policy automatic \
    --project "$PROJECT_ID"
  echo -n "$2" | gcloud secrets versions add "$1" --data-file=-
}

# Database
create_secret "sp-db-url"      "jdbc:postgresql://127.0.0.1:5432/smartprocure"
create_secret "sp-db-username" "sp_app"
create_secret "sp-db-password" "$(gcloud sql users describe sp_app --instance=sp-postgres-prod --format='value(password)')"

# Redis
REDIS_HOST=$(gcloud redis instances describe sp-redis-prod --region us-central1 --format='value(host)')
REDIS_AUTH=$(gcloud redis instances describe sp-redis-prod --region us-central1 --format='value(authString)')
create_secret "sp-redis-host"     "$REDIS_HOST"
create_secret "sp-redis-password" "$REDIS_AUTH"

# Kafka (Strimzi bootstrap)
create_secret "sp-kafka-bootstrap" "sp-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092"

# JWT
create_secret "sp-jwt-secret" "$(openssl rand -base64 48)"

# SMTP (SendGrid example)
create_secret "sp-smtp-host"     "smtp.sendgrid.net"
create_secret "sp-smtp-username" "apikey"
create_secret "sp-smtp-password" "<sendgrid-api-key>"
```

---

## 9. GKE Autopilot Cluster

```bash
gcloud container clusters create-auto "$CLUSTER" \
  --region "$REGION" \
  --release-channel regular \
  --workload-pool "${PROJECT_ID}.svc.id.goog" \
  --enable-master-authorized-networks \
  --master-authorized-networks "$(curl -s ifconfig.me)/32" \
  --enable-private-nodes \
  --master-ipv4-cidr 172.16.0.0/28 \
  --network default \
  --subnetwork default

# Get credentials
gcloud container clusters get-credentials "$CLUSTER" --region "$REGION"

# Create namespace
kubectl create namespace smartprocure

# Bind GKE service account to GCP service account (Workload Identity)
kubectl create serviceaccount sp-ksa \
  --namespace smartprocure

gcloud iam service-accounts add-iam-policy-binding \
  "sp-workload-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:${PROJECT_ID}.svc.id.goog[smartprocure/sp-ksa]"

kubectl annotate serviceaccount sp-ksa \
  --namespace smartprocure \
  "iam.gke.io/gcp-service-account=sp-workload-sa@${PROJECT_ID}.iam.gserviceaccount.com"
```

---

## 10. Kubernetes Manifests

### 10.1 Backend Deployment

```yaml
# k8s/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: smartprocure
  labels:
    app: backend
    version: "1.0.0"
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: backend
      annotations:
        # Managed Prometheus scraping
        prometheus.io/scrape: "true"
        prometheus.io/port:   "8080"
        prometheus.io/path:   "/actuator/prometheus"
    spec:
      serviceAccountName: sp-ksa
      containers:
        - name: backend
          image: us-central1-docker.pkg.dev/smartprocure-prod/smartprocure/smartprocure-backend:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: db-url
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: db-username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: db-password
            - name: REDIS_HOST
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: redis-host
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: redis-password
            - name: KAFKA_BOOTSTRAP_SERVERS
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: kafka-bootstrap
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: jwt-secret
            - name: MAIL_HOST
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: smtp-host
            - name: MAIL_USERNAME
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: smtp-username
            - name: MAIL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: smtp-password
          resources:
            requests:
              cpu: 500m
              memory: 1Gi
            limits:
              cpu: "2"
              memory: 2Gi
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["/bin/sh", "-c", "sleep 10"]
        # Cloud SQL Auth Proxy sidecar
        - name: cloud-sql-proxy
          image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.11
          args:
            - "--structured-logs"
            - "--port=5432"
            - "smartprocure-prod:us-central1:sp-postgres-prod"
          securityContext:
            runAsNonRoot: true
          resources:
            requests: { cpu: 100m, memory: 64Mi }
            limits:   { cpu: 500m, memory: 128Mi }
      terminationGracePeriodSeconds: 60
```

### 10.2 Backend Service

```yaml
# k8s/backend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: smartprocure
spec:
  selector:
    app: backend
  ports:
    - name: http
      port: 80
      targetPort: 8080
  type: ClusterIP
```

### 10.3 Notification-Service Deployment (native image)

```yaml
# k8s/notification/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
  namespace: smartprocure
  labels:
    app: notification-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: notification-service
  template:
    metadata:
      labels:
        app: notification-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port:   "8081"
        prometheus.io/path:   "/actuator/prometheus"
    spec:
      serviceAccountName: sp-ksa
      containers:
        - name: notification-service
          image: us-central1-docker.pkg.dev/smartprocure-prod/smartprocure/notification-service:native
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            - name: KAFKA_BOOTSTRAP_SERVERS
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: kafka-bootstrap
            - name: REDIS_HOST
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: redis-host
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: redis-password
            - name: MAIL_HOST
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: smtp-host
            - name: MAIL_USERNAME
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: smtp-username
            - name: MAIL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: sp-secrets
                  key: smtp-password
          resources:
            requests:
              cpu: 100m
              memory: 128Mi   # native image — very low memory
            limits:
              cpu: 500m
              memory: 256Mi
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 5    # fast native start-up
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 3
            periodSeconds: 10
```

### 10.4 HorizontalPodAutoscaler

```yaml
# k8s/backend/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: smartprocure
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 75
```

### 10.5 PodDisruptionBudget

```yaml
# k8s/backend/pdb.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: backend-pdb
  namespace: smartprocure
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: backend
```

### 10.6 Sync secrets from Secret Manager via External Secrets Operator

```bash
# Install External Secrets Operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  --namespace external-secrets --create-namespace
```

```yaml
# k8s/secrets/secret-store.yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: gcp-secret-store
  namespace: smartprocure
spec:
  provider:
    gcpsm:
      projectID: smartprocure-prod
---
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: sp-secrets
  namespace: smartprocure
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: gcp-secret-store
    kind: SecretStore
  target:
    name: sp-secrets
    creationPolicy: Owner
  data:
    - secretKey: db-url
      remoteRef: { key: sp-db-url }
    - secretKey: db-username
      remoteRef: { key: sp-db-username }
    - secretKey: db-password
      remoteRef: { key: sp-db-password }
    - secretKey: redis-host
      remoteRef: { key: sp-redis-host }
    - secretKey: redis-password
      remoteRef: { key: sp-redis-password }
    - secretKey: kafka-bootstrap
      remoteRef: { key: sp-kafka-bootstrap }
    - secretKey: jwt-secret
      remoteRef: { key: sp-jwt-secret }
    - secretKey: smtp-host
      remoteRef: { key: sp-smtp-host }
    - secretKey: smtp-username
      remoteRef: { key: sp-smtp-username }
    - secretKey: smtp-password
      remoteRef: { key: sp-smtp-password }
```

```bash
# Apply all manifests
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/backend/
kubectl apply -f k8s/notification/
```

---

## 11. Ingress & TLS — Google-Managed Certificate

```yaml
# k8s/ingress/managed-cert.yaml
apiVersion: networking.gke.io/v1
kind: ManagedCertificate
metadata:
  name: sp-tls-cert
  namespace: smartprocure
spec:
  domains:
    - api.smartprocure.io
    - app.smartprocure.io
---
# k8s/ingress/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: sp-ingress
  namespace: smartprocure
  annotations:
    kubernetes.io/ingress.global-static-ip-name: sp-global-ip
    networking.gke.io/managed-certificates:      sp-tls-cert
    kubernetes.io/ingress.class:                 gce
    networking.gke.io/v1beta1.FrontendConfig:    sp-frontend-config
spec:
  rules:
    - host: api.smartprocure.io
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: backend
                port:
                  number: 80
    - host: app.smartprocure.io
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend
                port:
                  number: 80
---
# k8s/ingress/frontend-config.yaml  (HTTP → HTTPS redirect)
apiVersion: networking.gke.io/v1beta1
kind: FrontendConfig
metadata:
  name: sp-frontend-config
  namespace: smartprocure
spec:
  redirectToHttps:
    enabled: true
    responseCodeName: MOVED_PERMANENTLY_DEFAULT
```

```bash
# Reserve a global static IP
gcloud compute addresses create sp-global-ip \
  --global --ip-version IPV4

STATIC_IP=$(gcloud compute addresses describe sp-global-ip \
  --global --format='value(address)')
echo "Static IP: $STATIC_IP"   # Use this in Cloud DNS

kubectl apply -f k8s/ingress/
```

---

## 12. Observability — Cloud Monitoring + Managed Prometheus

### 12.1 Enable Managed Service for Prometheus

```bash
gcloud container clusters update "$CLUSTER" \
  --region "$REGION" \
  --enable-managed-prometheus
```

### 12.2 PodMonitoring resource (scrapes `/actuator/prometheus`)

```yaml
# k8s/monitoring/pod-monitoring.yaml
apiVersion: monitoring.googleapis.com/v1
kind: PodMonitoring
metadata:
  name: sp-backend-metrics
  namespace: smartprocure
spec:
  selector:
    matchLabels:
      app: backend
  endpoints:
    - port: 8080
      path: /actuator/prometheus
      interval: 30s
---
apiVersion: monitoring.googleapis.com/v1
kind: PodMonitoring
metadata:
  name: sp-notification-metrics
  namespace: smartprocure
spec:
  selector:
    matchLabels:
      app: notification-service
  endpoints:
    - port: 8081
      path: /actuator/prometheus
      interval: 30s
```

```bash
kubectl apply -f k8s/monitoring/pod-monitoring.yaml
```

### 12.3 Cloud Monitoring alert policies

```bash
# HTTP error rate alert
gcloud alpha monitoring policies create \
  --policy-from-file=- <<'EOF'
{
  "displayName": "SmartProcure — High HTTP Error Rate",
  "conditions": [{
    "displayName": "5xx rate > 5%",
    "conditionThreshold": {
      "filter": "metric.type=\"prometheus.googleapis.com/http_server_requests_seconds_count/counter\" resource.type=\"prometheus_target\"",
      "aggregations": [{
        "alignmentPeriod": "60s",
        "perSeriesAligner": "ALIGN_RATE"
      }],
      "comparison": "COMPARISON_GT",
      "thresholdValue": 0.05,
      "duration": "120s"
    }
  }],
  "alertStrategy": { "autoClose": "604800s" },
  "notificationChannels": ["projects/smartprocure-prod/notificationChannels/CHANNEL_ID"]
}
EOF
```

### 12.4 Deploy Grafana (optional self-hosted on GKE)

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm install grafana grafana/grafana \
  --namespace monitoring --create-namespace \
  --set adminPassword="$(openssl rand -base64 16)" \
  --set persistence.enabled=true \
  --set persistence.storageClassName=standard-rwo \
  --set persistence.size=5Gi
```

Grafana can query the Managed Prometheus endpoint using the
[Google Cloud Monitoring data source](https://grafana.com/docs/grafana/latest/datasources/google-cloud-monitoring/).
Pre-built dashboards are provisioned automatically from `docker/grafana/dashboards/`.

---

## 13. DNS — Cloud DNS

```bash
# Create managed zone
gcloud dns managed-zones create smartprocure-zone \
  --dns-name "smartprocure.io." \
  --description "SmartProcure production DNS"

# A record pointing to the global static IP
gcloud dns record-sets create api.smartprocure.io. \
  --zone smartprocure-zone \
  --type A \
  --ttl 300 \
  --rrdatas "$STATIC_IP"

gcloud dns record-sets create app.smartprocure.io. \
  --zone smartprocure-zone \
  --type A \
  --ttl 300 \
  --rrdatas "$STATIC_IP"

# View NS records (update at your domain registrar)
gcloud dns managed-zones describe smartprocure-zone \
  --format "value(nameServers)"
```

---

## 14. Environment Variables Reference

| Variable | Source | Example value |
|----------|--------|--------------|
| `SPRING_PROFILES_ACTIVE` | K8s env | `prod` |
| `DB_URL` | External Secret | `jdbc:postgresql://127.0.0.1:5432/smartprocure` |
| `DB_USERNAME` | External Secret | `sp_app` |
| `DB_PASSWORD` | External Secret | `<cloud-sql-pass>` |
| `REDIS_HOST` | External Secret | `10.x.x.x` (Memorystore private IP) |
| `REDIS_PORT` | K8s env | `6379` |
| `REDIS_PASSWORD` | External Secret | `<auth-string>` |
| `KAFKA_BOOTSTRAP_SERVERS` | External Secret | `sp-kafka-kafka-bootstrap.kafka.svc:9092` |
| `KAFKA_CONSUMER_GROUP` | K8s env | `sp-main-group` |
| `JWT_SECRET` | External Secret | `<256-bit-random>` |
| `MAIL_HOST` | External Secret | `smtp.sendgrid.net` |
| `MAIL_PORT` | K8s env | `587` |
| `MAIL_AUTH` | K8s env | `true` |
| `MAIL_TLS` | K8s env | `true` |
| `OLLAMA_BASE_URL` | K8s env | `http://ollama:11434` |

---

## 15. CI/CD — Cloud Build + Cloud Deploy

### 15.1 `cloudbuild.yaml`

```yaml
# cloudbuild.yaml
steps:
  # 1 — Unit tests only (Testcontainers integration tests need Docker-in-Docker;
  #     run mvn verify in a separate nightly Cloud Build trigger with privileged mode)
  - name: maven:3.9-eclipse-temurin-21
    id: test
    entrypoint: mvn
    args: ["-B", "-ntp", "clean", "test", "-DskipITs"]
    dir: backend

  # 2 — Build & push backend image
  - name: gcr.io/cloud-builders/docker
    id: build-backend
    args:
      - build
      - -t
      - us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/smartprocure-backend:$SHORT_SHA
      - -t
      - us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/smartprocure-backend:latest
      - backend
    waitFor: [test]

  - name: gcr.io/cloud-builders/docker
    id: push-backend
    args:
      - push
      - --all-tags
      - us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/smartprocure-backend
    waitFor: [build-backend]

  # 3 — Build & push notification-service (native image — longer build)
  - name: gcr.io/cloud-builders/docker
    id: build-notification
    args:
      - build
      - -f
      - notification-service/Dockerfile.native
      - -t
      - us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/notification-service:$SHORT_SHA
      - -t
      - us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/notification-service:native
      - notification-service
    timeout: 900s   # native build ~5 min
    waitFor: [test]

  - name: gcr.io/cloud-builders/docker
    id: push-notification
    args:
      - push
      - --all-tags
      - us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/notification-service
    waitFor: [build-notification]

  # 4 — Create Cloud Deploy release
  - name: gcr.io/google.com/cloudsdktool/cloud-sdk
    id: deploy
    entrypoint: gcloud
    args:
      - deploy
      - releases
      - create
      - sp-release-$SHORT_SHA
      - --delivery-pipeline=smartprocure-pipeline
      - --region=us-central1
      - --images=backend=us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/smartprocure-backend:$SHORT_SHA,notification=us-central1-docker.pkg.dev/$PROJECT_ID/smartprocure/notification-service:$SHORT_SHA
    waitFor: [push-backend, push-notification]

options:
  machineType: E2_HIGHCPU_32
  logging: CLOUD_LOGGING_ONLY

timeout: 1800s
```

### 15.2 Cloud Deploy pipeline

```yaml
# cloud-deploy/pipeline.yaml
apiVersion: deploy.cloud.google.com/v1
kind: DeliveryPipeline
metadata:
  name: smartprocure-pipeline
  location: us-central1
description: SmartProcure production delivery pipeline
serialPipeline:
  stages:
    - targetId: staging
      profiles: [staging]
    - targetId: production
      profiles: [production]
      strategy:
        canary:
          runtimeConfig:
            kubernetes:
              gatewayServiceMesh:
                httpRoute: sp-route
                service: backend
                deployment: backend
          canaryDeployment:
            percentages: [25, 50, 75]
            verify: true
---
apiVersion: deploy.cloud.google.com/v1
kind: Target
metadata:
  name: production
  location: us-central1
requireApproval: true
gke:
  cluster: projects/smartprocure-prod/locations/us-central1/clusters/sp-gke-prod
```

---

## 16. Scaling & Cost Optimisation

| Recommendation | Detail |
|----------------|--------|
| **GKE Autopilot** | Node provisioning is automatic and billed per Pod resource request — no idle node cost |
| **Spot Pods (Autopilot)** | Mark notification-service Pods with `cloud.google.com/gke-spot: "true"` — saves ~60–91 % compute cost |
| **Cloud SQL — HA + read replica** | Add a read replica for analytics queries; failover is automatic |
| **Committed Use Discounts** | 1-year CUD on Cloud SQL `db-n2-standard-2` saves ~37 % |
| **Artifact Registry cleanup** | Set lifecycle policy to retain only the last 10 tagged images |
| **GCS Autoclass** | For invoice attachment bucket — objects automatically tiered after 30 days |
| **Cloud CDN** | Enable on the Global Load Balancer for the React frontend — reduces latency and egress cost |

---

## 17. Runbook — Common Operations

### Deploy a new version

```bash
# A Cloud Build trigger fires automatically on push to main.
# To trigger manually:
gcloud builds submit \
  --config cloudbuild.yaml \
  --region us-central1 .
```

### Roll back to a previous release

```bash
gcloud deploy rollbacks create \
  --delivery-pipeline smartprocure-pipeline \
  --region us-central1
```

### Open a shell in a running Pod

```bash
# Get a backend pod name
kubectl -n smartprocure get pods -l app=backend

# Exec in
kubectl -n smartprocure exec -it backend-<pod-id> \
  --container backend -- /bin/sh
```

### View live logs

```bash
# All backend pods, follow
kubectl -n smartprocure logs -l app=backend -f --max-log-requests 5

# Or via Cloud Logging
gcloud logging read \
  'resource.type="k8s_container" labels."k8s-pod/app"="backend"' \
  --project smartprocure-prod \
  --limit 100 \
  --format json | python3 -m json.tool
```

### Connect to Cloud SQL locally via Auth Proxy

```bash
# Download the proxy
curl -o cloud-sql-proxy \
  https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.11.0/cloud-sql-proxy.linux.amd64
chmod +x cloud-sql-proxy

# Start proxy (port 5433 to avoid local postgres conflict)
./cloud-sql-proxy \
  "smartprocure-prod:us-central1:sp-postgres-prod" \
  --port 5433 &

# Connect
psql "host=127.0.0.1 port=5433 dbname=smartprocure user=sp_app"
```

### Scale notification-service manually

```bash
kubectl -n smartprocure scale deployment notification-service \
  --replicas=4
```

### Force Flyway repair

```bash
# Connect via proxy (above), then:
psql "$DB_URL" -c "DELETE FROM flyway_schema_history WHERE success = false;"
# Rolling restart triggers Flyway repair
kubectl -n smartprocure rollout restart deployment/backend
```

### Check HPA status

```bash
kubectl -n smartprocure get hpa backend-hpa
kubectl -n smartprocure describe hpa backend-hpa
```
