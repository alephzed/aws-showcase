# ---------------------------------------------------------------------------
# Event-driven backbone (cost-minimized): custom EventBridge bus, a Notification
# SQS queue with a DLQ redrive, and a rule matching AppointmentBooked -> queue.
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_event_bus" "scheduling" {
  name = "scheduling-bus"

  tags = {
    Name = "${local.name_prefix}-scheduling-bus"
  }
}

resource "aws_sqs_queue" "notification_dlq" {
  name                      = "notification-dlq"
  message_retention_seconds = 1209600 # 14 days

  tags = {
    Name = "${local.name_prefix}-notification-dlq"
  }
}

resource "aws_sqs_queue" "notification" {
  name                       = "notification-queue"
  visibility_timeout_seconds = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.notification_dlq.arn
    maxReceiveCount     = 5
  })

  tags = {
    Name = "${local.name_prefix}-notification-queue"
  }
}

resource "aws_cloudwatch_event_rule" "appointment_booked" {
  name           = "${local.name_prefix}-appointment-booked"
  description    = "Route AppointmentBooked events to the Notification queue."
  event_bus_name = aws_cloudwatch_event_bus.scheduling.name

  event_pattern = jsonencode({
    "detail-type" = ["AppointmentBooked"]
  })
}

resource "aws_cloudwatch_event_target" "notification" {
  rule           = aws_cloudwatch_event_rule.appointment_booked.name
  event_bus_name = aws_cloudwatch_event_bus.scheduling.name
  target_id      = "notification-queue"
  arn            = aws_sqs_queue.notification.arn
}

# Allow EventBridge to send messages to the Notification queue (scoped to the rule).
resource "aws_sqs_queue_policy" "notification" {
  queue_url = aws_sqs_queue.notification.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowEventBridgeSend"
        Effect    = "Allow"
        Principal = { Service = "events.amazonaws.com" }
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.notification.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_cloudwatch_event_rule.appointment_booked.arn
          }
        }
      }
    ]
  })
}
