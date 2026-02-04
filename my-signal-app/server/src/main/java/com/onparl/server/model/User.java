package com.onparl.server.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity for phone-based authentication.
 * 
 * Stores registered users with their phone numbers and generated user IDs.
 * After successful OTP verification, a User record is created or updated.
 * 
 * DynamoDB Table: onparl-users
 * - Partition Key: phoneNumber
 * - GSI: userId-index (for userId lookups)
 */
@DynamoDbBean
public class User {

    /**
     * Unique identifier (Partition Key).
     * Can be either a phone number (e.g., +1415...) or an email address.
     */
    private String id;

    /**
     * Phone number in E.164 format (optional if using email).
     */
    private String phoneNumber;

    /**
     * Email address (optional if using phone).
     */
    private String email;

    /**
     * Unique user ID generated upon first registration.
     * Format: "user_" + UUID.
     * Used for Signal Protocol operations and chat identification.
     */
    private String userId;

    /**
     * Whether the phone number has been verified via OTP.
     * Set to true after successful OTP verification.
     */
    private boolean verified;

    /**
     * Unix timestamp (milliseconds) when the user first registered.
     */
    private long createdAt;

    /**
     * Unix timestamp (milliseconds) of the last successful login.
     * Updated on each OTP verification.
     */
    private long lastLoginAt;

    /**
     * User's display name.
     * Required during profile setup, can be updated later.
     * Max length: 50 characters.
     */
    private String displayName;

    /**
     * User's profile picture.
     * Stored as Base64 encoded image data (for simplicity).
     * Format: "data:image/png;base64,..."
     * Can be migrated to S3 URL later for production.
     * Max size: ~400KB when Base64 encoded.
     */
    private String profilePicture;

    /**
     * User's mood/status message.
     * Short text displayed alongside user profile.
     * Max length: 100 characters.
     * Can include emojis.
     */
    private String mood;

    /**
     * Flag indicating whether the user has completed profile setup.
     * Set to true after user provides at least a display name.
     * New users must complete profile setup before accessing chat.
     */
    private boolean profileComplete;

    /**
     * Set of User IDs that this user has added as contacts.
     */
    private Set<String> contacts = new HashSet<>();

    /**
     * Set of User IDs that this user has blocked.
     */
    private Set<String> blockedUsers = new HashSet<>();

    public User() {
        // Default constructor required by DynamoDB mapper
    }

    /**
     * Get the unique identifier (partition key).
     */
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Get the user ID (GSI partition key for userId-index).
     * This allows looking up users by userId.
     */
    @DynamoDbSecondaryPartitionKey(indexNames = "userId-index")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }

    @DynamoDbAttribute("contacts")
    public Set<String> getContacts() {
        return (contacts == null || contacts.isEmpty()) ? null : contacts;
    }

    public void setContacts(Set<String> contacts) {
        this.contacts = (contacts != null) ? contacts : new HashSet<>();
    }

    @DynamoDbAttribute("blockedUsers")
    public Set<String> getBlockedUsers() {
        return (blockedUsers == null || blockedUsers.isEmpty()) ? null : blockedUsers;
    }

    public void setBlockedUsers(Set<String> blockedUsers) {
        this.blockedUsers = (blockedUsers != null) ? blockedUsers : new HashSet<>();
    }
}
