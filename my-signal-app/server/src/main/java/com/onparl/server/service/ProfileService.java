package com.onparl.server.service;

import com.onparl.server.model.User;
import com.onparl.server.repository.DynamoDbUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for managing user profiles.
 * Handles profile setup, updates, and validation.
 */
@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    // Validation constants
    private static final int DISPLAY_NAME_MIN_LENGTH = 2;
    private static final int DISPLAY_NAME_MAX_LENGTH = 50;
    private static final int MOOD_MAX_LENGTH = 100;
    private static final int PROFILE_PICTURE_MAX_SIZE = 500_000; // ~500KB Base64

    // Regex for display name: alphanumeric, spaces, basic punctuation, emojis
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{Z}\\p{P}\\p{Emoji}]+$");

    @Autowired
    private DynamoDbUserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * Set up user profile for the first time.
     * 
     * @param userId         User ID
     * @param displayName    Display name (required)
     * @param profilePicture Profile picture Base64 data (optional)
     * @param mood           Mood/status message (optional)
     * @return Updated user object
     * @throws IllegalArgumentException if validation fails
     */
    public User setupProfile(String userId, String displayName, String profilePicture, String mood) {
        logger.info("Setting up profile for user: {}", userId);

        // Find user by userId
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User user = userOpt.get();

        // Validate and set display name (required)
        validateDisplayName(displayName);
        user.setDisplayName(displayName);

        // Upload profile picture to S3 (optional)
        if (profilePicture != null && !profilePicture.trim().isEmpty()) {
            String s3Url = s3Service.uploadProfilePicture(userId, profilePicture);
            user.setProfilePicture(s3Url); // Store S3 URL instead of Base64
        }

        // Validate and set mood (optional)
        if (mood != null && !mood.trim().isEmpty()) {
            validateMood(mood);
            user.setMood(mood);
        }

        // Mark profile as complete
        user.setProfileComplete(true);

        // Save to database
        userRepository.saveUser(user);

        logger.info("Profile setup complete for user: {}", userId);
        return user;
    }

    /**
     * Update user profile fields.
     * All fields are optional - only provided fields will be updated.
     * 
     * @param userId         User ID
     * @param displayName    New display name (optional)
     * @param profilePicture New profile picture (optional)
     * @param mood           New mood (optional)
     * @return Updated user object
     */
    public User updateProfile(String userId, String displayName, String profilePicture, String mood) {
        logger.info("Updating profile for user: {}", userId);

        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User user = userOpt.get();

        // Update display name if provided
        if (displayName != null && !displayName.trim().isEmpty()) {
            validateDisplayName(displayName);
            user.setDisplayName(displayName);
        }

        // Update profile picture if provided
        if (profilePicture != null) {
            if (profilePicture.trim().isEmpty()) {
                // Empty string means remove picture
                if (user.getProfilePicture() != null) {
                    s3Service.deleteProfilePicture(user.getProfilePicture());
                }
                user.setProfilePicture(null);
            } else {
                // Delete old picture if exists
                if (user.getProfilePicture() != null) {
                    s3Service.deleteProfilePicture(user.getProfilePicture());
                }
                // Upload new picture
                String s3Url = s3Service.uploadProfilePicture(userId, profilePicture);
                user.setProfilePicture(s3Url);
            }
        }

        // Update mood if provided
        if (mood != null) {
            if (mood.trim().isEmpty()) {
                // Empty string means remove mood
                user.setMood(null);
            } else {
                validateMood(mood);
                user.setMood(mood);
            }
        }

        userRepository.saveUser(user);

        logger.info("Profile updated for user: {}", userId);
        return user;
    }

    /**
     * Get user profile by userId.
     * 
     * @param userId User ID
     * @return User object
     */
    public Optional<User> getProfile(String userId) {
        return userRepository.findByUserId(userId);
    }

    /**
     * Validate display name.
     * 
     * Rules:
     * - Not null or empty
     * - Between 2-50 characters
     * - Only letters, numbers, spaces, basic punctuation, and emojis
     * 
     * @param displayName Display name to validate
     * @throws IllegalArgumentException if invalid
     */
    private void validateDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name is required");
        }

        String trimmed = displayName.trim();

        if (trimmed.length() < DISPLAY_NAME_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Display name must be at least " + DISPLAY_NAME_MIN_LENGTH + " characters");
        }

        if (trimmed.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Display name must not exceed " + DISPLAY_NAME_MAX_LENGTH + " characters");
        }

        // Check for valid characters (allowing emojis)
        if (!DISPLAY_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Display name contains invalid characters");
        }
    }

    /**
     * Validate profile picture data URI format.
     * Actual validation (size, type) happens in S3Service.
     * 
     * @param profilePicture Base64 profile picture data
     * @throws IllegalArgumentException if invalid
     */
    private void validateProfilePicture(String profilePicture) {
        if (profilePicture == null || profilePicture.trim().isEmpty()) {
            return; // Optional field
        }

        // Basic format check - S3Service will do full validation
        if (!profilePicture.startsWith("data:image/")) {
            throw new IllegalArgumentException(
                    "Profile picture must be a valid Base64 data URI (data:image/...)");
        }
    }

    /**
     * Validate mood/status message.
     * 
     * Rules:
     * - Max 100 characters
     * - Can contain emojis
     * 
     * @param mood Mood to validate
     * @throws IllegalArgumentException if invalid
     */
    private void validateMood(String mood) {
        if (mood == null || mood.trim().isEmpty()) {
            return; // Optional field
        }

        if (mood.length() > MOOD_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Mood must not exceed " + MOOD_MAX_LENGTH + " characters");
        }
    }
}
