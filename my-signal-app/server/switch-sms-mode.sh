#!/bin/bash

# Quick script to switch between mock mode and production SNS
# Usage: ./switch-sms-mode.sh [mock|production]

MODE=$1

if [ -z "$MODE" ]; then
  echo "Usage: ./switch-sms-mode.sh [mock|production]"
  echo ""
  echo "Current mode: $(grep 'sms.mock.enabled' src/main/resources/application.properties | cut -d'=' -f2)"
  exit 1
fi

if [ "$MODE" == "mock" ]; then
  echo "Switching to MOCK MODE (OTPs printed to console)..."
  sed -i 's/sms.mock.enabled=.*/sms.mock.enabled=true/' src/main/resources/application.properties
  echo "✅ Mock mode enabled. OTPs will be printed to console."
  echo "   Restart the server for changes to take effect."
  
elif [ "$MODE" == "production" ]; then
  echo "Switching to PRODUCTION MODE (real SMS via AWS SNS)..."
  
  # Check if AWS credentials are set
  if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "⚠️  WARNING: AWS credentials not found in environment!"
    echo "   Please set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY"
    echo "   Or source .env.production file"
    exit 1
  fi
  
  sed -i 's/sms.mock.enabled=.*/sms.mock.enabled=false/' src/main/resources/application.properties
  echo "✅ Production mode enabled. OTPs will be sent via AWS SNS."
  echo "   Restart the server for changes to take effect."
  
else
  echo "Invalid mode: $MODE"
  echo "Use 'mock' or 'production'"
  exit 1
fi
