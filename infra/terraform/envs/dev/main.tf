provider "aws" {
  region = var.region
  default_tags {
    tags = { Project = var.name, Environment = "dev" }
  }
}
module "network" {
  source               = "../../modules/network"
  name                 = var.name
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
}
module "alb" {
  source            = "../../modules/alb"
  name              = var.name
  vpc_id            = module.network.vpc_id
  vpc_cidr          = var.vpc_cidr
  public_subnet_ids = module.network.public_subnet_ids
  allowed_cidrs     = var.allowed_cidrs
}
module "rds" {
  source                = "../../modules/rds"
  name                  = var.name
  vpc_id                = module.network.vpc_id
  private_subnet_ids    = module.network.private_subnet_ids
  ecs_security_group_id = module.ecs.security_group_id
  database_name         = var.database_name
  instance_class        = var.database_instance_class
  instance_count        = var.database_instance_count
}
module "ecs" {
  source                 = "../../modules/ecs"
  name                   = var.name
  vpc_id                 = module.network.vpc_id
  private_subnet_ids     = module.network.private_subnet_ids
  alb_security_group_id  = module.alb.alb_security_group_id
  target_group_arn       = module.alb.target_group_arn
  image                  = var.image
  database_endpoint      = module.rds.cluster_endpoint
  database_name          = module.rds.database_name
  master_user_secret_arn = module.rds.master_user_secret_arn
  cpu                    = var.cpu
  memory                 = var.memory
  desired_count          = var.desired_count
  depends_on             = [module.alb]
}
