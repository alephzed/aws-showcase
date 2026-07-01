# ---------------------------------------------------------------------------
# Scheduling service (issue #1): Fargate task definition + service behind the ALB.
# DB credentials injected from Secrets Manager; publishes to the EventBridge bus.
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "scheduling" {
  family                   = "${local.name_prefix}-scheduling"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.service_cpu
  memory                   = var.service_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.scheduling_task.arn

  container_definitions = jsonencode([
    {
      name      = "scheduling"
      image     = "${aws_ecr_repository.this["scheduling"].repository_url}:${var.scheduling_image_tag}"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "AWS_REGION", value = var.region },
        { name = "EVENTBRIDGE_BUS_NAME", value = aws_cloudwatch_event_bus.scheduling.name }
      ]

      # DB credentials sourced from the Secrets Manager JSON (never in the image).
      secrets = [
        { name = "SPRING_DATASOURCE_URL", valueFrom = "${aws_secretsmanager_secret.db.arn}:jdbc_url::" },
        { name = "SPRING_DATASOURCE_USERNAME", valueFrom = "${aws_secretsmanager_secret.db.arn}:username::" },
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = "${aws_secretsmanager_secret.db.arn}:password::" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.scheduling.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "scheduling"
        }
      }
    }
  ])

  tags = {
    Name = "${local.name_prefix}-scheduling"
  }
}

resource "aws_ecs_service" "scheduling" {
  name            = "${local.name_prefix}-scheduling"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.scheduling.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
    base              = 0
  }

  network_configuration {
    subnets = aws_subnet.public[*].id
    # No NAT: tasks need public IPs to pull from ECR and reach AWS APIs.
    assign_public_ip = true
    security_groups  = [aws_security_group.service.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.scheduling.arn
    container_name   = "scheduling"
    container_port   = 8080
  }

  health_check_grace_period_seconds = 120

  # Ensure the listener/target group exist before wiring the service.
  depends_on = [aws_lb_listener.http]

  tags = {
    Name = "${local.name_prefix}-scheduling"
  }
}
