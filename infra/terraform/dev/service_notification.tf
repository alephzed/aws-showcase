# ---------------------------------------------------------------------------
# Notification service (issue #1): Fargate task definition + service (no ALB).
# Polls the Notification SQS queue and emits a stubbed confirmation.
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "notification" {
  family                   = "${local.name_prefix}-notification"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.service_cpu
  memory                   = var.service_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.notification_task.arn

  container_definitions = jsonencode([
    {
      name      = "notification"
      image     = "${aws_ecr_repository.this["notification"].repository_url}:${var.notification_image_tag}"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "AWS_REGION", value = var.region },
        { name = "NOTIFICATION_QUEUE_URL", value = aws_sqs_queue.notification.id }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.notification.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "notification"
        }
      }
    }
  ])

  tags = {
    Name = "${local.name_prefix}-notification"
  }
}

resource "aws_ecs_service" "notification" {
  name            = "${local.name_prefix}-notification"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.notification.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets = aws_subnet.public[*].id
    # No NAT: tasks need public IPs to reach AWS APIs (SQS).
    assign_public_ip = true
    security_groups  = [aws_security_group.service.id]
  }

  tags = {
    Name = "${local.name_prefix}-notification"
  }
}
