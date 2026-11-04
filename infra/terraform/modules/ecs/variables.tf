variable "name" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "private_subnet_ids" {
  type = list(string)
}
variable "alb_security_group_id" {
  type = string
}
variable "target_group_arn" {
  type = string
}
variable "image" {
  type = string
}
variable "database_endpoint" {
  type = string
}
variable "database_name" {
  type = string
}
variable "master_user_secret_arn" {
  type = string
}
variable "secret_kms_key_arn" {
  type    = string
  default = null
}
variable "cpu" {
  type    = number
  default = 512
}
variable "memory" {
  type    = number
  default = 1024
}
variable "desired_count" {
  type    = number
  default = 1
}
variable "log_retention_days" {
  type    = number
  default = 14
}
