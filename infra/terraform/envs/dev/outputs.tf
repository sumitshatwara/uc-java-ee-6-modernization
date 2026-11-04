output "alb_dns_name" {
  value = module.alb.alb_dns_name
}
output "application_url" {
  value = "http://${module.alb.alb_dns_name}/daytrader/"
}
output "database_endpoint" {
  value = module.rds.cluster_endpoint
}
output "database_port" {
  value = module.rds.port
}
output "database_name" {
  value = module.rds.database_name
}
output "master_user_secret_arn" {
  value = module.rds.master_user_secret_arn
}
output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}
