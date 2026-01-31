output "users_table_arn" {
  description = "ARN of the users table"
  value       = aws_dynamodb_table.users.arn
}

output "users_table_name" {
  description = "Name of the users table"
  value       = aws_dynamodb_table.users.name
}

output "otps_table_arn" {
  description = "ARN of the OTPs table"
  value       = aws_dynamodb_table.otps.arn
}

output "otps_table_name" {
  description = "Name of the OTPs table"
  value       = aws_dynamodb_table.otps.name
}

output "messages_table_arn" {
  description = "ARN of the messages table"
  value       = aws_dynamodb_table.messages.arn
}

output "messages_table_name" {
  description = "Name of the messages table"
  value       = aws_dynamodb_table.messages.name
}

output "identity_keys_table_arn" {
  description = "ARN of the identity keys table"
  value       = aws_dynamodb_table.identity_keys.arn
}

output "pre_keys_table_arn" {
  description = "ARN of the pre keys table"
  value       = aws_dynamodb_table.pre_keys.arn
}

output "signed_pre_keys_table_arn" {
  description = "ARN of the signed pre keys table"
  value       = aws_dynamodb_table.signed_pre_keys.arn
}
