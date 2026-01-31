output "web_client_url" {
  value = aws_cloudfront_distribution.web_client.domain_name
}

output "s3_bucket_name" {
  value = aws_s3_bucket.web_client.id
}

output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.web_client.id
}
