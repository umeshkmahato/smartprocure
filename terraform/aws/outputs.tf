# =============================================================================
# AWS Outputs — SmartProcure
# =============================================================================
# These values are printed in your terminal after `terraform apply` finishes.
# They tell you exactly how to access your newly created server.
# =============================================================================

output "server_public_ip" {
  description = "Fixed public IP address of your EC2 server. Bookmark this!"
  value       = aws_eip.app.public_ip
}

output "frontend_url" {
  description = "Open this in your browser to access the SmartProcure frontend."
  value       = "http://${aws_eip.app.public_ip}:${var.frontend_port}"
}

output "backend_api_url" {
  description = "Base URL for the Spring Boot backend API."
  value       = "http://${aws_eip.app.public_ip}:${var.backend_port}"
}

output "backend_health_url" {
  description = "Hit this URL to check if the backend is alive."
  value       = "http://${aws_eip.app.public_ip}:${var.backend_port}/actuator/health"
}

output "notification_health_url" {
  description = "Hit this URL to check if the notification service is alive."
  value       = "http://${aws_eip.app.public_ip}:${var.notification_port}/actuator/health"
}

output "mailhog_ui_url" {
  description = "Open this to see all emails sent by the app (dev only)."
  value       = "http://${aws_eip.app.public_ip}:${var.mailhog_ui_port}"
}

output "ssh_command" {
  description = "Run this command to log in to your server via SSH."
  value       = "ssh -i ~/.ssh/id_rsa ubuntu@${aws_eip.app.public_ip}"
}

output "instance_id" {
  description = "EC2 Instance ID — useful for the AWS console."
  value       = aws_instance.app.id
}

output "instance_type" {
  description = "EC2 instance type being used."
  value       = aws_instance.app.instance_type
}

output "aws_region" {
  description = "AWS region where everything was deployed."
  value       = var.aws_region
}

output "startup_log_command" {
  description = "SSH into the server and run this to watch the startup progress."
  value       = "ssh -i ~/.ssh/id_rsa ubuntu@${aws_eip.app.public_ip} 'sudo tail -f /var/log/userdata.log'"
}
