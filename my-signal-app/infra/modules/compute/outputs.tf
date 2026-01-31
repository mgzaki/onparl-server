output "ecr_repository_url" {
  value = aws_ecr_repository.server.repository_url
}

output "alb_dns_name" {
  value = aws_lb.main.dns_name
}
