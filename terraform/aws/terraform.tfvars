# =============================================================================
# SmartProcure — YOUR AWS Personal Config
# =============================================================================
# ⚠️  EDIT THIS FILE with your own values before running terraform apply
# ⚠️  NEVER commit this file to Git — it contains passwords!
#     Add it to .gitignore: echo "terraform.tfvars" >> .gitignore
# =============================================================================

# ── Region ────────────────────────────────────────────────────────────────────
# us-east-1 (N. Virginia) has the best free-tier coverage
aws_region   = "us-east-1"
project_name = "smartprocure"
environment  = "dev"

# ── Instance ──────────────────────────────────────────────────────────────────
# t3.micro = FREE TIER (750 hrs/month for first 12 months of your AWS account)
# DO NOT change to a larger instance or you will be charged!
instance_type       = "t3.micro"
root_volume_size_gb = 30   # 30 GB = free-tier EBS limit — do not increase!

# ── SSH ───────────────────────────────────────────────────────────────────────
# Path to your SSH public key on THIS machine (not the cloud server)
# To generate: ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
ssh_public_key_path = "~/.ssh/id_rsa.pub"

# Your IP address for SSH access (more secure than 0.0.0.0/0)
# Find your IP: curl ifconfig.me
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
# The first admin user created when the app starts
admin_email    = "admin@smartprocure.internal"
admin_password = "Admin@SmartProcure2025!"  # ← change this!

# ── Ports (defaults are fine — only change if you have conflicts) ─────────────
frontend_port     = 3000
backend_port      = 8080
notification_port = 8081
mailhog_ui_port   = 8025
