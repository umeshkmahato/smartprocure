# 🚀 SmartProcure — Terraform Infrastructure Guide
# For Beginners | Free-Tier Safe (AWS & GCP)

---

## 📖 What Is Terraform?

Terraform is a tool that lets you **describe your cloud infrastructure as code**.
Instead of clicking around in AWS or GCP consoles, you write files and Terraform
creates everything for you automatically.

Think of it like a recipe:
- You write the recipe (`.tf` files)
- Terraform cooks it (provisions cloud resources)
- You can delete everything with one command (`terraform destroy`)

---

## 🗂️ Folder Structure

```
terraform/
├── README.md              ← You are here
├── aws/                   ← Everything for Amazon Web Services
│   ├── backend.tf         ← Where Terraform saves its state file (S3)
│   ├── variables.tf       ← All settings with descriptions
│   ├── main.tf            ← Actual cloud resources (VPC, EC2, etc.)
│   ├── outputs.tf         ← Values shown after deploy (IP, URLs)
│   ├── terraform.tfvars   ← YOUR personal config — edit this file
│   └── scripts/
│       └── userdata.sh    ← Script that runs on EC2 at first boot
└── gcp/                   ← Everything for Google Cloud Platform
    ├── backend.tf         ← Where Terraform saves its state file (GCS)
    ├── variables.tf       ← All settings with descriptions
    ├── main.tf            ← Actual cloud resources (VPC, VM, etc.)
    ├── outputs.tf         ← Values shown after deploy (IP, URLs)
    ├── terraform.tfvars   ← YOUR personal config — edit this file
    └── scripts/
        └── startup.sh     ← Script that runs on GCP VM at first boot
```

---

## 💰 Cost Summary (Free-Tier Safe)

| What runs where | AWS | GCP |
|-----------------|-----|-----|
| **VM (server)** | EC2 `t3.micro` — FREE 12 months | Compute Engine `e2-micro` — **ALWAYS FREE** |
| **Disk** | 30 GB EBS gp3 — FREE 12 months | 30 GB HDD — **ALWAYS FREE** |
| **Public IP** | Elastic IP (free when attached) | Static IP (free when attached) |
| **All services** | Run inside Docker on the VM | Run inside Docker on the VM |
| **Estimated monthly cost** | **$0 (first 12 months)** | **$0–7/month always** |

> ⚠️  Ollama/AI is **disabled** in cloud deployments — GPU instances are expensive.
> Run Ollama locally on your laptop instead.

---

## ✅ Prerequisites

### For AWS
1. **AWS Account** → https://aws.amazon.com/free
2. **AWS CLI** installed → https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html
3. **Configure credentials**:
   ```bash
   aws configure
   # Enter: Access Key ID, Secret Access Key, Region (us-east-1), Output (json)
   ```
4. **Terraform** installed → https://developer.hashicorp.com/terraform/downloads
5. **SSH key pair** on your machine:
   ```bash
   ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
   ```

### For GCP
1. **GCP Account** → https://cloud.google.com/free
2. **Create a GCP Project** in the console
3. **Enable Compute Engine API**:
   ```bash
   gcloud services enable compute.googleapis.com
   ```
4. **gcloud CLI** installed → https://cloud.google.com/sdk/docs/install
5. **Authenticate**:
   ```bash
   gcloud auth application-default login
   ```
6. **Terraform** installed (same as above)
7. **SSH key pair** (same as above)

---

## 🚀 Deploy to AWS (Step by Step)

