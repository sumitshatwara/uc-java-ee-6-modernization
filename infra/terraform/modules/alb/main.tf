resource "aws_security_group" "this" {
  name_prefix = "${var.name}-alb-"
  vpc_id      = var.vpc_id
}
resource "aws_vpc_security_group_ingress_rule" "http" {
  for_each          = toset(var.allowed_cidrs)
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = each.value
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}
resource "aws_vpc_security_group_egress_rule" "app" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = var.vpc_cidr
  ip_protocol       = "tcp"
  from_port         = 8080
  to_port           = 8080
}
resource "aws_lb" "this" {
  name               = "${var.name}-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = var.public_subnet_ids
  security_groups    = [aws_security_group.this.id]
}
resource "aws_lb_target_group" "this" {
  name        = "${var.name}-app"
  vpc_id      = var.vpc_id
  target_type = "ip"
  port        = 8080
  protocol    = "HTTP"
  health_check {
    path    = "/daytrader/actuator/health"
    matcher = "200"
  }
}
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}
resource "aws_lb_listener_rule" "rest" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
  condition {
    path_pattern { values = ["/rest/*"] }
  }
}
