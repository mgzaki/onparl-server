package com.onparl.server.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * SIGNAL MESSAGE ENTITY - DynamoDB storage model for encrypted messages.
 * 
 * DATABASE SCHEMA DESIGN:
 * Table: "onparl-messages"
 * Partition Key: recipientId (groups all messages for a user together)
 * Sort Key: timestamp (orders messages chronologically)
 * 
 * WHY THIS KEY DESIGN?
 * - Efficient query: "Fetch all messages for Bob" → Query by recipientId
 * - Automatic sorting: Messages automatically ordered by timestamp
 * - Scalable: Each user's messages are in a separate partition (no hot
 * partitions)
 * 
 * QUERY PATTERN:
 * DynamoDB Query (fast): "recipientId = 'bob'" → Returns all Bob's messages,
 * sorted
 * Alternative would be Scan (slow): Search entire table - DON'T DO THIS!
 * 
 * ENTITY vs DTO:
 * - This Entity is for database storage (@DynamoDbBean)
 * - SignalMessage (DTO) is for API communication
 * - Controllers convert between them
 * 
 * ENCRYPTION:
 * The "content" field stores encrypted blob - server has no idea what it says!
 */
@DynamoDbBean // Marks this as a DynamoDB table mapping
public class SignalMessageEntity {

    private String recipientId; // WHO the message is for (partition key)
    private long timestamp; // WHEN it was sent (sort key)
    private String senderId; // WHO sent it (regular attribute)
    private String content; // ENCRYPTED payload (Base64 ciphertext)

    /**
     * Default constructor - required by DynamoDB Enhanced Client.
     * AWS SDK uses reflection to instantiate objects when reading from DB.
     */
    public SignalMessageEntity() {
    }

    /**
     * Partition key - groups all messages for a recipient together.
     * DynamoDB Query: "Give me all items where recipientId = 'bob'"
     * 
     * WHY recipientId and not senderId?
     * - Users need to query "What messages did I receive?" (inbox pattern)
     * - Less common: "What messages did I send?" (outbox pattern)
     * - You optimize the primary key for your most common query!
     */
    @DynamoDbPartitionKey
    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Sort key - provides chronological ordering within a partition.
     * DynamoDB automatically sorts items by this value.
     * Result: Messages for Bob are returned oldest-to-newest (or newest-to-oldest).
     * 
     * Unix timestamp (milliseconds) ensures uniqueness and natural ordering.
     */
    @DynamoDbSortKey
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Get the encrypted message content.
     * Stored as Base64 string for easy JSON serialization.
     * The actual encrypted bytes are: Double Ratchet ciphertext + MAC + metadata.
     */
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
