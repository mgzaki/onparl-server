#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGION="${AWS_REGION:-us-east-1}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo -e "${BLUE}=== 🚀 Starting Onparl Deployment ===${NC}"
echo -e "${YELLOW}Environment: $ENVIRONMENT${NC}"
echo -e "${YELLOW}Region: $REGION${NC}"
echo ""

# 1. Apply Infrastructure
echo -e "${BLUE}--- 🏗️  Deploying Infrastructure (OpenTofu) ---${NC}"
cd "$SCRIPT_DIR"

# Check if OpenTofu is installed
if ! command -v tofu &> /dev/null; then
    echo -e "${RED}❌ OpenTofu not found. Please install OpenTofu first.${NC}"
    exit 1
fi

# Always initialize to ensure modules are installed
echo "  📦 Initializing OpenTofu..."
tofu init -upgrade

# Plan and Apply
echo "  📋 Creating execution plan..."
tofu plan -var="environment=$ENVIRONMENT" -out=tfplan

echo "  🔨 Applying infrastructure changes..."
tofu apply tfplan
rm tfplan

# Extract Outputs
echo -e "\n${BLUE}--- 📊 Extracting Infrastructure Outputs ---${NC}"
ECR_REPO=$(tofu output -raw ecr_repository_url 2>/dev/null || echo "")
API_URL=$(tofu output -raw server_api_url 2>/dev/null || echo "")
WEB_URL=$(tofu output -raw web_client_url 2>/dev/null || echo "")
S3_BUCKET=$(tofu output -raw s3_bucket_name 2>/dev/null || echo "")
S3_PROFILE_BUCKET=$(tofu output -raw profile_pictures_bucket_name 2>/dev/null || echo "onparl-profile-pictures-$ENVIRONMENT")
ALB_DNS=$(tofu output -raw alb_dns_name 2>/dev/null || echo "")

echo -e "  ${GREEN}✓${NC} ECR Repository:        $ECR_REPO"
echo -e "  ${GREEN}✓${NC} API URL:               $API_URL"
echo -e "  ${GREEN}✓${NC} Web Client URL:        $WEB_URL"
echo -e "  ${GREEN}✓${NC} S3 Bucket (Frontend):  $S3_BUCKET"
echo -e "  ${GREEN}✓${NC} S3 Bucket (Profiles):  $S3_PROFILE_BUCKET"
echo -e "  ${GREEN}✓${NC} ALB DNS:               $ALB_DNS"

# 2. Deploy Server (if ECR exists - production deployment)
if [ -n "$ECR_REPO" ]; then
    echo -e "\n${BLUE}--- 🐳 Deploying Server to ECS ---${NC}"
    cd "$ROOT_DIR/server"

    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
        exit 1
    fi

    # Login to ECR
    echo "  🔐 Logging into ECR..."
    aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$ECR_REPO"

    # Build Docker image
    echo "  🔨 Building Docker image..."
    docker build -t onparl-server:$ENVIRONMENT .
    docker tag onparl-server:$ENVIRONMENT "$ECR_REPO:latest"
    docker tag onparl-server:$ENVIRONMENT "$ECR_REPO:$ENVIRONMENT"

    # Push to ECR
    echo "  ⬆️  Pushing to ECR..."
    docker push "$ECR_REPO:latest"
    docker push "$ECR_REPO:$ENVIRONMENT"

    # Update ECS Service
    echo "  🔄 Updating ECS Service..."
    CLUSTER_NAME="onparl-cluster"
    SERVICE_NAME="onparl-server-service"
    
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --force-new-deployment \
        --region "$REGION" > /dev/null
    
    echo -e "  ${GREEN}✓${NC} Server deployed to ECS"
else
    echo -e "\n${YELLOW}⚠️  Skipping ECS deployment (running locally)${NC}"
    echo -e "  To run server locally:"
    echo -e "    cd $ROOT_DIR/server"
    echo -e "    ./gradlew bootRun"
fi

