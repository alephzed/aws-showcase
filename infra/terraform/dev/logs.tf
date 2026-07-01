resource "aws_cloudwatch_log_group" "scheduling" {
  name              = "/ecs/${local.name_prefix}/scheduling"
  retention_in_days = var.log_retention_days

  tags = {
    Name = "${local.name_prefix}-scheduling-logs"
  }
}

resource "aws_cloudwatch_log_group" "notification" {
  name              = "/ecs/${local.name_prefix}/notification"
  retention_in_days = var.log_retention_days

  tags = {
    Name = "${local.name_prefix}-notification-logs"
  }
}
