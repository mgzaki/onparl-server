package com.onparl.server.service;

import com.onparl.server.model.User;
import com.onparl.server.repository.DynamoDbUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Authentication service that orchestrates the phone-based authentication flow.
 * 
 * AUTHENTICATION FLOW:
 * 1. User requests OTP for their phone number
 * 2. OTP is generated and sent via SMS
 * 3. User submits OTP for verification
 * 4. If valid, user is authenticated and userId is returned
 * 5. New users are automatically registered, returning users are logged in
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final OtpService otpService;
    private final SmsService smsService;
    private final DynamoDbUserRepository userRepository;

    public AuthService(OtpService otpService, SmsService smsService, DynamoDbUserRepository userRepository) {
        this.otpService = otpService;
        this.smsService = smsService;
        this.userRepository = userRepository;
    }

    /**
     * Request an OTP to be sent to the phone number.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @throws IllegalArgumentException if phone number is invalid
     * @throws IllegalStateException    if rate limit is exceeded
     */
    public void requestOtp(String phoneNumber) {
        // Validate phone number format
        if (!smsService.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Generate OTP (throws exception if rate limited)
        String otp = otpService.generateOtp(phoneNumber);

        // Send OTP via SMS
        smsService.sendOtp(phoneNumber, otp);

        logger.info("OTP requested for phone: {}", maskPhoneNumber(phoneNumber));
    }

    /**
     * Verify OTP and authenticate the user.
     * 
     * For new users: Creates a user account
     * For existing users: Updates last login time
     * 
     * @param phoneNumber Phone number in E.164 format
     * @param otp         The OTP code to verify
     * @return The authenticated User object
     * @throws IllegalArgumentException if OTP is invalid or expired
     */
    public User verifyOtpAndLogin(String phoneNumber, String otp) {
        // Verify the OTP
        boolean isValid = otpService.verifyOtp(phoneNumber, otp);

        if (!isValid) {
            int remainingAttempts = otpService.getRemainingAttempts(phoneNumber);
            logger.warn("Invalid OTP attempt for phone: {}. Remaining attempts: {}",
                    maskPhoneNumber(phoneNumber), remainingAttempts);
            throw new IllegalArgumentException("Invalid or expired OTP. Attempts remaining: " + remainingAttempts);
        }

        // Get or create user
        User user = getOrCreateUser(phoneNumber);

        // Update last login time
        user.setLastLoginAt(System.currentTimeMillis());
        user.setVerified(true);
        userRepository.saveUser(user);

        logger.info("User authenticated successfully: {}", user.getUserId());

        return user;
    }

    /**
     * Get an existing user or create a new one.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return User object (existing or newly created)
     */
    public User getOrCreateUser(String phoneNumber) {
        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);

        if (existingUser.isPresent()) {
            logger.info("Returning user login: {}", existingUser.get().getUserId());
            return existingUser.get();
        }

        // Create new user
        User newUser = new User();
        newUser.setPhoneNumber(phoneNumber);
        newUser.setUserId(generateUserId());
        newUser.setVerified(false);
        newUser.setCreatedAt(System.currentTimeMillis());
        newUser.setLastLoginAt(System.currentTimeMillis());
        newUser.setProfileComplete(false); // Profile setup required

        userRepository.saveUser(newUser);

        logger.info("New user registered: {}", newUser.getUserId());

        return newUser;
    }

    /**
     * Get a user by their userId.
     * 
     * @param userId The user ID
     * @return User object if found
     */
    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    /**
     * Get a user by their phone number.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return User object if found
     */
    public Optional<User> getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    /**
     * Invalidate any existing OTP for a phone number.
     * Useful for logout or when user abandons authentication.
     * 
     * @param phoneNumber Phone number in E.164 format
     */
    public void invalidateOtp(String phoneNumber) {
        otpService.invalidateOtp(phoneNumber);
        logger.info("OTP invalidated for phone: {}", maskPhoneNumber(phoneNumber));
    }

    /**
     * Get time remaining until OTP expiry.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return Seconds until expiry, or 0 if no OTP exists
     */
    public long getOtpExpirySeconds(String phoneNumber) {
        return otpService.getSecondsUntilExpiry(phoneNumber);
    }

    /**
     * Generate a unique user ID.
     * Format: "user_" + UUID
     */
    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Mask a phone number for logging.
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }

        int visibleDigits = 7;
        String visible = phoneNumber.substring(0, Math.min(phoneNumber.length(), visibleDigits));
        return visible + "****";
    }
}
