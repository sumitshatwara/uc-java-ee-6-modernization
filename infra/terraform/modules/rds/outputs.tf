output "cluster_endpoint" {
  value = aws_rds_cluster.this.endpoint
}
output "port" {
  value = aws_rds_cluster.this.port
}
output "database_name" {
  value = aws_rds_cluster.this.database_name
}
output "master_user_secret_arn" {
  value = aws_rds_cluster.this.master_user_secret[0].secret_arn
}
output "security_group_id" {
  value = aws_security_group.this.id
}
