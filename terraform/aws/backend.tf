# =============================================================================
# Terraform State Backend — AWS S3 + DynamoDB
# =============================================================================
# What is "state"?
#   Terraform keeps a record of everything it created in a "state file".
#   By default it saves this locally (terraform.tfstate).
#   Using S3 means it's safe, backed up, and shareable with teammates.
#
# HOW TO USE:
#   Option A (Beginner — local state, simplest):
#     Comment out the entire backend "s3" block below.
#     Terraform will store state in a local file. Fine for learning.
#
#   Option B (Recommended — remote state in S3):
#     1. Create the S3 bucket and DynamoDB table (see README.md Step 1)
#     2. Replace YOUR_ACCOUNT_ID below with your real AWS account ID
#        Run: aws sts get-caller-identity --query Account --output text
#     3. Uncomment the block and run: terraform init
# =============================================================================

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # ── OPTION B: Remote state (comment out if using local state) ─────────────
  # backend "s3" {
  #   bucket         = "smartprocure-tfstate-YOUR_ACCOUNT_ID"
  #   key            = "aws/dev/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true                              # encrypts the state file
  #   dynamodb_table = "smartprocure-tfstate-lock"      # prevents simultaneous edits
  # }
}
