# Public ALB for the Scheduling service. The ECS service itself is created by
# issue #1; here we provision the ALB, a placeholder target group, and an HTTP
# listener, and export their ARNs for #1 to attach to.
resource "aws_lb" "main" {
  name               = "${local.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = {
    Name = "${local.name_prefix}-alb"
  }
}

# Placeholder target group for the Scheduling service (target_type = ip for
# Fargate awsvpc networking). Issue #1 registers tasks against this.
resource "aws_lb_target_group" "scheduling" {
  name        = "${local.name_prefix}-scheduling-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "${local.name_prefix}-scheduling-tg"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.scheduling.arn
  }

  tags = {
    Name = "${local.name_prefix}-http-listener"
  }
}
