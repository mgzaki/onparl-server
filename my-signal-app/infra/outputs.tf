output "ecr_repository_url" {
  description = "URL of the ECR repository"
  value       = module.compute.ecr_repository_url
}

output "server_api_url" {
  description = "URL of the Onparl Server Load Balancer"
  value       = "http://${module.compute.alb_dns_name}"
}

output "web_client_url" {
  description = "URL of the Onparl Web Client (CloudFront)"
  value       = "https://${module.frontend.web_client_url}"
}

output "s3_bucket_name" {
  description = "Name of the S3 bucket for Web Client assets"
  value       = module.frontend.s3_bucket_name
}

output "dynamodb_table_messages" {
  value = module.database.messages_table_name
}

output "cloudfront_distribution_id" {
  description = "ID of the CloudFront distribution"
  value       = module.frontend.cloudfront_distribution_id
}
