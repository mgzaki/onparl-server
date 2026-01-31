# AWS S3 Storage Module for Profile Pictures

This module creates and configures an S3 bucket for storing user profile pictures.

## Features

- **Public read access** for profile pictures
- **CORS configuration** for web frontend access
- **Lifecycle policies** to clean up old versions and incomplete uploads
- **Optional versioning** for backup/recovery
- **Server-side encryption** (AES256)
- **Environment-specific naming** (dev, staging, prod)

## Usage

```hcl
module "storage" {
  source = "./modules/storage"

  environment      = var.environment
  allowed_origins  = ["https://yourdomain.com"]  # CORS origins
  enable_versioning = false  # Set to true for prod
}
```

## IAM Permissions Needed

Your ECS task role or EC2 instance profile needs these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:PutObjectAcl",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::onparl-profile-pictures-*/*"
    }
  ]
}
```

## Outputs

- `bucket_name` - The name of the S3 bucket
- `bucket_arn` - The ARN of the S3 bucket
- `bucket_domain_name` - The domain name for accessing files
- `bucket_regional_domain_name` - The regional domain name

## Security Considerations

1. **Public Read Access**: Profile pictures are publicly readable by design
2. **Write Access**: Only backend (via IAM role) can upload/delete
3. **CORS**: Restrict `allowed_origins` to your domain in production
4. **Encryption**: All objects are encrypted at rest (AES256)
