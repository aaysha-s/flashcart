resource "aws_lb" "main" {
  name               = "${var.project}-alb"
  load_balancer_type = "application"
  internal           = false
  subnets            = local.public_subnets
  security_groups    = [aws_security_group.alb.id]
}

resource "aws_lb_target_group" "gateway" {
  name        = "${var.project}-gateway"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip" # Fargate tasks register by IP
  vpc_id      = data.aws_vpc.default.id

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  deregistration_delay = 10
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway.id
  }
}