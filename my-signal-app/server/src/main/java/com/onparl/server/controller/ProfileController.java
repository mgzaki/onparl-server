package com.onparl.server.controller;

import com.onparl.server.model.User;
import com.onparl.server.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for user profile management.
 * 
 * Endpoints:
 * - POST /api/profile/setup - Initial profile setup for new users
 * - PUT /api/profile/update - Update existing profile
 * - GET /api/profile/{userId} - Get user profile
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileService profileService;

    /**
     * Set up user profile for the first time.
     * 
     * Required fields: userId, displayName
     * Optional fields: profilePicture, mood
     * 
     * @param request Profile setup request
     * @return Success response with user object
     */
    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setupProfile(@RequestBody ProfileSetupRequest request) {
        try {
            logger.info("Profile setup request for user: {}", request.getUserId());

            User user = profileService.setupProfile(
                    request.getUserId(),
                    request.getDisplayName(),
                    request.getProfilePicture(),
                    request.getMood());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", buildUserResponse(user));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Profile setup validation failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Profile setup failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update user profile.
     * 
     * All fields are optional - only provided fields will be updated.
     * 
     * @param request Profile update request
     * @return Success response with updated user object
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        try {
            logger.info("Profile update request for user: {}", request.getUserId());

            User user = profileService.updateProfile(
                    request.getUserId(),
                    request.getDisplayName(),
                    request.getProfilePicture(),
                    request.getMood());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", buildUserResponse(user));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Profile update validation failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("Profile update failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get user profile by userId.
     * 
     * @param userId User ID
     * @return User profile
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String userId) {
        try {
            logger.info("Profile request for user: {}", userId);

            Optional<User> userOpt = profileService.getProfile(userId);

            if (userOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", buildUserResponse(userOpt.get()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get profile", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Build user response object with relevant fields.
     * Masks sensitive information like full phone number.
     */
    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("profilePicture", user.getProfilePicture());
        userMap.put("mood", user.getMood());
        userMap.put("profileComplete", user.isProfileComplete());
        userMap.put("phoneNumber", maskPhoneNumber(user.getPhoneNumber()));
        return userMap;
    }

    /**
     * Mask phone number for privacy (show only last 4 digits).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4).replaceAll(".", "*")
                + phoneNumber.substring(phoneNumber.length() - 4);
    }

    // Request DTOs (Data Transfer Objects)

    public static class ProfileSetupRequest {
        private String userId;
        private String displayName;
        private String profilePicture;
        private String mood;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getProfilePicture() {
            return profilePicture;
        }

        public void setProfilePicture(String profilePicture) {
            this.profilePicture = profilePicture;
        }

        public String getMood() {
            return mood;
        }

        public void setMood(String mood) {
            this.mood = mood;
        }
    }

    public static class ProfileUpdateRequest {
        private String userId;
        private String displayName;
        private String profilePicture;
        private String mood;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getProfilePicture() {
            return profilePicture;
        }

        public void setProfilePicture(String profilePicture) {
            this.profilePicture = profilePicture;
        }

        public String getMood() {
            return mood;
        }

        public void setMood(String mood) {
            this.mood = mood;
        }
    }
}
