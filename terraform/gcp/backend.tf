# =============================================================================
# Terraform State Backend — GCP Cloud Storage (GCS)
# =============================================================================
# What is "state"?
#   Terraform keeps a record of everything it created in a "state file".
#   Storing it in GCS keeps it safe and shareable.
#
# HOW TO USE:
#   Option A (Beginner — local state, simplest to start):
#     Comment out the entire backend "gcs" block below.
#     Terraform saves state locally as terraform.tfstate
#
#   Option B (Recommended — remote state in GCS):
#     1. Create the GCS bucket first (see README.md Step 1 for GCP)
#        gcloud storage buckets create gs://smartprocure-tfstate-YOUR_PROJECT_ID \
#          --location=us-central1 --uniform-bucket-level-access
#     2. Replace YOUR_PROJECT_ID below with your real GCP project ID
#     3. Run: terraform init
# =============================================================================

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  # ── OPTION B: Remote state in GCS (comment out if using local state) ───────
  # backend "gcs" {
  #   bucket = "smartprocure-tfstate-YOUR_PROJECT_ID"   # ← change this
  #   prefix = "gcp/dev"
  # }
}