# 3. Deploy Web Client (if S3 bucket exists)
if [ -n "$S3_BUCKET" ]; then
    echo -e "\n${BLUE}--- ⚛️  Deploying Web Client to S3 ---${NC}"
    cd "$ROOT_DIR/web-client"

    # Update API URL in config
    CONFIG_FILE="src/utils/SignalManager.ts"
    if [ -n "$WEB_URL" ]; then
        echo "  📝 Updating $CONFIG_FILE with API URL..."
        # Use WEB_URL (CloudFront) to avoid mixed content issues
        sed -i.bak "s|export const SERVER_URL = '.*';|export const SERVER_URL = '$WEB_URL';|" "$CONFIG_FILE"
        rm -f "$CONFIG_FILE.bak"
    fi

    # Install dependencies
    echo "  📦 Installing dependencies..."
    npm install --silent

    # Build React app
    echo "  🔨 Building React app..."
    npm run build

    # Sync to S3
    echo "  ⬆️  Syncing to S3..."
    aws s3 sync dist/ "s3://$S3_BUCKET" --delete --region "$REGION"

    # Invalidate CloudFront cache
    CLOUDFRONT_ID=$(cd "$SCRIPT_DIR" && tofu output -raw cloudfront_distribution_id 2>/dev/null || echo "")
    if [ -n "$CLOUDFRONT_ID" ]; then
        echo "  🔄 Invalidating CloudFront cache..."
        aws cloudfront create-invalidation \
            --distribution-id "$CLOUDFRONT_ID" \
            --paths "/*" > /dev/null
        echo -e "  ${GREEN}✓${NC} Web client deployed to CloudFront"
    else
        echo -e "  ${GREEN}✓${NC} Web client deployed to S3"
    fi
else
    echo -e "\n${YELLOW}⚠️  Skipping web deployment (running locally)${NC}"
    echo -e "  To run web client locally:"
    echo -e "    cd $ROOT_DIR/web-client"
    echo -e "    npm run dev"
fi

# 4. Verify DynamoDB Tables
echo -e "\n${BLUE}--- 🗄️  Verifying DynamoDB Tables ---${NC}"
TABLES=("onparl-users" "onparl-otps" "onparl-messages" "onparl-identity-keys" "onparl-pre-keys" "onparl-signed-pre-keys")

for table in "${TABLES[@]}"; do
    if aws dynamodb describe-table --table-name "$table" --region "$REGION" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} $table"
    else
        echo -e "  ${YELLOW}⚠${NC}  $table (not found - may need manual creation)"
    fi
done

# 5. Verify S3 Profile Pictures Bucket
echo -e "\n${BLUE}--- 🖼️  Verifying S3 Profile Pictures Bucket ---${NC}"
if aws s3 ls "s3://$S3_PROFILE_BUCKET" --region "$REGION" > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} $S3_PROFILE_BUCKET"
    
    # Check bucket policy
    if aws s3api get-bucket-policy --bucket "$S3_PROFILE_BUCKET" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} Public read policy configured"
    else
        echo -e "  ${YELLOW}⚠${NC}  No bucket policy (images may not be publicly accessible)"
    fi
else
    echo -e "  ${RED}❌${NC} $S3_PROFILE_BUCKET not found"
fi

# 6. Display Environment Info
echo -e "\n${BLUE}=== 📋 Deployment Summary ===${NC}"
echo ""
echo -e "${GREEN}✅ Deployment Complete!${NC}"
echo ""
echo -e "Environment:     ${YELLOW}$ENVIRONMENT${NC}"
echo -e "Region:          ${YELLOW}$REGION${NC}"
echo ""

if [ -n "$WEB_URL" ]; then
    echo -e "🌐 Web Application:  ${GREEN}$WEB_URL${NC}"
fi

if [ -n "$API_URL" ]; then
    echo -e "🔌 API Endpoint:     ${GREEN}$API_URL${NC}"
fi

if [ -n "$ALB_DNS" ]; then
    echo -e "⚖️  Load Balancer:    ${GREEN}$ALB_DNS${NC}"
fi

echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo -e "  1. Test phone authentication at your web URL"
echo -e "  2. Upload a profile picture to test S3 integration"
echo -e "  3. Send encrypted messages to test Signal Protocol"
echo -e "  4. Monitor CloudWatch logs for any issues"
echo ""
echo -e "${YELLOW}💡 Tip: Set SMS_MOCK_ENABLED=false in production to send real SMS${NC}"
echo ""
