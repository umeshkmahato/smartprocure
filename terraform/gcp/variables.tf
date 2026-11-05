# =============================================================================
# GCP Variables — SmartProcure
# =============================================================================
# These are all the settings you can customize.
# Set your personal values in terraform.tfvars (never commit that file to Git!)
# =============================================================================

# ── Project ───────────────────────────────────────────────────────────────────
variable "gcp_project_id" {
  description = <<EOT
Your GCP Project ID (not the project name or number).
Find it: gcloud config get-value project
  OR: GCP Console → top-left dropdown → copy the ID column value
EOT
  type        = string
}

variable "gcp_region" {
  description = <<EOT
GCP region to deploy in.
IMPORTANT: e2-micro is ALWAYS FREE only in these three regions:
  us-central1  (Iowa)
  us-east1     (South Carolina)
  us-west1     (Oregon)
Choosing any other region will incur charges!
EOT
  type    = string
  default = "us-central1"
}

variable "gcp_zone" {
  description = "Availability zone inside the region. Must match your chosen region."
  type        = string
  default     = "us-central1-a"
}

# ── Project metadata ──────────────────────────────────────────────────────────
variable "project_name" {
  description = "Short name for this project. Used in every resource name."
  type        = string
  default     = "smartprocure"
}

variable "environment" {
  description = "Environment label (dev / staging / prod)."
  type        = string
  default     = "dev"
}

# ── Networking ────────────────────────────────────────────────────────────────
variable "vpc_cidr" {
  description = "IP range for your private network inside GCP."
  type        = string
  default     = "10.0.0.0/16"
}

variable "subnet_cidr" {
  description = "IP range for the subnet. Must be inside vpc_cidr."
  type        = string
  default     = "10.0.1.0/24"
}

# ── Compute ───────────────────────────────────────────────────────────────────
variable "machine_type" {
  description = <<EOT
GCP machine type.
  e2-micro  = ALWAYS FREE (1 vCPU, 1 GB RAM) ← use this
  e2-small  = ~$7/month    (2 vCPU, 2 GB RAM)
  e2-medium = ~$14/month   (2 vCPU, 4 GB RAM)
NOTE: e2-micro only has 1 GB RAM. If services crash due to memory,
      upgrade to e2-small and accept the small monthly charge.
EOT
  type    = string
  default = "e2-micro"
}

variable "disk_size_gb" {
  description = <<EOT
Boot disk size in GB.
30 GB of standard (HDD) persistent disk is ALWAYS FREE.
Do NOT use SSD (pd-ssd) — it is NOT free tier.
EOT
  type    = number
  default = 30
}

variable "disk_type" {
  description = "Disk type. pd-standard (HDD) = free tier. pd-ssd = NOT free."
  type        = string
  default     = "pd-standard"
}

variable "os_image" {
  description = "OS image for the VM. Ubuntu 22.04 LTS is stable and well supported."
  type        = string
  default     = "ubuntu-os-cloud/ubuntu-2204-lts"
}

# ── SSH Access ────────────────────────────────────────────────────────────────
variable "ssh_username" {
  description = "Linux username for SSH login. This user is created on the VM."
  type        = string
  default     = "smartprocure"
}

variable "ssh_public_key_path" {
  description = <<EOT
Path to your SSH PUBLIC key file on this machine.
Generate one if you don't have it:
  ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
Then this path is: ~/.ssh/id_rsa.pub
EOT
  type    = string
  default = "~/.ssh/id_rsa.pub"
}

variable "allowed_ssh_cidr" {
  description = <<EOT
IP address allowed to SSH into the server.
Use your own IP for security: run `curl ifconfig.me` to find it.
Format: "YOUR.IP.ADDRESS/32"
"0.0.0.0/0" allows everyone — OK for learning, bad for production.
EOT
  type    = string
  default = "0.0.0.0/0"
}

# ── Application ───────────────────────────────────────────────────────────────
variable "db_username" {
  description = "PostgreSQL database username."
  type        = string
  default     = "smartprocure"
}

variable "db_password" {
  description = "PostgreSQL database password. Change this!"
  type        = string
  sensitive   = true
  default     = "smartprocure_dev"
}

variable "jwt_secret" {
  description = <<EOT
Secret key used to sign JWT tokens. Must be base64-encoded, at least 256 bits.
Generate: openssl rand -base64 64
EOT
  type      = string
  sensitive = true
  default   = "bXktc3VwZXItc2VjcmV0LWtleS1mb3Itc21hcnRwcm9jdXJlLWFwcC0yMDI1LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM="
}

variable "admin_email" {
  description = "Email address for the auto-created admin account."
  type        = string
  default     = "admin@smartprocure.internal"
}

variable "admin_password" {
  description = "Password for the auto-created admin account. Change this!"
  type        = string
  sensitive   = true
  default     = "Admin@SmartProcure2025!"
}

variable "frontend_port" {
  description = "Port the React frontend is served on."
  type        = number
  default     = 3000
}

variable "backend_port" {
  description = "Port the Spring Boot backend API is served on."
  type        = number
  default     = 8080
}

variable "notification_port" {
  description = "Port the notification service is served on."
  type        = number
  default     = 8081
}

variable "mailhog_ui_port" {
  description = "Port for the MailHog email web UI."
  type        = number
  default     = 8025
}
