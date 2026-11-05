# =============================================================================
# SmartProcure — YOUR GCP Personal Config
# =============================================================================
# ⚠️  EDIT THIS FILE with your own values before running terraform apply
# ⚠️  NEVER commit this file to Git — it contains passwords!
#     Add it to .gitignore: echo "terraform.tfvars" >> .gitignore
# =============================================================================

# ── Project ───────────────────────────────────────────────────────────────────
# ⚠️  REQUIRED: replace with your real GCP project ID
# Find it: gcloud config get-value project
gcp_project_id = "YOUR_GCP_PROJECT_ID"

# ── Region & Zone ─────────────────────────────────────────────────────────────
# ⚠️  MUST be one of these three for always-free e2-micro:
#   us-central1 / us-east1 / us-west1
gcp_region = "us-central1"
gcp_zone   = "us-central1-a"

# ── Project metadata ──────────────────────────────────────────────────────────
project_name = "smartprocure"
environment  = "dev"

# ── Compute ───────────────────────────────────────────────────────────────────
# e2-micro = ALWAYS FREE (do NOT change to avoid charges)
machine_type = "e2-micro"
disk_size_gb = 30            # 30 GB HDD = always free limit
disk_type    = "pd-standard" # HDD = free | pd-ssd = NOT free

# ── OS Image ──────────────────────────────────────────────────────────────────
os_image = "ubuntu-os-cloud/ubuntu-2204-lts"

# ── SSH ───────────────────────────────────────────────────────────────────────
# Linux username created on the VM for SSH login
ssh_username = "smartprocure"

# Path to your SSH PUBLIC key on THIS machine
# Generate: ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
ssh_public_key_path = "~/.ssh/id_rsa.pub"

# Your IP for SSH access — find it: curl ifconfig.me
# Format: "1.2.3.4/32"
allowed_ssh_cidr = "0.0.0.0/0"   # ← change to your IP for better security

# ── Database ──────────────────────────────────────────────────────────────────
db_username = "smartprocure"
db_password = "smartprocure_dev"  # ← change this to something strong!

# ── Application Secrets ───────────────────────────────────────────────────────
# Generate a secure JWT secret:
#   openssl rand -base64 64
jwt_secret = "bXktc3VwZXItc2VjcmV0LWtleS1mb3Itc21hcnRwcm9jdXJlLWFwcC0yMDI1LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM="

# ── Admin Account ─────────────────────────────────────────────────────────────
admin_email    = "admin@smartprocure.internal"
admin_password = "Admin@SmartProcure2025!"  # ← change this!

# ── Ports (defaults are fine) ─────────────────────────────────────────────────
frontend_port     = 3000
backend_port      = 8080
notification_port = 8081
mailhog_ui_port   = 8025
