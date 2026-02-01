terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "database" {
  source = "./modules/database"
}

module "storage" {
  source = "./modules/storage"

  environment       = var.environment
  allowed_origins   = ["*"]  # Restrict to your domain in production
  enable_versioning = false
}

module "networking" {
  source     = "./modules/networking"
  aws_region = var.aws_region
}

module "ses" {
  source       = "./modules/ses"
  sender_email = var.sender_email
}

module "compute" {
  source = "./modules/compute"

  aws_region       = var.aws_region
  vpc_id           = module.networking.vpc_id
  public_subnets   = module.networking.public_subnets
  private_subnets  = module.networking.private_subnets
  alb_sg_id        = module.networking.alb_sg_id
  ecs_tasks_sg_id  = module.networking.ecs_tasks_sg_id
  database_arns    = [
    module.database.users_table_arn,
    module.database.otps_table_arn,
    module.database.messages_table_arn,
    module.database.identity_keys_table_arn,
    module.database.pre_keys_table_arn,
    module.database.signed_pre_keys_table_arn
  ]
  s3_bucket_arn    = module.storage.bucket_arn
}

module "frontend" {
  source       = "./modules/frontend"
  alb_dns_name = module.compute.alb_dns_name
}
