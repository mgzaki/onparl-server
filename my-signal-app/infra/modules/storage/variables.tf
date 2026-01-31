variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "allowed_origins" {
  description = "List of allowed CORS origins for S3 bucket"
  type        = list(string)
  default     = ["*"]  # Restrict in production to your domain
}

variable "enable_versioning" {
  description = "Enable versioning for profile pictures bucket"
  type        = bool
  default     = false
}
