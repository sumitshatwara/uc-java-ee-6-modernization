resource "aws_security_group" "this" {
  name_prefix = "${var.name}-db-"
  vpc_id      = var.vpc_id
}
resource "aws_vpc_security_group_ingress_rule" "ecs" {
  security_group_id            = aws_security_group.this.id
  referenced_security_group_id = var.ecs_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}
resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db"
  subnet_ids = var.private_subnet_ids
}
resource "aws_rds_cluster" "this" {
  cluster_identifier          = "${var.name}-db"
  engine                      = "aurora-postgresql"
  database_name               = var.database_name
  port                        = 5432
  master_username             = var.master_username
  manage_master_user_password = true
  storage_encrypted           = true
  db_subnet_group_name        = aws_db_subnet_group.this.name
  vpc_security_group_ids      = [aws_security_group.this.id]
  backup_retention_period     = 7
  deletion_protection         = true
  skip_final_snapshot         = false
  final_snapshot_identifier   = "${var.name}-final"
}
resource "aws_rds_cluster_instance" "this" {
  count                = var.instance_count
  identifier           = "${var.name}-db-${count.index + 1}"
  cluster_identifier   = aws_rds_cluster.this.id
  engine               = aws_rds_cluster.this.engine
  instance_class       = var.instance_class
  db_subnet_group_name = aws_db_subnet_group.this.name
  publicly_accessible  = false
}
