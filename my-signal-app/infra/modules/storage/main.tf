/**
 * S3 Bucket for Profile Pictures
 * 
 * This module creates an S3 bucket for storing user profile pictures.
 * Pictures are publicly readable but only the backend can write/delete.
 */

resource "aws_s3_bucket" "profile_pictures" {
  bucket = "onparl-profile-pictures-${var.environment}"

  tags = {
    Name        = "Onparl Profile Pictures"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# Public access configuration
resource "aws_s3_bucket_public_access_block" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

# Bucket policy for public read access
resource "aws_s3_bucket_policy" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.profile_pictures.arn}/*"
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.profile_pictures]
}

# CORS configuration for web frontend
resource "aws_s3_bucket_cors_configuration" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = var.allowed_origins
    max_age_seconds = 3000
  }
}

# Lifecycle policy to clean up old/orphaned images
resource "aws_s3_bucket_lifecycle_configuration" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  rule {
    id     = "delete-old-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }

  rule {
    id     = "cleanup-incomplete-uploads"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# Enable versioning for backup/recovery
resource "aws_s3_bucket_versioning" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  versioning_configuration {
    status = var.enable_versioning ? "Enabled" : "Suspended"
  }
}

# Server-side encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "profile_pictures" {
  bucket = aws_s3_bucket.profile_pictures.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
