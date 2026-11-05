# SmartProcure — AWS Production Deployment Guide

> **Target:** AWS ECS Fargate (application) + managed AWS services (data, messaging, observability)  
> **Region:** `us-east-1` (adjust as needed)  
> **Prerequisites:** AWS CLI v2, Docker, Maven 3.9+, Terraform ≥ 1.7 (optional but recommended)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [AWS Services Used](#2-aws-services-used)
3. [Prerequisites & IAM Setup](#3-prerequisites--iam-setup)
4. [Push Images to ECR](#4-push-images-to-ecr)
5. [RDS — PostgreSQL 16](#5-rds--postgresql-16)
6. [ElastiCache — Redis](#6-elasticache--redis)
7. [MSK — Apache Kafka](#7-msk--apache-kafka)
8. [Secrets Manager](#8-secrets-manager)
9. [ECS Cluster & Task Definitions](#9-ecs-cluster--task-definitions)
10. [Application Load Balancer](#10-application-load-balancer)
11. [ECS Services](#11-ecs-services)
12. [Observability — CloudWatch + Managed Grafana](#12-observability--cloudwatch--managed-grafana)
13. [DNS & TLS — Route 53 + ACM](#13-dns--tls--route-53--acm)
14. [Environment Variables Reference](#14-environment-variables-reference)
15. [CI/CD — GitHub Actions](#15-cicd--github-actions)
16. [Scaling & Cost Optimisation](#16-scaling--cost-optimisation)
17. [Runbook — Common Operations](#17-runbook--common-operations)

---

## 1. Architecture Overview

```
                        ┌─────────────────────────────────────────────┐
                        │                   VPC                        │
  Internet              │                                              │
     │                  │  ┌──────────┐    ┌────────────────────────┐  │
     ▼                  │  │  Public  │    │     Private Subnets    │  │
 ┌───────┐  HTTPS 443   │  │ Subnets  │    │                        │  │
 │Route53│─────────────▶│  │          │    │  ┌──────────────────┐ │  │
 └───────┘              │  │  ┌─────┐ │    │   │  ECS Fargate     │ │  │
                        │  │  │ ALB │─┼────┼─▶│  ┌────────────┐  │ │ │
                        │  │  └─────┘ │    │   │  │  backend   │  │  │ │
                        │  └──────────┘    │   │  └─────┬──────┘  │  │ │
                        │                  │   │        │         │  │ │
                        │                  │   │  ┌─────▼──────┐  │  │ │
                        │                  │   │  │notification│  │  │ │
                        │                  │   │  │  -service  │  │  │ │
                        │                  │   │  └────────────┘  │  │ │
                        │                  │   └────────┬─────────┘  │ │
                        │                  │           │             │ │
                        │                  │  ┌────────▼───────-──┐  │ │
                        │                  │  │   Data Layer      │  │ │
                        │                  │  │  RDS Postgres 16  │  │ │
                        │                  │  │  ElastiCache Redis│  │ │
                        │                  │  │  MSK Kafka        │  │ │
                        │                  │  └───────────────────┘  │ │
                        │                  └─────────────────────────┘ │
                        └─────────────────────────────────────────────-┘
```

---

## 2. AWS Services Used

| AWS Service | Purpose | Tier / Size |
|-------------|---------|-------------|
| **ECR** | Container image registry | Private, per-service repo |
| **ECS Fargate** | Serverless container runtime | 2 vCPU / 4 GB (backend), 0.5 vCPU / 1 GB (notification) |
| **RDS for PostgreSQL 16** | Primary database + pgvector | `db.t4g.medium` (prod: `db.r8g.large`) |
| **ElastiCache (Redis 7)** | JWT refresh tokens, idempotency | `cache.t4g.micro` (prod: `cache.r7g.large`) |
| **MSK (Kafka)** | Domain event streaming | `kafka.t3.small` × 3 brokers |
| **ALB** | HTTPS load balancer + path routing | — |
| **ACM** | TLS certificate (free) | — |
| **Route 53** | DNS | — |
| **Secrets Manager** | DB/Redis/JWT credentials | — |
| **CloudWatch** | Logs + Container Insights metrics | — |
| **Amazon Managed Grafana** | Dashboards (optional) | — |
| **S3** | Terraform state, invoice attachments | — |

---

## 3. Prerequisites & IAM Setup

### 3.1 Install tools

```bash
# AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip awscliv2.zip && sudo ./aws/install

# Configure credentials
aws configure
# AWS Access Key ID: <your key>
# AWS Secret Access Key: <your secret>
# Default region: us-east-1
# Default output format: json
```

### 3.2 Create a deployment IAM role (least-privilege)

```bash
# Create policy document
cat > sp-deploy-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": ["ecr:*"],               "Resource": "*" },
    { "Effect": "Allow", "Action": ["ecs:*"],               "Resource": "*" },
    { "Effect": "Allow", "Action": ["elasticloadbalancing:*"],"Resource": "*" },
    { "Effect": "Allow", "Action": ["secretsmanager:GetSecretValue"], "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:smartprocure/*" },
    { "Effect": "Allow", "Action": ["logs:*"],              "Resource": "*" },
    { "Effect": "Allow", "Action": ["iam:PassRole"],        "Resource": "arn:aws:iam::*:role/sp-ecs-task-role" }
  ]
}
EOF

aws iam create-policy \
  --policy-name SmartProcureDeployPolicy \
  --policy-document file://sp-deploy-policy.json
```

### 3.3 ECS task execution role

```bash
aws iam create-role \
  --role-name sp-ecs-task-execution-role \
  --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":{"Service":"ecs-tasks.amazonaws.com"},
      "Action":"sts:AssumeRole"
    }]
  }'

aws iam attach-role-policy \
  --role-name sp-ecs-task-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# Allow pulling secrets
aws iam attach-role-policy \
  --role-name sp-ecs-task-execution-role \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

---

## 4. Push Images to ECR

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
ECR_BASE="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

# Create repositories
for repo in smartprocure-backend smartprocure-frontend notification-service; do
  aws ecr create-repository --repository-name "$repo" --region "$REGION"
done

# Authenticate Docker to ECR
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$ECR_BASE"

# Build & push backend (JVM)
cd backend
docker build -t "${ECR_BASE}/smartprocure-backend:latest" .
docker push "${ECR_BASE}/smartprocure-backend:latest"

# Build & push notification-service (native image)
cd ../notification-service
docker build -f Dockerfile.native \
             -t "${ECR_BASE}/notification-service:native" .
docker push "${ECR_BASE}/notification-service:native"

# Build & push frontend
cd ../frontend
docker build -t "${ECR_BASE}/smartprocure-frontend:latest" .
docker push "${ECR_BASE}/smartprocure-frontend:latest"
```

> **Tag strategy:** Also push `:<git-sha>` tags for rollback:
> ```bash
> SHA=$(git rev-parse --short HEAD)
> docker tag "${ECR_BASE}/smartprocure-backend:latest" \
>            "${ECR_BASE}/smartprocure-backend:${SHA}"
> docker push "${ECR_BASE}/smartprocure-backend:${SHA}"
> ```

---

## 5. RDS — PostgreSQL 16

### 5.1 Create subnet group

```bash
aws rds create-db-subnet-group \
  --db-subnet-group-name sp-db-subnet-group \
  --db-subnet-group-description "SmartProcure DB subnets" \
  --subnet-ids subnet-PRIVATE1 subnet-PRIVATE2
```

### 5.2 Create the instance

```bash
aws rds create-db-instance \
  --db-instance-identifier smartprocure-prod \
  --db-instance-class db.t4g.medium \
  --engine postgres \
  --engine-version 16.3 \
  --master-username sp_app \
  --master-user-password "$(openssl rand -base64 32)" \
  --db-name smartprocure \
  --allocated-storage 100 \
  --storage-type gp3 \
  --storage-encrypted \
  --no-publicly-accessible \
  --db-subnet-group-name sp-db-subnet-group \
  --vpc-security-group-ids sg-DB \
  --backup-retention-period 7 \
  --deletion-protection \
  --tags Key=Project,Value=SmartProcure Key=Env,Value=prod
```

### 5.3 Enable pgvector extension (after first connection)

```sql
-- Connect via bastion or SSM Session Manager
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

### 5.4 Connection string (store in Secrets Manager — see §8)

```
postgresql://sp_app:<password>@<rds-endpoint>:5432/smartprocure?sslmode=verify-full
```

---

## 6. ElastiCache — Redis

```bash
aws elasticache create-replication-group \
  --replication-group-id sp-redis-prod \
  --replication-group-description "SmartProcure Redis" \
  --cache-node-type cache.t4g.micro \
  --engine redis \
  --engine-version 7.1 \
  --num-cache-clusters 1 \
  --subnet-group-name sp-cache-subnet-group \
  --security-group-ids sg-REDIS \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --auth-token "$(openssl rand -base64 32)" \
  --tags Key=Project,Value=SmartProcure Key=Env,Value=prod
```

---

## 7. MSK — Apache Kafka

```bash
aws kafka create-cluster \
  --cluster-name sp-kafka-prod \
  --kafka-version "3.6.0" \
  --number-of-broker-nodes 3 \
  --broker-node-group-info '{
    "InstanceType": "kafka.t3.small",
    "ClientSubnets": ["subnet-PRIVATE1","subnet-PRIVATE2","subnet-PRIVATE3"],
    "SecurityGroups": ["sg-KAFKA"],
    "StorageInfo": { "EbsStorageInfo": { "VolumeSize": 100 } }
  }' \
  --encryption-info '{
    "EncryptionInTransit": {
      "ClientBroker": "TLS",
      "InCluster": true
    }
  }' \
  --client-authentication '{
    "Sasl": { "Scram": { "Enabled": true } }
  }'
```

After creation, retrieve the bootstrap servers:

```bash
aws kafka get-bootstrap-brokers \
  --cluster-arn <cluster-arn> \
  --query 'BootstrapBrokerStringSaslScram' \
  --output text
```

---

## 8. Secrets Manager

Store all sensitive values so ECS tasks can inject them as environment variables at runtime:

```bash
# Database
aws secretsmanager create-secret \
  --name smartprocure/prod/db \
  --secret-string '{
    "url":      "jdbc:postgresql://<rds-endpoint>:5432/smartprocure?sslmode=verify-full",
    "username": "sp_app",
    "password": "<rds-password>"
  }'

# Redis
aws secretsmanager create-secret \
  --name smartprocure/prod/redis \
  --secret-string '{
    "host":     "<elasticache-endpoint>",
    "port":     "6379",
    "password": "<auth-token>"
  }'

# JWT
aws secretsmanager create-secret \
  --name smartprocure/prod/jwt \
  --secret-string '{
    "secret":          "<256-bit-random-string>",
    "expirationMs":    "900000",
    "refreshExpDays":  "7"
  }'

# Kafka
aws secretsmanager create-secret \
  --name smartprocure/prod/kafka \
  --secret-string '{
    "bootstrapServers": "<msk-bootstrap-brokers>",
    "username":         "sp-kafka-user",
    "password":         "<scram-password>"
  }'

# SMTP (SES)
aws secretsmanager create-secret \
  --name smartprocure/prod/smtp \
  --secret-string '{
    "host":     "email-smtp.us-east-1.amazonaws.com",
    "port":     "587",
    "username": "<ses-smtp-user>",
    "password": "<ses-smtp-password>",
    "from":     "noreply@smartprocure.io"
  }'
```

---

## 9. ECS Cluster & Task Definitions

### 9.1 Create the cluster

```bash
aws ecs create-cluster \
  --cluster-name smartprocure-prod \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE,weight=1,base=1 \
    capacityProvider=FARGATE_SPOT,weight=4 \
  --tags key=Project,value=SmartProcure key=Env,value=prod
```

### 9.2 Backend task definition

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

cat > backend-task-def.json <<EOF
{
  "family": "smartprocure-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "2048",
  "memory": "4096",
  "executionRoleArn": "arn:aws:iam::${ACCOUNT_ID}:role/sp-ecs-task-execution-role",
  "taskRoleArn":      "arn:aws:iam::${ACCOUNT_ID}:role/sp-ecs-task-role",
  "containerDefinitions": [
    {
      "name": "backend",
      "image": "${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/smartprocure-backend:latest",
      "essential": true,
      "portMappings": [{ "containerPort": 8080, "protocol": "tcp" }],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE",    "value": "prod" },
        { "name": "SERVER_PORT",               "value": "8080" }
      ],
      "secrets": [
        { "name": "DB_URL",                  "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/db:url::" },
        { "name": "DB_USERNAME",             "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/db:username::" },
        { "name": "DB_PASSWORD",             "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/db:password::" },
        { "name": "REDIS_HOST",              "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/redis:host::" },
        { "name": "REDIS_PASSWORD",          "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/redis:password::" },
        { "name": "KAFKA_BOOTSTRAP_SERVERS", "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/kafka:bootstrapServers::" },
        { "name": "JWT_SECRET",              "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/jwt:secret::" },
        { "name": "MAIL_HOST",              "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/smtp:host::" },
        { "name": "MAIL_USERNAME",          "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/smtp:username::" },
        { "name": "MAIL_PASSWORD",          "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/smtp:password::" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group":         "/ecs/smartprocure-backend",
          "awslogs-region":        "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command":     ["CMD-SHELL", "wget -q --spider http://localhost:8080/actuator/health || exit 1"],
        "interval":    15,
        "timeout":     5,
        "retries":     3,
        "startPeriod": 60
      }
    }
  ]
}
EOF

aws ecs register-task-definition \
  --cli-input-json file://backend-task-def.json
```

### 9.3 Notification-service task definition (native image)

```bash
cat > notification-task-def.json <<EOF
{
  "family": "smartprocure-notification",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::${ACCOUNT_ID}:role/sp-ecs-task-execution-role",
  "containerDefinitions": [
    {
      "name": "notification-service",
      "image": "${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/notification-service:native",
      "essential": true,
      "portMappings": [{ "containerPort": 8081, "protocol": "tcp" }],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" }
      ],
      "secrets": [
        { "name": "KAFKA_BOOTSTRAP_SERVERS", "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/kafka:bootstrapServers::" },
        { "name": "REDIS_HOST",             "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/redis:host::" },
        { "name": "REDIS_PASSWORD",         "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/redis:password::" },
        { "name": "MAIL_HOST",              "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/smtp:host::" },
        { "name": "MAIL_USERNAME",          "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/smtp:username::" },
        { "name": "MAIL_PASSWORD",          "valueFrom": "arn:aws:secretsmanager:us-east-1:${ACCOUNT_ID}:secret:smartprocure/prod/smtp:password::" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group":         "/ecs/smartprocure-notification",
          "awslogs-region":        "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command":     ["CMD-SHELL", "wget -qO- http://localhost:8081/actuator/health || exit 1"],
        "interval":    15,
        "timeout":     5,
        "retries":     3,
        "startPeriod": 10
      }
    }
  ]
}
EOF

aws ecs register-task-definition \
  --cli-input-json file://notification-task-def.json
```

### 9.4 Create CloudWatch log groups

```bash
aws logs create-log-group --log-group-name /ecs/smartprocure-backend
aws logs create-log-group --log-group-name /ecs/smartprocure-notification
aws logs put-retention-policy \
  --log-group-name /ecs/smartprocure-backend \
  --retention-in-days 30
```

---

## 10. Application Load Balancer

```bash
# Create ALB (internet-facing, public subnets)
ALB_ARN=$(aws elbv2 create-load-balancer \
  --name sp-alb-prod \
  --subnets subnet-PUBLIC1 subnet-PUBLIC2 \
  --security-groups sg-ALB \
  --scheme internet-facing \
  --type application \
  --query 'LoadBalancers[0].LoadBalancerArn' \
  --output text)

# Target group — backend
BACKEND_TG_ARN=$(aws elbv2 create-target-group \
  --name sp-backend-tg \
  --protocol HTTP \
  --port 8080 \
  --target-type ip \
  --vpc-id vpc-XXXXXX \
  --health-check-path /actuator/health \
  --health-check-interval-seconds 15 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3 \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

# HTTPS listener (port 443) → forward to backend
aws elbv2 create-listener \
  --load-balancer-arn "$ALB_ARN" \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:us-east-1:${ACCOUNT_ID}:certificate/CERT-ID \
  --default-actions Type=forward,TargetGroupArn="$BACKEND_TG_ARN"

# HTTP listener → redirect to HTTPS
aws elbv2 create-listener \
  --load-balancer-arn "$ALB_ARN" \
  --protocol HTTP \
  --port 80 \
  --default-actions '[{
    "Type": "redirect",
    "RedirectConfig": {
      "Protocol": "HTTPS", "Port": "443", "StatusCode": "HTTP_301"
    }
  }]'
```

---

## 11. ECS Services

### 11.1 Backend service

```bash
aws ecs create-service \
  --cluster smartprocure-prod \
  --service-name backend \
  --task-definition smartprocure-backend \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration '{
    "awsvpcConfiguration": {
      "subnets":        ["subnet-PRIVATE1","subnet-PRIVATE2"],
      "securityGroups": ["sg-ECS-BACKEND"],
      "assignPublicIp": "DISABLED"
    }
  }' \
  --load-balancers '[{
    "targetGroupArn": "'"$BACKEND_TG_ARN"'",
    "containerName":  "backend",
    "containerPort":  8080
  }]' \
  --deployment-configuration '{
    "minimumHealthyPercent": 100,
    "maximumPercent":        200
  }' \
  --health-check-grace-period-seconds 90 \
  --enable-execute-command
```

### 11.2 Notification-service (no ALB — Kafka consumer only)

```bash
aws ecs create-service \
  --cluster smartprocure-prod \
  --service-name notification-service \
  --task-definition smartprocure-notification \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration '{
    "awsvpcConfiguration": {
      "subnets":        ["subnet-PRIVATE1","subnet-PRIVATE2"],
      "securityGroups": ["sg-ECS-NOTIFICATION"],
      "assignPublicIp": "DISABLED"
    }
  }' \
  --deployment-configuration '{
    "minimumHealthyPercent": 50,
    "maximumPercent":        200
  }' \
  --enable-execute-command
```

### 11.3 Auto-scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/smartprocure-prod/backend \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# CPU-based scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/smartprocure-prod/backend \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name sp-backend-cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 60.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown":  120,
    "ScaleOutCooldown": 60
  }'
```

---

## 12. Observability — CloudWatch + Managed Grafana

### 12.1 CloudWatch Container Insights

```bash
aws ecs update-cluster-settings \
  --cluster smartprocure-prod \
  --settings name=containerInsights,value=enabled
```

### 12.2 Prometheus scraping via CloudWatch Agent

Add a sidecar container to the backend task definition that runs the CloudWatch agent
in Prometheus mode — it scrapes `/actuator/prometheus` and ships metrics to CloudWatch:

```json
{
  "name": "cwagent",
  "image": "public.ecr.aws/cloudwatch-agent/cloudwatch-agent:latest",
  "essential": false,
  "environment": [
    { "name": "CW_CONFIG_CONTENT", "value": "{\"logs\":{\"metrics_collected\":{\"prometheus\":{\"cluster_name\":\"smartprocure-prod\",\"log_group_name\":\"/ecs/smartprocure-prometheus\",\"prometheus_config_path\":\"/etc/cwagentconfig/prometheus.yaml\",\"emf_processor\":{\"metric_declaration_dedup\":true,\"metric_namespace\":\"SmartProcure\",\"metric_unit\":{},\"metric_declaration\":[{\"source_labels\":[\"__name__\"],\"label_matcher\":\"^smartprocure_\",\"dimensions\":[[\"module\"]],\"metric_selectors\":[\"^smartprocure_\"]}]}}}}}" }
  ]
}
```

### 12.3 Alerting via CloudWatch Alarms → SNS

```bash
# SNS topic for ops notifications
aws sns create-topic --name sp-prod-alerts
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:${ACCOUNT_ID}:sp-prod-alerts \
  --protocol email \
  --notification-endpoint ops@smartprocure.io

# Example: p95 latency alarm
aws cloudwatch put-metric-alarm \
  --alarm-name "sp-backend-p95-latency" \
  --alarm-description "HTTP p95 latency > 1s for 3 consecutive minutes" \
  --namespace SmartProcure \
  --metric-name http_server_requests_seconds \
  --dimensions Name=quantile,Value=0.95 \
  --statistic Average \
  --period 60 \
  --evaluation-periods 3 \
  --threshold 1.0 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:${ACCOUNT_ID}:sp-prod-alerts
```

---

## 13. DNS & TLS — Route 53 + ACM

```bash
# Request certificate (DNS validation)
CERT_ARN=$(aws acm request-certificate \
  --domain-name api.smartprocure.io \
  --subject-alternative-names "*.smartprocure.io" \
  --validation-method DNS \
  --query CertificateArn \
  --output text)

# After DNS CNAME validation completes, get the ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names sp-alb-prod \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

# Create Route 53 alias record
HOSTED_ZONE_ID=$(aws route53 list-hosted-zones-by-name \
  --dns-name smartprocure.io \
  --query 'HostedZones[0].Id' \
  --output text | cut -d/ -f3)

aws route53 change-resource-record-sets \
  --hosted-zone-id "$HOSTED_ZONE_ID" \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name":   "api.smartprocure.io",
        "Type":   "A",
        "AliasTarget": {
          "HostedZoneId":         "Z35SXDOTRQ7X7K",
          "DNSName":              "'"$ALB_DNS"'",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'
```

---

## 14. Environment Variables Reference

| Variable | Source | Example value |
|----------|--------|--------------|
| `SPRING_PROFILES_ACTIVE` | ECS env | `prod` |
| `DB_URL` | Secrets Manager | `jdbc:postgresql://…:5432/smartprocure?sslmode=verify-full` |
| `DB_USERNAME` | Secrets Manager | `sp_app` |
| `DB_PASSWORD` | Secrets Manager | `<rds-password>` |
| `REDIS_HOST` | Secrets Manager | `sp-redis.xxx.cache.amazonaws.com` |
| `REDIS_PORT` | ECS env | `6379` |
| `REDIS_PASSWORD` | Secrets Manager | `<auth-token>` |
| `KAFKA_BOOTSTRAP_SERVERS` | Secrets Manager | `b-1.xxx.kafka.us-east-1.amazonaws.com:9096` |
| `KAFKA_CONSUMER_GROUP` | ECS env | `sp-main-group` |
| `JWT_SECRET` | Secrets Manager | `<256-bit-random>` |
| `MAIL_HOST` | Secrets Manager | `email-smtp.us-east-1.amazonaws.com` |
| `MAIL_PORT` | ECS env | `587` |
| `MAIL_AUTH` | ECS env | `true` |
| `MAIL_TLS` | ECS env | `true` |
| `OLLAMA_BASE_URL` | ECS env | `http://ollama-service:11434` (self-hosted EC2/ECS) |

---

## 15. CI/CD — GitHub Actions

```yaml
# .github/workflows/deploy-aws.yml
name: Deploy to AWS ECS

on:
  push:
    branches: [main]

env:
  AWS_REGION:    us-east-1
  ECR_REGISTRY:  ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com
  CLUSTER:       smartprocure-prod

jobs:
  build-push-deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write   # OIDC
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/sp-github-deploy-role
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to ECR
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run unit tests
        # -DskipITs skips Testcontainers integration tests which require Docker-in-Docker.
        # Run mvn verify (with integration tests) in a separate nightly job with DinD if needed.
        run: cd backend && mvn -B test -DskipITs

      - name: Build & push backend image
        env:
          SHA: ${{ github.sha }}
        run: |
          cd backend
          docker build -t $ECR_REGISTRY/smartprocure-backend:$SHA \
                       -t $ECR_REGISTRY/smartprocure-backend:latest .
          docker push $ECR_REGISTRY/smartprocure-backend:$SHA
          docker push $ECR_REGISTRY/smartprocure-backend:latest

      - name: Build & push notification-service (native)
        env:
          SHA: ${{ github.sha }}
        run: |
          cd notification-service
          docker build -f Dockerfile.native \
                       -t $ECR_REGISTRY/notification-service:$SHA \
                       -t $ECR_REGISTRY/notification-service:native .
          docker push $ECR_REGISTRY/notification-service:$SHA
          docker push $ECR_REGISTRY/notification-service:native

      - name: Deploy backend to ECS
        run: |
          aws ecs update-service \
            --cluster $CLUSTER \
            --service backend \
            --force-new-deployment

      - name: Deploy notification-service to ECS
        run: |
          aws ecs update-service \
            --cluster $CLUSTER \
            --service notification-service \
            --force-new-deployment

      - name: Wait for backend stability
        run: |
          aws ecs wait services-stable \
            --cluster $CLUSTER \
            --services backend notification-service
```

---

## 16. Scaling & Cost Optimisation

| Recommendation | Detail |
|----------------|--------|
| **Fargate Spot** | Use 80 % FARGATE_SPOT / 20 % FARGATE for notification-service (stateless Kafka consumer) — saves ~70 % compute cost |
| **RDS Reserved Instances** | 1-year RI for `db.t4g.medium` saves ~40 % vs on-demand |
| **ElastiCache Reserved** | 1-year reserved node saves ~35 % |
| **ECR Lifecycle Policy** | Keep only the last 10 images per repo; delete untagged automatically |
| **S3 Intelligent Tiering** | For invoice attachment bucket (infrequent access after 30 days) |
| **Savings Plans** | Compute Savings Plan covers ECS Fargate across all tasks |

---

## 17. Runbook — Common Operations

### Deploy a new version

```bash
SHA=$(git rev-parse --short HEAD)
aws ecs update-service \
  --cluster smartprocure-prod \
  --service backend \
  --task-definition smartprocure-backend:LATEST \
  --force-new-deployment
aws ecs wait services-stable \
  --cluster smartprocure-prod \
  --services backend
```

### Roll back to a previous image

```bash
# List recent task definitions
aws ecs list-task-definitions \
  --family-prefix smartprocure-backend \
  --sort DESC --max-items 5

# Update service to a specific revision
aws ecs update-service \
  --cluster smartprocure-prod \
  --service backend \
  --task-definition smartprocure-backend:42
```

### Open a shell in a running Fargate task (ECS Exec)

```bash
TASK_ID=$(aws ecs list-tasks \
  --cluster smartprocure-prod \
  --service-name backend \
  --query 'taskArns[0]' --output text | cut -d/ -f3)

aws ecs execute-command \
  --cluster smartprocure-prod \
  --task "$TASK_ID" \
  --container backend \
  --interactive \
  --command "/bin/sh"
```

### View live logs

```bash
aws logs tail /ecs/smartprocure-backend --follow
```

### Force a Flyway repair (broken migration)

```bash
# Connect to RDS via SSM Session Manager bastion, then:
psql "$DB_URL" -c "DELETE FROM flyway_schema_history WHERE success = false;"
# Redeploy triggers Flyway repair automatically
```
