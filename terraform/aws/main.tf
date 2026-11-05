# =============================================================================
# SmartProcure — AWS Infrastructure (Free-Tier Safe)
# =============================================================================
# What this file creates:
#   1. Provider  → tells Terraform to use AWS
#   2. VPC       → your private network in the cloud
#   3. Subnet    → a section of that network (public = reachable from internet)
#   4. IGW       → Internet Gateway — the "front door" to the internet
#   5. Route Table → rules for where traffic goes
#   6. Security Group → firewall rules (what ports are open)
#   7. Key Pair  → your SSH public key uploaded to AWS
#   8. EC2 Instance → the actual virtual machine (t3.micro = free tier)
#   9. Elastic IP → a fixed public IP address for your server
# =============================================================================

# ── 1. Provider ───────────────────────────────────────────────────────────────
# Tells Terraform: "Talk to AWS in this region"
provider "aws" {
  region = var.aws_region

  # Every resource will get these tags automatically
  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# ── 2. Find the latest Ubuntu 22.04 AMI ───────────────────────────────────────
# AMI = Amazon Machine Image (the OS template for your VM)
# This data source automatically finds the newest Ubuntu 22.04 image
# so you don't have to look up AMI IDs manually.
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical (official Ubuntu publisher)

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ── 3. VPC — Virtual Private Cloud ────────────────────────────────────────────
# Think of this as your own private section of the AWS network.
# Nobody else can access resources inside here unless you allow it.
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr       # 10.0.0.0/16 = 65,536 IP addresses
  enable_dns_hostnames = true               # lets instances get DNS names
  enable_dns_support   = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

# ── 4. Internet Gateway ───────────────────────────────────────────────────────
# Without this, your VPC is completely isolated from the internet.
# This is the "front door" that connects your VPC to the outside world.
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

# ── 5. Public Subnet ──────────────────────────────────────────────────────────
# A subnet is a smaller network inside your VPC.
# "Public" means resources here can get a public IP and be reached from internet.
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidr  # 10.0.1.0/24 = 256 addresses
  availability_zone       = "${var.aws_region}a"    # e.g. us-east-1a
  map_public_ip_on_launch = true                    # auto-assign public IPs

  tags = {
    Name = "${var.project_name}-public-subnet"
  }
}

# ── 6. Route Table ────────────────────────────────────────────────────────────
# Rules that tell your subnet: "send all traffic (0.0.0.0/0) to the internet gateway"
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"                      # all traffic
    gateway_id = aws_internet_gateway.main.id      # goes to internet
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

# Connect the route table to the public subnet
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# ── 7. Security Group — Firewall Rules ────────────────────────────────────────
# Controls what traffic can reach your EC2 instance.
# ingress = incoming traffic (from internet to your server)
# egress  = outgoing traffic (from your server to internet)
resource "aws_security_group" "app" {
  name        = "${var.project_name}-sg"
  description = "SmartProcure application firewall rules"
  vpc_id      = aws_vpc.main.id

  # SSH — to log in to the server remotely
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  # HTTP — standard web traffic
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS — secure web traffic
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Backend API (Spring Boot)
  ingress {
    description = "Backend API"
    from_port   = var.backend_port
    to_port     = var.backend_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Notification Service (Spring Boot)
  ingress {
    description = "Notification Service"
    from_port   = var.notification_port
    to_port     = var.notification_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Frontend (React / Nginx)
  ingress {
    description = "Frontend"
    from_port   = var.frontend_port
    to_port     = var.frontend_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # MailHog — dev email UI (see caught emails here)
  ingress {
    description = "MailHog Web UI"
    from_port   = var.mailhog_ui_port
    to_port     = var.mailhog_ui_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Grafana — observability dashboards
  ingress {
    description = "Grafana"
    from_port   = 3001
    to_port     = 3001
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow ALL outbound traffic (server can reach internet freely)
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"           # -1 means all protocols
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-sg"
  }
}

# ── 8. SSH Key Pair ───────────────────────────────────────────────────────────
# Uploads your public key to AWS so you can SSH into the EC2 instance.
# Your PRIVATE key (~/.ssh/id_rsa) stays on your machine — never share it!
resource "aws_key_pair" "main" {
  key_name   = "${var.project_name}-key"
  public_key = file(pathexpand(var.ssh_public_key_path))

  tags = {
    Name = "${var.project_name}-keypair"
  }
}

# ── 9. EC2 Instance — The Virtual Machine ─────────────────────────────────────
# This is your actual cloud server where everything runs.
# t3.micro is FREE TIER eligible (750 hours/month for first 12 months).
resource "aws_instance" "app" {
  ami                    = data.aws_ami.ubuntu.id          # Ubuntu 22.04
  instance_type          = var.instance_type               # t3.micro (free)
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.app.id]
  key_name               = aws_key_pair.main.key_name

  # Disk: 30 GB is the free-tier limit — gp3 is the cheapest SSD type
  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size_gb
    delete_on_termination = true # deletes disk when instance is terminated

    tags = {
      Name = "${var.project_name}-disk"
    }
  }

  # Startup script: runs ONCE when the instance first boots
  # It installs Docker and starts the SmartProcure stack
  # templatefile() reads the script and substitutes ${variable} placeholders
  user_data = templatefile("${path.module}/scripts/userdata.sh", {
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

  # Ensures the internet gateway exists before the instance starts
  depends_on = [aws_internet_gateway.main]

  tags = {
    Name = "${var.project_name}-server"
  }
}

# ── 10. Elastic IP — Fixed Public IP Address ──────────────────────────────────
# Without this, your server gets a NEW random IP every time it restarts.
# Elastic IP gives your server a permanent address.
# Cost: FREE when attached to a running instance. $0.005/hr when unattached.
resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"

  tags = {
    Name = "${var.project_name}-eip"
  }

  # Must wait for internet gateway before assigning EIP
  depends_on = [aws_internet_gateway.main]
}