### Step 1 — Create the S3 bucket for Terraform state
> Skip this if you want to use local state (see note in `aws/backend.tf`)
```bash
aws s3api create-bucket \
  --bucket smartprocure-tfstate-$(aws sts get-caller-identity --query Account --output text) \
  --region us-east-1

aws s3api put-bucket-versioning \
  --bucket smartprocure-tfstate-$(aws sts get-caller-identity --query Account --output text) \
  --versioning-configuration Status=Enabled

aws dynamodb create-table \
  --table-name smartprocure-tfstate-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Step 2 — Edit your config
```bash
cd terraform/aws
# Open terraform.tfvars and fill in YOUR values
```

### Step 3 — Deploy
```bash
cd terraform/aws
terraform init       # Download AWS provider plugin
terraform plan       # Preview what will be created (safe, no changes yet)
terraform apply      # Create the resources (type 'yes' when prompted)
```

### Step 4 — Get your server's IP
After `terraform apply` finishes, you'll see:
```
Outputs:
  server_public_ip = "1.2.3.4"
  frontend_url     = "http://1.2.3.4:3000"
  backend_url      = "http://1.2.3.4:8080"
  ssh_command      = "ssh -i ~/.ssh/id_rsa ubuntu@1.2.3.4"
```

### Step 5 — Wait ~5 minutes, then access the app
The startup script takes 3–5 minutes to install Docker and start all services.
```bash
# SSH into your server
ssh -i ~/.ssh/id_rsa ubuntu@YOUR_SERVER_IP

# Check if services are running
docker compose ps

# See logs
docker compose logs -f
```

### Step 6 — Destroy (stop paying / stop using)
```bash
terraform destroy    # Deletes EVERYTHING — type 'yes' when prompted
```

---

## 🚀 Deploy to GCP (Step by Step)

### Step 1 — Create GCS bucket for Terraform state
> Skip this if you want to use local state (see note in `gcp/backend.tf`)
```bash
gcloud storage buckets create gs://smartprocure-tfstate-YOUR_PROJECT_ID \
  --location=us-central1 \
  --uniform-bucket-level-access
```

### Step 2 — Edit your config
```bash
cd terraform/gcp
# Open terraform.tfvars and fill in YOUR values (especially gcp_project_id)
```

### Step 3 — Deploy
```bash
cd terraform/gcp
terraform init
terraform plan
terraform apply
```

### Step 4 — Get your server's IP
```
Outputs:
  server_public_ip = "34.1.2.3"
  frontend_url     = "http://34.1.2.3:3000"
  backend_url      = "http://34.1.2.3:8080"
  ssh_command      = "ssh -i ~/.ssh/id_rsa smartprocure@34.1.2.3"
```

### Step 5 — Destroy
```bash
terraform destroy
```

---

## 🐳 Deploy the Application (after infrastructure is running)

The startup script starts the **infrastructure** (Postgres, Redis, Kafka, MailHog).
To deploy the **application** (backend, frontend, notification service):

```bash
# SSH into your server
ssh -i ~/.ssh/id_rsa ubuntu@YOUR_SERVER_IP   # AWS
ssh -i ~/.ssh/id_rsa smartprocure@YOUR_SERVER_IP  # GCP

# Go to app directory
cd /opt/smartprocure

# Clone your repository (replace with your actual repo URL)
git clone https://github.com/YOUR_USERNAME/smartprocure.git app
cd app

# Build and start the application
docker compose -f docker-compose.app.yml up --build -d
```

---

## ❓ Common Issues

| Problem | Solution |
|---------|----------|
| `terraform init` fails | Check internet connection, re-run |
| `Error: No valid credential sources` | Run `aws configure` or `gcloud auth login` |
| SSH connection refused | Wait 5 more minutes for startup script to finish |
| Services not starting | SSH in and run `sudo journalctl -u smartprocure -f` |
| `t3.micro` not free tier | Verify your AWS account is < 12 months old |
| GCP billing alert | Make sure you're using `e2-micro` in `us-central1` |

---

## 📞 Useful Commands

```bash
# See all running containers on the VM
docker ps

# See logs of a specific service
docker logs sp-postgres
docker logs sp-redis
docker logs sp-kafka

# Restart all services
docker compose restart

# Stop all services
docker compose down

# Check disk usage
df -h

# Check memory usage
free -h
```
