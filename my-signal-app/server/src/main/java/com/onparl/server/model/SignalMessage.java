package com.onparl.server.model;

/**
 * SIGNAL MESSAGE - Data Transfer Object (DTO) for API requests/responses.
 * 
 * DTO PATTERN:
 * This is a "plain old Java object" (POJO) used to transfer data between:
 * - Client → Server (HTTP POST /api/messages)
 * - Server → Client (HTTP GET /api/messages/{userId}, WebSocket push)
 * 
 * DIFFERENCE FROM ENTITY:
 * - DTO: Used for API communication (no database annotations)
 * - Entity: Used for database storage (has @DynamoDbBean annotations)
 * - Controllers receive DTOs, convert to Entities for storage
 * 
 * JSON SERIALIZATION:
 * Spring automatically converts this to/from JSON:
 * {
 * "senderId": "alice",
 * "recipientId": "bob",
 * "content": "base64_encrypted_blob", // ← THE ENCRYPTED PAYLOAD!
 * "timestamp": 1234567890
 * }
 * 
 * ENCRYPTION NOTE:
 * The "content" field contains a Base64-encoded encrypted message.
 * It was encrypted by the sender using the Double Ratchet algorithm.
 * The server never decrypts it - only stores and forwards it!
 */
public class SignalMessage {
    private String senderId; // Who sent this message (e.g., "alice")
    private String recipientId; // Who should receive it (e.g., "bob")
    private String content; // Base64 encoded ENCRYPTED content (Double Ratchet ciphertext)
    private long timestamp; // Unix timestamp (milliseconds since epoch)

    /**
     * Default constructor - required for JSON deserialization.
     * Jackson (JSON library) uses reflection to create instances.
     */
    public SignalMessage() {
    }

    /**
     * All-args constructor for easy object creation.
     * Used in MessageController when converting Entity → DTO.
     */
    public SignalMessage(String senderId, String recipientId, String content, long timestamp) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // =================================================================================
    // Getters and Setters - required for JSON serialization/deserialization
    // =================================================================================

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Get the encrypted message content.
     * This is a Base64-encoded string representing encrypted bytes from Double
     * Ratchet.
     * Contains: ciphertext, message key, chain key metadata, etc.
     */
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Get the message timestamp (Unix epoch milliseconds).
     * Assigned by the sender's client when encrypting the message.
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
