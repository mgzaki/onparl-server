package com.onparl.server.repository;

import com.onparl.server.model.SignalMessageEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DYNAMODB MESSAGE REPOSITORY - Data access layer for encrypted messages.
 * 
 * PURPOSE:
 * Provides CRUD operations for storing and retrieving encrypted messages.
 * Abstracts DynamoDB complexity from business logic (controllers).
 * 
 * TABLE: "onparl-messages"
 * - Partition key: recipientId
 * - Sort key: timestamp
 * - This design optimizes for the query: "Get all messages for user X"
 * 
 * OPERATIONS:
 * - save(): Store a new encrypted message
 * - findByRecipientId(): Retrieve all messages for a recipient (inbox pattern)
 * 
 * NO ENCRYPTION HERE:
 * This repository blindly stores encrypted blobs - it never sees plaintext!
 */
@Repository // Spring Data Access Component
public class DynamoDbMessageRepository {

    private final DynamoDbTable<SignalMessageEntity> table;

    /**
     * Constructor - initialize DynamoDB table mapping.
     * 
     * @param enhancedClient AWS DynamoDB Enhanced Client (injected by Spring)
     */
    public DynamoDbMessageRepository(DynamoDbEnhancedClient enhancedClient) {
        // Map SignalMessageEntity class to "onparl-messages" table
        // TableSchema.fromBean uses reflection to understand the entity structure
        this.table = enhancedClient.table("onparl-messages", TableSchema.fromBean(SignalMessageEntity.class));
    }

    /**
     * Save a message to DynamoDB.
     * 
     * OPERATION: PUT (upsert)
     * - If a message with same recipientId+timestamp exists, it's overwritten
     * - Usually each message has a unique timestamp, so this creates new items
     * 
     * ENCRYPTION NOTE:
     * The message.content field contains encrypted data - we store it blindly!
     */
    public void save(SignalMessageEntity message) {
        table.putItem(message); // DynamoDB PutItem operation
    }

    /**
     * Find all messages for a specific recipient.
     * 
     * OPERATION: QUERY (not Scan!)
     * - Query is efficient: Uses partition key index
     * - Only reads items for this specific recipientId
     * - Returns results sorted by timestamp (oldest to newest)
     * 
     * QUERY COST:
     * - Reads only from one partition → Fast!
     * - Scan alternative: Read ENTIRE table → Slow and expensive!
     * 
     * USE CASE:
     * When Bob opens the app, fetch all pending messages: findByRecipientId("bob")
     * 
     * @param recipientId User ID to fetch messages for
     * @return List of encrypted messages (still encrypted!)
     */
    public List<SignalMessageEntity> findByRecipientId(String recipientId) {
        // Build a DynamoDB key with just the partition key
        Key key = Key.builder().partitionValue(recipientId).build();

        // Query: "WHERE recipientId = {recipientId}" (automatically sorted by
        // timestamp)
        return table.query(r -> r.queryConditional(QueryConditional.keyEqualTo(key)))
                .items() // Get lazy iterable of results
                .stream() // Convert to Java Stream
                .collect(Collectors.toList()); // Collect into List
    }
}
