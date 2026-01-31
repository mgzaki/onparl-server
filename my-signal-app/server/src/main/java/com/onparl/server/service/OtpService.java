package com.onparl.server.service;

import com.onparl.server.model.OtpVerification;
import com.onparl.server.repository.DynamoDbOtpRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

/**
 * Service for OTP (One-Time Password) generation and verification.
 * 
 * SECURITY FEATURES:
 * - Generates cryptographically secure random 6-digit codes
 * - Stores hashed OTPs (SHA-256 with salt), never plaintext
 * - 5-minute expiration window
 * - Maximum 3 verification attempts per OTP
 * - Rate limiting: 1 OTP per phone per 60 seconds
 * - Automatic cleanup via DynamoDB TTL
 */
@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    private static final int MAX_ATTEMPTS = 3;
    private static final long RATE_LIMIT_MS = 60 * 1000; // 60 seconds

    private final DynamoDbOtpRepository otpRepository;
    private final SecureRandom secureRandom;

    public OtpService(DynamoDbOtpRepository otpRepository) {
        this.otpRepository = otpRepository;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a new OTP for the given phone number.
     * 
     * Rate Limiting: Will throw exception if called too frequently for the same
     * phone.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return The generated 6-digit OTP (to be sent via SMS)
     * @throws IllegalStateException if rate limit is exceeded
     */
    public String generateOtp(String phoneNumber) {
        // Check rate limit
        Optional<OtpVerification> existing = otpRepository.findByPhoneNumber(phoneNumber);
        if (existing.isPresent()) {
            long timeSinceCreation = System.currentTimeMillis() - existing.get().getCreatedAt();
            if (timeSinceCreation < RATE_LIMIT_MS) {
                long remainingSeconds = (RATE_LIMIT_MS - timeSinceCreation) / 1000;
                throw new IllegalStateException(
                        "Please wait " + remainingSeconds + " seconds before requesting another OTP");
            }
        }

        // Generate 6-digit OTP
        String otp = generateRandomOtp();

        // Generate random salt for hashing
        String salt = generateRandomSalt();

        // Hash the OTP with salt
        String otpHash = hashOtp(otp, salt);

        // Create OTP verification record
        OtpVerification verification = new OtpVerification();
        verification.setPhoneNumber(phoneNumber);
        verification.setOtpHash(otpHash);
        verification.setSalt(salt);
        verification.setCreatedAt(System.currentTimeMillis());
        verification.setExpiresAt(System.currentTimeMillis() + OTP_EXPIRY_MS);
        verification.setAttempts(0);
        verification.setUsed(false);

        // Save to DynamoDB
        otpRepository.saveOtp(verification);

        return otp;
    }

    /**
     * Verify an OTP for the given phone number.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @param otp         The OTP code to verify
     * @return true if OTP is valid, false otherwise
     */
    public boolean verifyOtp(String phoneNumber, String otp) {
        Optional<OtpVerification> verificationOpt = otpRepository.findByPhoneNumber(phoneNumber);

        if (verificationOpt.isEmpty()) {
            return false;
        }

        OtpVerification verification = verificationOpt.get();

        // Check if expired
        if (verification.isExpired()) {
            otpRepository.deleteByPhoneNumber(phoneNumber);
            return false;
        }

        // Check if already used
        if (verification.isUsed()) {
            return false;
        }

        // Check if max attempts reached
        if (verification.isMaxAttemptsReached()) {
            otpRepository.deleteByPhoneNumber(phoneNumber);
            return false;
        }

        // Verify the OTP
        String otpHash = hashOtp(otp, verification.getSalt());
        boolean isValid = otpHash.equals(verification.getOtpHash());

        if (isValid) {
            // Mark as used and delete
            otpRepository.deleteByPhoneNumber(phoneNumber);
        } else {
            // Increment failed attempts
            otpRepository.incrementAttempts(phoneNumber);
        }

        return isValid;
    }

    /**
     * Invalidate (delete) an OTP for a phone number.
     * Useful for logout or when user requests a new OTP.
     */
    public void invalidateOtp(String phoneNumber) {
        otpRepository.deleteByPhoneNumber(phoneNumber);
    }

    /**
     * Get remaining attempts for an OTP.
     */
    public int getRemainingAttempts(String phoneNumber) {
        return otpRepository.findByPhoneNumber(phoneNumber)
                .map(otp -> Math.max(0, MAX_ATTEMPTS - otp.getAttempts()))
                .orElse(0);
    }

    /**
     * Get time until OTP expiry in seconds.
     */
    public long getSecondsUntilExpiry(String phoneNumber) {
        return otpRepository.findByPhoneNumber(phoneNumber)
                .map(otp -> Math.max(0, (otp.getExpiresAt() - System.currentTimeMillis()) / 1000))
                .orElse(0L);
    }

    /**
     * Generate a cryptographically secure random 6-digit OTP.
     */
    private String generateRandomOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // Range: 100000-999999
        return String.valueOf(otp);
    }

    /**
     * Generate a random salt for OTP hashing.
     */
    private String generateRandomSalt() {
        byte[] saltBytes = new byte[16];
        secureRandom.nextBytes(saltBytes);
        return DigestUtils.sha256Hex(saltBytes);
    }

    /**
     * Hash an OTP with salt using SHA-256.
     * 
     * @param otp  The plaintext OTP
     * @param salt The salt for hashing
     * @return SHA-256 hash as hex string
     */
    private String hashOtp(String otp, String salt) {
        return DigestUtils.sha256Hex(otp + salt);
    }
}
