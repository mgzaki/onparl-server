package com.onparl.server.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * OTP Verification entity for phone authentication.
 * 
 * Stores One-Time Password verification data with expiration.
 * OTPs are hashed (never stored in plaintext) and automatically expire after 5
 * minutes.
 * 
 * DynamoDB Table: onparl-otp-verifications
 * - Partition Key: phoneNumber
 * - TTL: expiresAt (DynamoDB auto-deletes expired records)
 * 
 * SECURITY NOTES:
 * - OTPs are stored as SHA-256 hashes, never plaintext
 * - Each OTP can only be used once (deleted after successful verification)
 * - Maximum 3 verification attempts per OTP
 * - Rate limited: 1 OTP per phone number per 60 seconds
 */
@DynamoDbBean
public class OtpVerification {

    /**
     * Phone number in E.164 format (partition key).
     */
    private String phoneNumber;

    /**
     * SHA-256 hash of the OTP code.
     * Format: SHA256(otp + salt)
     * NEVER store the actual OTP in plaintext!
     */
    private String otpHash;

    /**
     * Salt used for hashing the OTP.
     * A random salt is generated for each OTP to prevent rainbow table attacks.
     */
    private String salt;

    /**
     * Unix timestamp (milliseconds) when this OTP expires.
     * Default: 5 minutes from creation.
     * DynamoDB TTL will auto-delete this record after expiration.
     */
    private long expiresAt;

    /**
     * Number of failed verification attempts for this OTP.
     * Max allowed: 3 attempts.
     * After 3 failed attempts, the OTP is invalidated.
     */
    private int attempts;

    /**
     * Unix timestamp (milliseconds) when this OTP was created.
     * Used for rate limiting (prevent requesting multiple OTPs too quickly).
     */
    private long createdAt;

    /**
     * Whether this OTP has been used successfully.
     * Once verified, the OTP is marked as used and should be deleted.
     */
    private boolean used;

    public OtpVerification() {
        // Default constructor required by DynamoDB mapper
    }

    /**
     * Get the phone number (partition key).
     */
    @DynamoDbPartitionKey
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    /**
     * Check if this OTP is expired.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Check if maximum attempts have been reached.
     */
    public boolean isMaxAttemptsReached() {
        return attempts >= 3;
    }
}
