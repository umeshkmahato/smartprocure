# =============================================================================
# SmartProcure — GCP Infrastructure (Always-Free Safe)
# =============================================================================
# What this file creates:
#   1. Provider      → tells Terraform to use GCP
#   2. VPC Network   → your private network in GCP
#   3. Subnet        → a section of that network in your chosen region
#   4. Firewall Rules→ what traffic is allowed in/out (like AWS Security Groups)
#   5. Static IP     → a fixed public IP address for your server
#   6. VM Instance   → the actual virtual machine (e2-micro = always free)
# =============================================================================

# ── 1. Provider ───────────────────────────────────────────────────────────────
# Tells Terraform: "Talk to GCP for this project in this region"
provider "google" {
  project = var.gcp_project_id
  region  = var.gcp_region
  zone    = var.gcp_zone
}

# ── 2. VPC Network ────────────────────────────────────────────────────────────
# In GCP, a VPC is global (spans all regions).
# auto_create_subnetworks = false means WE control the subnets.
resource "google_compute_network" "main" {
  name                    = "${var.project_name}-vpc"
  auto_create_subnetworks = false   # we define our own subnets below
  description             = "SmartProcure VPC — managed by Terraform"
}

# ── 3. Subnet ─────────────────────────────────────────────────────────────────
# A subnet must be in a specific region. Resources in this subnet
# get private IPs from the cidr_block range.
resource "google_compute_subnetwork" "main" {
  name          = "${var.project_name}-subnet"
  ip_cidr_range = var.subnet_cidr          # 10.0.1.0/24
  region        = var.gcp_region
  network       = google_compute_network.main.id

  # Enables Google's private access — VMs without external IPs
  # can still reach Google APIs (optional but good practice)
  private_ip_google_access = true
}

# ── 4a. Firewall Rule — Allow inbound app traffic ─────────────────────────────
# In GCP, firewall rules attach to the NETWORK (not the VM directly).
# Any VM with the matching "target_tag" gets these rules applied.
resource "google_compute_firewall" "allow_app" {
  name        = "${var.project_name}-allow-app"
  network     = google_compute_network.main.id
  description = "Allow inbound traffic to SmartProcure application ports"
  direction   = "INGRESS"
  priority    = 1000

  allow {
    protocol = "tcp"
    ports = [
      "22",                                        # SSH
      "80",                                        # HTTP
      "443",                                       # HTTPS
      tostring(var.backend_port),                  # 8080 — Spring Boot backend
      tostring(var.notification_port),             # 8081 — Notification service
      tostring(var.frontend_port),                 # 3000 — React frontend
      tostring(var.mailhog_ui_port),               # 8025 — MailHog UI
      "3001",                                      # Grafana dashboards
    ]
  }

  # Only apply to VMs tagged with this label
  target_tags = ["${var.project_name}-server"]

  # Allow from everywhere (restrict to your IP for better security)
  source_ranges = ["0.0.0.0/0"]
}

# ── 4b. Firewall Rule — Allow all outbound traffic ────────────────────────────
# VMs need to reach the internet to pull Docker images etc.
resource "google_compute_firewall" "allow_egress" {
  name        = "${var.project_name}-allow-egress"
  network     = google_compute_network.main.id
  description = "Allow all outbound traffic from SmartProcure VMs"
  direction   = "EGRESS"
  priority    = 1000

  allow {
    protocol = "all"   # all protocols, all ports
  }

  target_tags        = ["${var.project_name}-server"]
  destination_ranges = ["0.0.0.0/0"]
}

# ── 4c. Firewall Rule — Allow SSH from your IP only ───────────────────────────
resource "google_compute_firewall" "allow_ssh" {
  name        = "${var.project_name}-allow-ssh"
  network     = google_compute_network.main.id
  description = "Allow SSH into SmartProcure VMs"
  direction   = "INGRESS"
  priority    = 900   # higher priority than app rule (lower number = higher priority)

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  target_tags   = ["${var.project_name}-server"]
  source_ranges = [var.allowed_ssh_cidr]
}

# ── 5. Static External IP ─────────────────────────────────────────────────────
# Without this, your VM gets a new random IP every time it restarts.
# Cost: FREE when attached to a RUNNING instance.
#       ~$0.01/hr when unattached (instance is stopped) → remember to destroy!
resource "google_compute_address" "app" {
  name         = "${var.project_name}-static-ip"
  region       = var.gcp_region
  address_type = "EXTERNAL"
  description  = "Static public IP for SmartProcure server"
}

# ── 6. Compute Instance — The Virtual Machine ─────────────────────────────────
# e2-micro is ALWAYS FREE in us-central1, us-east1, us-west1
# Limits: 1 vCPU (burst), 1 GB RAM, 30 GB HDD disk
resource "google_compute_instance" "app" {
  name         = "${var.project_name}-server"
  machine_type = var.machine_type   # e2-micro (always free)
  zone         = var.gcp_zone
  description  = "SmartProcure application server — managed by Terraform"

  # Tags are used by firewall rules above to target this VM
  tags = ["${var.project_name}-server"]

  labels = {
    project     = var.project_name
    environment = var.environment
    managed_by  = "terraform"
  }

  # Boot disk: use pd-standard (HDD) — 30 GB is free tier
  # DO NOT use pd-ssd (not free) or exceed 30 GB
  boot_disk {
    initialize_params {
      image = var.os_image          # Ubuntu 22.04 LTS
      size  = var.disk_size_gb      # 30 GB (free tier limit)
      type  = var.disk_type         # pd-standard (HDD, free tier)
    }
  }

  # Network interface — attaches the VM to our subnet with the static IP
  network_interface {
    network    = google_compute_network.main.id
    subnetwork = google_compute_subnetwork.main.id

    # Attach the static external IP so the VM is reachable from the internet
    access_config {
      nat_ip = google_compute_address.app.address
    }
  }

  # SSH key — GCP injects this into the VM via metadata
  # Format: "username:ssh-rsa AAAA..."
  metadata = {
    ssh-keys = "${var.ssh_username}:${file(pathexpand(var.ssh_public_key_path))}"

    # Startup script: runs ONCE when VM first boots
    # Installs Docker and starts the SmartProcure stack
    startup-script = templatefile("${path.module}/scripts/startup.sh", {
      project_name      = var.project_name
      db_username       = var.db_username
      db_password       = var.db_password
      jwt_secret        = var.jwt_secret
      admin_email       = var.admin_email
      admin_password    = var.admin_password
      backend_port      = var.backend_port
      notification_port = var.notification_port
      frontend_port     = var.frontend_port
    })
  }

  # Allow the VM to call GCP APIs (e.g., Cloud Logging, Cloud Storage)
  service_account {
    scopes = [
      "https://www.googleapis.com/auth/cloud-platform",
    ]
  }

  # Scheduling: preemptible = cheaper but can be killed any time (not for prod)
  # For free-tier e2-micro, standard scheduling is fine
  scheduling {
    automatic_restart   = true    # restart if GCP kills the VM (rare)
    on_host_maintenance = "MIGRATE"
    preemptible         = false   # keep false — preemptible breaks free tier
  }

  # Ensure firewall rules exist before creating the VM
  depends_on = [
    google_compute_firewall.allow_app,
    google_compute_firewall.allow_ssh,
  ]
}
