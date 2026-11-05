# =============================================================================
# GCP Outputs — SmartProcure
# =============================================================================
# These values are printed in your terminal after `terraform apply` finishes.
# They tell you exactly how to access your newly created server.
# =============================================================================

output "server_public_ip" {
  description = "Fixed public IP address of your GCP VM. Bookmark this!"
  value       = google_compute_address.app.address
}

output "frontend_url" {
  description = "Open this in your browser to access the SmartProcure frontend."
  value       = "http://${google_compute_address.app.address}:${var.frontend_port}"
}

output "backend_api_url" {
  description = "Base URL for the Spring Boot backend API."
  value       = "http://${google_compute_address.app.address}:${var.backend_port}"
}

output "backend_health_url" {
  description = "Hit this URL to check if the backend is alive."
  value       = "http://${google_compute_address.app.address}:${var.backend_port}/actuator/health"
}

output "notification_health_url" {
  description = "Hit this URL to check if the notification service is alive."
  value       = "http://${google_compute_address.app.address}:${var.notification_port}/actuator/health"
}

output "mailhog_ui_url" {
  description = "Open this to see all emails sent by the app (dev only)."
  value       = "http://${google_compute_address.app.address}:${var.mailhog_ui_port}"
}

output "ssh_command" {
  description = "Run this command to log in to your server via SSH."
  value       = "ssh -i ~/.ssh/id_rsa ${var.ssh_username}@${google_compute_address.app.address}"
}

output "instance_name" {
  description = "GCP Compute Instance name — useful in the GCP Console."
  value       = google_compute_instance.app.name
}

output "instance_zone" {
  description = "GCP zone where the VM was deployed."
  value       = google_compute_instance.app.zone
}

output "machine_type" {
  description = "GCP machine type being used."
  value       = google_compute_instance.app.machine_type
}

output "gcp_project_id" {
  description = "GCP Project ID where resources were deployed."
  value       = var.gcp_project_id
}

output "startup_log_command" {
  description = "SSH into the server and run this to watch startup progress."
  value       = "ssh -i ~/.ssh/id_rsa ${var.ssh_username}@${google_compute_address.app.address} 'sudo journalctl -u google-startup-scripts -f'"
}

output "gcp_console_url" {
  description = "Direct link to this VM in the GCP Console."
  value       = "https://console.cloud.google.com/compute/instances?project=${var.gcp_project_id}"
}
