variable "name" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "private_subnet_ids" {
  type = list(string)
}
variable "ecs_security_group_id" {
  type = string
}
variable "database_name" {
  type    = string
  default = "daytrader"
}
variable "master_username" {
  type    = string
  default = "daytrader"
}
variable "instance_class" {
  type    = string
  default = "db.t4g.medium"
}
variable "instance_count" {
  type    = number
  default = 1
}
