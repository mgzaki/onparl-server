# SMS Mode Configuration Guide

## Current Mode: MOCK (Development)

Your system is configured to **print OTPs to the console** instead of sending real SMS messages.

---

## How It Works

The system uses a single flag to control SMS behavior:

```properties
sms.mock.enabled=true   # Mock mode: prints to console
sms.mock.enabled=false  # Production: sends via AWS SNS
```

---

## Quick Mode Switch

### Option 1: Using the Helper Script

```bash
cd /home/msamory/Documents/onparl_server/onparl-server/my-signal-app/server

# Switch to mock mode
./switch-sms-mode.sh mock

# Switch to production mode (requires AWS credentials)
./switch-sms-mode.sh production

# Check current mode
./switch-sms-mode.sh
```

### Option 2: Manual Configuration

Edit `src/main/resources/application.properties`:

```properties
# For mock mode (development)
sms.mock.enabled=true

# For production (real SMS)
sms.mock.enabled=false
```

Then restart the server.

---

## Setting Up AWS Credentials (for Production)

### Step 1: Get AWS Credentials

1. Log in to AWS Console
2. Go to IAM → Users → Your User
3. Create access key for "Application running outside AWS"
4. Copy `Access Key ID` and `Secret Access Key`

### Step 2: Configure Credentials

#### Option A: Environment Variables (Recommended)

```bash
export AWS_ACCESS_KEY_ID="your_actual_access_key"
export AWS_SECRET_ACCESS_KEY="your_actual_secret_key"
export AWS_REGION="us-east-1"
```

#### Option B: Use .env.production File

1. Copy the template:
   ```bash
   cp .env.production.template .env.production
   ```

2. Edit `.env.production` with your real credentials:
   ```bash
   AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
   AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   AWS_REGION=us-east-1
   SMS_MOCK_ENABLED=false
   ```

3. Source it before running:
   ```bash
   source .env.production
   ./gradlew bootRun
   ```

### Step 3: IAM Permissions Required

Your AWS user needs this permission:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## Testing

### Mock Mode Test

1. Start server: `./gradlew bootRun`
2. Request OTP from frontend
3. Check console for output:
   ```
   ========================================
   📱 SMS MOCK MODE
   To: +14155552671
   Message: Your Onparl verification code is: 123456
   ...
   ========================================
   ```

### Production Mode Test

1. Switch to production mode
2. Set AWS credentials
3. Restart server
4. Request OTP with YOUR phone number
5. Check your phone for SMS

---

## Cost Monitoring

When using production mode:

- **US SMS:** ~$0.00645 per message
- **Monitor costs** in AWS Cost Explorer
- **Set billing alerts** at $10, $25, $50

---

## Security Best Practices

✅ **DO:**
- Use environment variables for credentials
- Keep `.env.production` in `.gitignore`
- Use IAM roles when deployed on EC2/ECS
- Rotate credentials periodically

❌ **DON'T:**
- Commit credentials to git
- Share access keys
- Use root account credentials
- Hard-code credentials in application.properties

---

## Current Configuration

**Mode:** Mock (Development)  
**Credentials:** Not required (mock mode)  
**AWS SNS:** Configured but not active  
**Cost:** $0 (no real SMS sent)

**To switch to production:**
1. Set AWS credentials (environment variables or .env.production)
2. Run: `./switch-sms-mode.sh production`
3. Restart server
4. Test with your phone number first!
