variable "region" {
  type    = string
  default = "us-east-1"
}
variable "name" {
  type    = string
  default = "daytrader-dev"
}
variable "image" {
  type    = string
  default = "registry.example.com/daytrader-boot:replace-with-immutable-tag"
}
variable "vpc_cidr" {
  type    = string
  default = "10.42.0.0/16"
}
variable "availability_zones" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b"]
}
variable "public_subnet_cidrs" {
  type    = list(string)
  default = ["10.42.0.0/24", "10.42.1.0/24"]
}
variable "private_subnet_cidrs" {
  type    = list(string)
  default = ["10.42.10.0/24", "10.42.11.0/24"]
}
variable "allowed_cidrs" {
  type    = list(string)
  default = ["192.0.2.0/24"]
}
variable "database_name" {
  type    = string
  default = "daytrader"
}
variable "database_instance_class" {
  type    = string
  default = "db.t4g.medium"
}
variable "database_instance_count" {
  type    = number
  default = 1
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
