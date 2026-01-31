variable "aws_region" {}
variable "vpc_id" {}
variable "public_subnets" { type = list(string) }
variable "private_subnets" { type = list(string) }
variable "alb_sg_id" {}
variable "ecs_tasks_sg_id" {}
variable "database_arns" { type = list(string) }
variable "s3_bucket_arn" { type = string }
