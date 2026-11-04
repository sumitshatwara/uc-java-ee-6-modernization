variable "name" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "vpc_cidr" {
  type = string
}
variable "public_subnet_ids" {
  type = list(string)
}
variable "allowed_cidrs" {
  type = list(string)
}
