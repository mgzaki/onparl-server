# Onparl Server

End-to-end encrypted messaging server using Signal Protocol (X3DH) with phone number authentication.

## Features

### 🔐 Phone Number Authentication with OTP/SMS
- Secure phone-based login with one-time passwords
- SMS delivery via AWS SNS (configurable mock mode)
- OTP security: SHA-256 hashing, 5-minute expiration, rate limiting
- Auto-registration for new users

### 🔒 End-to-End Encryption  
- Signal Protocol (X3DH) implementation
- Forward secrecy with rotating prekeys
- WebSocket-based real-time messaging
- No server-side message decryption

### 📱 Authentication Flow
1. User enters phone number (+country code)
2. Server generates and sends 6-digit OTP via SMS
3. User verifies OTP (max 3 attempts)
4. Signal Protocol initializes automatically
5. Secure messaging begins

## Quick Start

### Prerequisites
- Java 21+
- Gradle
- AWS account (for production SMS)
- DynamoDB Local or AWS DynamoDB

### Development Mode (Mock SMS)

```bash
# Start the server
./gradlew bootRun

# Server runs on http://localhost:8080
# OTPs printed to console (no real SMS sent)
```

**Test Authentication:**
1. Frontend: http://localhost:5173/
2. Enter phone: `+14155552671` (any +1 number)
3. Check console for OTP: `Your verification code is: 123456`
4. Enter code and start chatting!

### Production Mode (Real SMS)

#### 1. Set AWS Credentials

```bash
export AWS_ACCESS_KEY_ID="your_access_key"
export AWS_SECRET_ACCESS_KEY="your_secret_key"
export AWS_REGION="us-east-1"
```

Or use `.env.production`:
```bash
cp .env.production.template .env.production
# Edit with real credentials
source .env.production
```

#### 2. Switch to Production Mode

```bash
./switch-sms-mode.sh production
./gradlew bootRun
```

#### 3. AWS SNS Setup

Required IAM permissions:
```json
{
  "Effect": "Allow",
  "Action": ["sns:Publish"],
  "Resource": "*"
}
```

**Important:** Request SMS spending limit increase via AWS Support (default: $1/month)

## Configuration

### SMS Mode Toggle

**Mock Mode** (Development - Current Default):
```properties
sms.mock.enabled=true  # OTPs printed to console
```

**Production Mode** (Real SMS):
```properties
sms.mock.enabled=false  # OTPs sent via AWS SNS
```

**Quick Switch:**
```bash
./switch-sms-mode.sh mock       # Switch to mock mode
./switch-sms-mode.sh production # Switch to production mode
```

### OTP Configuration

Edit `src/main/resources/application.properties`:

```properties
# OTP Settings
otp.length=6                    # 6-digit codes
otp.expiry.minutes=5            # 5-minute validity
otp.max.attempts=3              # 3 verification attempts
otp.rate.limit.seconds=60       # 1 OTP per minute per phone

# SMS Settings
sms.sender.id=Onparl            # SMS sender name
sms.mock.enabled=true           # Mock mode on/off
```

## API Endpoints

### Authentication

#### POST /api/auth/request-otp
Request OTP to be sent via SMS.

**Request:**
```json
{
  "phoneNumber": "+14155552671"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "OTP sent to +1415555****",
  "expiresIn": 300
}
```

**Errors:**
- `400` - Invalid phone number format
- `429` - Rate limit exceeded (wait 60s)
- `500` - SMS delivery failure

#### POST /api/auth/verify-otp
Verify OTP and authenticate user.

**Request:**
```json
{
  "phoneNumber": "+14155552671",
  "otp": "123456"
}
```

**Response (200):**
```json
{
  "success": true,
  "userId": "user_abc123def456",
  "phoneNumber": "+14155552671",
  "isNewUser": false
}
```

**Errors:**
- `401` - Invalid or expired OTP
- `400` - Missing fields

#### POST /api/auth/resend-otp
Resend OTP (same as request-otp, subject to rate limiting).

### Signal Protocol Endpoints

- `POST /api/keys/prekeys` - Upload prekey bundle
- `GET /api/keys/bundle/{userId}` - Get user's key bundle for encryption
- `POST /api/messages` - Send encrypted message
- `WS /ws-signal` - WebSocket for real-time message delivery

## Database Tables (DynamoDB)

Auto-created on first startup:

### Authentication
- **onparl-users** - User accounts (partition key: phoneNumber)
- **onparl-otp-verifications** - OTP codes with TTL (partition key: phoneNumber)

