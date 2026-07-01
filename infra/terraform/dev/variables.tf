variable "region" {
  description = "AWS region for the dev baseline."
  type        = string
  default     = "us-east-2"
}

variable "environment" {
  description = "Environment name (used in tags and resource names)."
  type        = string
  default     = "dev"
}

variable "project" {
  description = "Project name prefix for resources."
  type        = string
  default     = "aws-showcase"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for the two public subnets (one per AZ)."
  type        = list(string)
  default     = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "db_name" {
  description = "Initial PostgreSQL database name."
  type        = string
  default     = "scheduling"
}

variable "db_username" {
  description = "Master username for the PostgreSQL instance."
  type        = string
  default     = "scheduling_admin"
}

variable "db_engine_version" {
  description = "PostgreSQL engine version."
  type        = string
  default     = "16.4"
}

variable "log_retention_days" {
  description = "CloudWatch log group retention in days."
  type        = number
  default     = 7
}

locals {
  name_prefix = "${var.project}-${var.environment}"
}
