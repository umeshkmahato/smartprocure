# =============================================================================
# AWS Variables — SmartProcure
# =============================================================================
# These are all the settings you can customize.
# Set your personal values in terraform.tfvars (never commit that file!)
# =============================================================================

# ── Region ────────────────────────────────────────────────────────────────────
variable "aws_region" {
  description = "AWS region to deploy in. us-east-1 has the best free-tier support."
  type        = string
  default     = "us-east-1"
}

# ── Project ───────────────────────────────────────────────────────────────────
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
  description = "IP range for your private network. 10.0.0.0/16 gives you 65 536 addresses."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "IP range for the public subnet inside the VPC."
  type        = string
  default     = "10.0.1.0/24"
}

# ── Compute ───────────────────────────────────────────────────────────────────
variable "instance_type" {
  description = <<EOT
EC2 instance type.
  t3.micro  = FREE TIER (750 hrs/month for 12 months) ← use this
  t3.small  = ~$15/month (more RAM, needed after free tier)
EOT
  type        = string
  default     = "t3.micro"
}

variable "root_volume_size_gb" {
  description = "Disk size in GB. 30 GB is the free-tier limit — do not exceed it."
  type        = number
  default     = 30
}

# ── SSH Access ────────────────────────────────────────────────────────────────
variable "ssh_public_key_path" {
  description = <<EOT
Path to your SSH PUBLIC key file on this machine.
Generate one if you don't have it:
  ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa
Then this path is: ~/.ssh/id_rsa.pub
EOT
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "allowed_ssh_cidr" {
  description = <<EOT
IP address allowed to SSH into the server.
Use your own IP for security: run `curl ifconfig.me` to find it.
Format: "YOUR.IP.ADDRESS/32"
Using "0.0.0.0/0" allows everyone — OK for learning, bad for production.
EOT
  type        = string
  default     = "0.0.0.0/0"
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
  sensitive   = true # Terraform will never print this in logs
  default     = "smartprocure_dev"
}

variable "jwt_secret" {
  description = <<EOT
Secret key used to sign JWT tokens. Must be a base64-encoded string of at least 256 bits.
Generate a secure one:
  openssl rand -base64 64
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
