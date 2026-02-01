package com.onparl.server.service;

import com.onparl.server.model.User;
import com.onparl.server.repository.DynamoDbUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Authentication service that orchestrates the authentication flow.
 * Supports both Phone (SMS) and Email authentication.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final OtpService otpService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final DynamoDbUserRepository userRepository;

    public AuthService(OtpService otpService, SmsService smsService, EmailService emailService,
            DynamoDbUserRepository userRepository) {
        this.otpService = otpService;
        this.smsService = smsService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    /**
     * Request an OTP to be sent to the identifier (phone or email).
     * 
     * @param identifier Phone number or Email address
     * @param type       "PHONE" or "EMAIL"
     */
    public void requestOtp(String identifier, String type) {
        if ("PHONE".equalsIgnoreCase(type)) {
            // Validate phone number
            if (!smsService.isValidPhoneNumber(identifier)) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
            // Generate & Send OTP
            String otp = otpService.generateOtp(identifier);
            smsService.sendOtp(identifier, otp);
            logger.info("OTP requested for phone: {}", maskIdentifier(identifier));

        } else if ("EMAIL".equalsIgnoreCase(type)) {
            // Validate email
            if (!emailService.isValidEmail(identifier)) {
                throw new IllegalArgumentException("Invalid email format");
            }
            // Generate & Send OTP
            String otp = otpService.generateOtp(identifier);
            emailService.sendOtp(identifier, otp);
            logger.info("OTP requested for email: {}", identifier);

        } else {
            throw new IllegalArgumentException("Invalid authentication type: " + type);
        }
    }

    /**
     * Verify OTP and authenticate the user.
     */
    public User verifyOtpAndLogin(String identifier, String otp, String type) {
        // Verify the OTP
        boolean isValid = otpService.verifyOtp(identifier, otp);

        if (!isValid) {
            int remainingAttempts = otpService.getRemainingAttempts(identifier);
            logger.warn("Invalid OTP attempt for: {}. Remaining attempts: {}",
                    maskIdentifier(identifier), remainingAttempts);
            throw new IllegalArgumentException("Invalid or expired OTP. Attempts remaining: " + remainingAttempts);
        }

        // Get or create user
        User user = getOrCreateUser(identifier, type);

        // Update last login time
        user.setLastLoginAt(System.currentTimeMillis());
        user.setVerified(true);
        userRepository.saveUser(user);

        logger.info("User authenticated successfully: {}", user.getUserId());

        return user;
    }

    /**
     * Get an existing user or create a new one.
     */
    public User getOrCreateUser(String identifier, String type) {
        Optional<User> existingUser = userRepository.findById(identifier);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create new user
        User newUser = new User();
        newUser.setId(identifier); // Partition Key

        if ("PHONE".equalsIgnoreCase(type)) {
            newUser.setPhoneNumber(identifier);
        } else {
            newUser.setEmail(identifier);
        }

        newUser.setUserId(generateUserId());
        newUser.setVerified(false);
        newUser.setCreatedAt(System.currentTimeMillis());
        newUser.setLastLoginAt(System.currentTimeMillis());
        newUser.setProfileComplete(false);

        userRepository.saveUser(newUser);

        logger.info("New user registered: {}", newUser.getUserId());

        return newUser;
    }

    /**
     * Dev Login: Bypass OTP and log in directly.
     * Only for testing purposes.
     */
    public User devLogin(String identifier, String type) {
        logger.warn("DEV LOGIN requested for: {}", identifier);

        // Get or create user
        User user = getOrCreateUser(identifier, type);

        // Update last login time
        user.setLastLoginAt(System.currentTimeMillis());
        // Auto-verify in dev mode
        user.setVerified(true);
        userRepository.saveUser(user);

        return user;
    }

    /**
     * Get a user by their userId.
     */
    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    /**
     * Get a user by their identifier (phone or email).
     */
    public Optional<User> getUserByIdentifier(String identifier) {
        return userRepository.findById(identifier);
    }

    /**
     * Invalidate any existing OTP.
     */
    public void invalidateOtp(String identifier) {
        otpService.invalidateOtp(identifier);
    }

    /**
     * Get time remaining until OTP expiry.
     */
    public long getOtpExpirySeconds(String identifier) {
        return otpService.getSecondsUntilExpiry(identifier);
    }

    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "****";
        }
        return identifier.substring(0, Math.min(identifier.length(), 3)) + "****";
    }
}
