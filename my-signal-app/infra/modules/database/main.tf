# DynamoDB Tables for Onparl

resource "aws_dynamodb_table" "users" {
  name           = "onparl-users"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "phoneNumber"
    type = "S"
  }

  global_secondary_index {
    name            = "PhoneNumberIndex"
    hash_key        = "phoneNumber"
    projection_type = "ALL"
  }

  tags = {
    Name        = "Onparl Users"
    Environment = "dev"
  }
}

resource "aws_dynamodb_table" "otps" {
  name           = "onparl-otps"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "phoneNumber"

  attribute {
    name = "phoneNumber"
    type = "S"
  }

  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  tags = {
    Name        = "Onparl OTPs"
    Environment = "dev"
  }
}

resource "aws_dynamodb_table" "messages" {
  name           = "onparl-messages"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "recipientId"
  range_key      = "timestamp"

  attribute {
    name = "recipientId"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "N"
  }

  tags = {
    Name        = "Onparl Messages"
    Environment = "dev"
  }
}

resource "aws_dynamodb_table" "identity_keys" {
  name           = "onparl-identity-keys"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  tags = {
    Name        = "Onparl Identity Keys"
    Environment = "dev"
  }
}

resource "aws_dynamodb_table" "pre_keys" {
  name           = "onparl-pre-keys"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "userId"
  range_key      = "keyId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "keyId"
    type = "N"
  }

  tags = {
    Name        = "Onparl PreKeys"
    Environment = "dev"
  }
}

resource "aws_dynamodb_table" "signed_pre_keys" {
  name           = "onparl-signed-pre-keys"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "userId"
  range_key      = "keyId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "keyId"
    type = "N"
  }

  tags = {
    Name        = "Onparl Signed PreKeys"
    Environment = "dev"
  }
}