### Signal Protocol
- **onparl-identity-keys** - Long-term identity keys
- **onparl-pre-keys** - One-time prekeys
- **onparl-signed-pre-keys** - Medium-term signed prekeys
- **onparl-messages** - Encrypted message storage

## Security Features

### OTP Security
- ✅ SHA-256 hashed with random salt (never stored plaintext)
- ✅ 5-minute expiration window
- ✅ Maximum 3 verification attempts
- ✅ Rate limiting: 1 OTP per phone per 60 seconds
- ✅ Auto-deletion after successful verification
- ✅ DynamoDB TTL for automatic cleanup

### Phone Number Validation
- ✅ E.164 format required (+[country][number])
- ✅ libphonenumber validation library
- ✅ Client and server-side validation
- ✅ Masked in logs for privacy (+1415555****)

### Signal Protocol
- ✅ X3DH key agreement
- ✅ Double Ratchet encryption
- ✅ Forward secrecy
- ✅ Prekey rotation

## Cost Estimates (Production)

### AWS SNS SMS Pricing (US)
- **Per SMS:** ~$0.00645
- **Expected:** 100 users/day × 1.5 OTPs = 150 SMS/day
- **Monthly:** ~$29 for 4,500 SMS

### DynamoDB
- **On-Demand:** ~$0.10/month (negligible)

**Total:** ~$30/month

## Troubleshooting

### OTP Not Received (Mock Mode)
✅ Check console for `📱 SMS MOCK MODE` output  
✅ Verify `sms.mock.enabled=true` in application.properties  
✅ Look for rate limiting errors in logs

### OTP Not Received (Production)
✅ Verify AWS credentials are set  
✅ Check AWS SNS spending limits  
✅ Validate phone number format (E.164)  
✅ Check CloudWatch logs for SNS errors  
✅ Test with different carrier (some block AWS SMS)

### Invalid OTP Error
✅ Check OTP hasn't expired (5 minutes)  
✅ Verify no extra spaces in input  
✅ Try requesting new OTP  
✅ Check remaining attempts (max 3)

### Build Issues
```bash
# Clean and rebuild
./gradlew clean build

# Skip tests
./gradlew build -x test
```

## Project Structure

```
server/
├── src/main/java/com/example/onparl/server/
│   ├── controller/
│   │   ├── AuthController.java          # Auth API endpoints
│   │   ├── KeyController.java           # Signal Protocol keys
│   │   └── MessageController.java       # Message sending
│   ├── model/
│   │   ├── User.java                    # User entity
│   │   ├── OtpVerification.java         # OTP entity
│   │   └── KeyEntities.java             # Signal Protocol keys
│   ├── repository/
│   │   ├── DynamoDbUserRepository.java  # User data access
│   │   └── DynamoDbOtpRepository.java   # OTP data access
│   ├── service/
│   │   ├── AuthService.java             # Auth orchestration
│   │   ├── OtpService.java              # OTP generation/verification
│   │   └── SmsService.java              # SMS sending
│   └── config/
│       ├── TableInitializer.java        # Auto-create DynamoDB tables
│       └── WebSocketConfig.java         # WebSocket configuration
├── .env.development                     # Dev config (mock mode)
├── .env.production.template             # Production template
├── switch-sms-mode.sh                   # Mode switching script
└── SMS_CONFIGURATION.md                 # Detailed SMS guide
```

## Documentation

- 📖 [SMS Configuration Guide](./SMS_CONFIGURATION.md) - Detailed SMS setup
- 📖 [Walkthrough](/.gemini/antigravity/brain/c34462f5-5058-4bcd-a093-48978f8de831/walkthrough.md) - Complete implementation walkthrough

## Development

### Running Tests
```bash
./gradlew test
```

### Building for Production
```bash
./gradlew build
java -jar build/libs/server-0.0.1-SNAPSHOT.jar
```

### Environment Variables
```bash
# AWS Credentials (production)
AWS_ACCESS_KEY_ID=<your-key>
AWS_SECRET_ACCESS_KEY=<your-secret>
AWS_REGION=us-east-1

# Optional Overrides
OTP_EXPIRY_MINUTES=5
OTP_RATE_LIMIT_SECONDS=60
SMS_SENDER_ID=Onparl
```

## Tech Stack

- **Backend:** Spring Boot 3.x, Java 21
- **Database:** AWS DynamoDB
- **SMS:** AWS SNS
- **Encryption:** Signal Protocol (libsignal-client)
- **WebSocket:** STOMP over SockJS
- **Phone Validation:** libphonenumber

## License

MIT

## Support

For issues or questions, see the [walkthrough documentation](/.gemini/antigravity/brain/c34462f5-5058-4bcd-a093-48978f8de831/walkthrough.md) or check the troubleshooting section above.