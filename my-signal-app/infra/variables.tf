variable "aws_region" {
  description = "AWS Region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "sender_email" {
  description = "Email address to verify in SES for sending OTPs"
  type        = string
  default     = "mgzaki2406@gmail.com"
}
