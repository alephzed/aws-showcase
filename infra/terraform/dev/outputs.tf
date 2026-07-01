# --- Networking ---
output "vpc_id" {
  description = "VPC id."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet ids (one per AZ)."
  value       = aws_subnet.public[*].id
}

output "alb_security_group_id" {
  description = "ALB security group id."
  value       = aws_security_group.alb.id
}

output "service_security_group_id" {
  description = "Fargate service security group id."
  value       = aws_security_group.service.id
}

output "rds_security_group_id" {
  description = "RDS security group id."
  value       = aws_security_group.rds.id
}

# --- Load balancer ---
output "alb_dns_name" {
  description = "Public DNS name of the ALB."
  value       = aws_lb.main.dns_name
}

output "alb_arn" {
  description = "ALB ARN."
  value       = aws_lb.main.arn
}

output "alb_listener_arn" {
  description = "HTTP listener ARN (issue #1 attaches its service rule here)."
  value       = aws_lb_listener.http.arn
}

output "scheduling_target_group_arn" {
  description = "Placeholder target group ARN for the Scheduling service."
  value       = aws_lb_target_group.scheduling.arn
}

# --- ECR ---
output "ecr_repository_urls" {
  description = "ECR repository URLs keyed by service."
  value       = { for k, r in aws_ecr_repository.this : k => r.repository_url }
}

# --- ECS ---
output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  description = "ECS cluster ARN."
  value       = aws_ecs_cluster.main.arn
}

output "ecs_task_execution_role_arn" {
  description = "Shared ECS task execution role ARN."
  value       = aws_iam_role.ecs_task_execution.arn
}

output "scheduling_task_role_arn" {
  description = "Scheduling service task role ARN."
  value       = aws_iam_role.scheduling_task.arn
}

output "notification_task_role_arn" {
  description = "Notification service task role ARN."
  value       = aws_iam_role.notification_task.arn
}

output "scheduling_log_group_name" {
  description = "CloudWatch log group for the Scheduling service."
  value       = aws_cloudwatch_log_group.scheduling.name
}

output "notification_log_group_name" {
  description = "CloudWatch log group for the Notification service."
  value       = aws_cloudwatch_log_group.notification.name
}

# --- EventBridge ---
output "eventbridge_bus_name" {
  description = "Custom EventBridge bus name."
  value       = aws_cloudwatch_event_bus.scheduling.name
}

output "eventbridge_bus_arn" {
  description = "Custom EventBridge bus ARN."
  value       = aws_cloudwatch_event_bus.scheduling.arn
}

# --- SQS ---
output "notification_queue_url" {
  description = "Notification SQS queue URL."
  value       = aws_sqs_queue.notification.id
}

output "notification_queue_arn" {
  description = "Notification SQS queue ARN."
  value       = aws_sqs_queue.notification.arn
}

output "notification_dlq_url" {
  description = "Notification DLQ URL."
  value       = aws_sqs_queue.notification_dlq.id
}

output "notification_dlq_arn" {
  description = "Notification DLQ ARN."
  value       = aws_sqs_queue.notification_dlq.arn
}

# --- RDS / Secrets ---
output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint (host:port)."
  value       = aws_db_instance.main.endpoint
}

output "rds_address" {
  description = "RDS PostgreSQL host address."
  value       = aws_db_instance.main.address
}

output "db_secret_arn" {
  description = "Secrets Manager ARN holding DB connection credentials."
  value       = aws_secretsmanager_secret.db.arn
}
