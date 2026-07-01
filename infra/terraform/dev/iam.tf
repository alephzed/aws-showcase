data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# ---------------------------------------------------------------------------
# ECS task EXECUTION role — used by the ECS agent to pull images, write logs,
# and inject the DB secret. Shared by both services.
# ---------------------------------------------------------------------------
resource "aws_iam_role" "ecs_task_execution" {
  name               = "${local.name_prefix}-ecs-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json

  tags = {
    Name = "${local.name_prefix}-ecs-task-execution"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_managed" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Allow the execution role to read the DB secret so it can be injected as a
# container secret at task startup.
data "aws_iam_policy_document" "execution_secret_read" {
  statement {
    sid       = "ReadDbSecret"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.db.arn]
  }
}

resource "aws_iam_role_policy" "ecs_task_execution_secret" {
  name   = "${local.name_prefix}-execution-secret-read"
  role   = aws_iam_role.ecs_task_execution.id
  policy = data.aws_iam_policy_document.execution_secret_read.json
}

# ---------------------------------------------------------------------------
# Per-service task roles (application runtime permissions, least privilege).
# ---------------------------------------------------------------------------

# Scheduling: publish domain events to the custom bus + read its DB secret.
resource "aws_iam_role" "scheduling_task" {
  name               = "${local.name_prefix}-scheduling-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json

  tags = {
    Name = "${local.name_prefix}-scheduling-task"
  }
}

data "aws_iam_policy_document" "scheduling_task" {
  statement {
    sid       = "PutSchedulingEvents"
    actions   = ["events:PutEvents"]
    resources = [aws_cloudwatch_event_bus.scheduling.arn]
  }

  statement {
    sid       = "ReadDbSecret"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.db.arn]
  }
}

resource "aws_iam_role_policy" "scheduling_task" {
  name   = "${local.name_prefix}-scheduling-task-policy"
  role   = aws_iam_role.scheduling_task.id
  policy = data.aws_iam_policy_document.scheduling_task.json
}

# Notification: consume from the notification queue (and its DLQ).
resource "aws_iam_role" "notification_task" {
  name               = "${local.name_prefix}-notification-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json

  tags = {
    Name = "${local.name_prefix}-notification-task"
  }
}

data "aws_iam_policy_document" "notification_task" {
  statement {
    sid = "ConsumeNotificationQueue"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
      "sqs:ChangeMessageVisibility",
    ]
    resources = [
      aws_sqs_queue.notification.arn,
      aws_sqs_queue.notification_dlq.arn,
    ]
  }
}

resource "aws_iam_role_policy" "notification_task" {
  name   = "${local.name_prefix}-notification-task-policy"
  role   = aws_iam_role.notification_task.id
  policy = data.aws_iam_policy_document.notification_task.json
}
