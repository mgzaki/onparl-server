package com.onparl.server.repository;

import com.onparl.server.model.KeyEntities;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DYNAMODB KEY REPOSITORY - Data Access Layer for cryptographic keys.
 * 
 * REPOSITORY PATTERN:
 * This class encapsulates all database operations for cryptographic keys,
 * providing a clean abstraction between business logic and data storage.
 * 
 * DYNAMODB ENHANCED CLIENT:
 * - Uses AWS SDK's "Enhanced Client" for object-relational mapping (ORM-style)
 * - Automatically converts Java objects to/from DynamoDB JSON format
 * - Handles table names, key definitions, and type conversions
 * 
 * THREE TABLES:
 * 1. onparl-identity-keys: One identity key per user (partition key: userId)
 * 2. onparl-pre-keys: Many one-time prekeys per user (partition: userId, sort:
 * keyId)
 * 3. onparl-signed-pre-keys: Few signed prekeys per user (partition: userId,
 * sort: keyId)
 * 
 * WHY SEPARATE TABLES?
 * - Different access patterns (identity = single item, prekeys = batch queries)
 * - Different lifecycle (identity = permanent, one-time prekeys = consumed)
 * - Better performance (avoid hot partitions from mixed table)
 */
@Repository // Spring stereotype for data access components
public class DynamoDbKeyRepository {

    // Table references - initialized once in constructor
    private final DynamoDbTable<KeyEntities.IdentityKeyEntity> identityKeyTable;
    private final DynamoDbTable<KeyEntities.PreKeyEntity> preKeyTable;
    private final DynamoDbTable<KeyEntities.SignedPreKeyEntity> signedPreKeyTable;

    /**
     * Constructor initializes DynamoDB table mappings.
     * 
     * @param enhancedClient AWS DynamoDB Enhanced Client (injected by Spring)
     */
    public DynamoDbKeyRepository(DynamoDbEnhancedClient enhancedClient) {
        // Map Java classes to DynamoDB tables using TableSchema
        this.identityKeyTable = enhancedClient.table("onparl-identity-keys",
                TableSchema.fromBean(KeyEntities.IdentityKeyEntity.class));
        this.preKeyTable = enhancedClient.table("onparl-pre-keys",
                TableSchema.fromBean(KeyEntities.PreKeyEntity.class));
        this.signedPreKeyTable = enhancedClient.table("onparl-signed-pre-keys",
                TableSchema.fromBean(KeyEntities.SignedPreKeyEntity.class));
    }

    // =================================================================================
    // IDENTITY KEY OPERATIONS
    // =================================================================================

    /**
     * Save or update an identity key (PUT operation).
     * If a key already exists for this userId, it will be overwritten.
     */
    public void saveIdentityKey(KeyEntities.IdentityKeyEntity identityKey) {
        identityKeyTable.putItem(identityKey); // DynamoDB PUT (upsert)
    }

    /**
     * Find an identity key by userId.
     * 
     * @return Optional containing the key if found, empty if not found
     */
    public Optional<KeyEntities.IdentityKeyEntity> findIdentityKey(String userId) {
        // Build a DynamoDB Key (partition key only)
        Key key = Key.builder().partitionValue(userId).build();
        // getItem returns null if not found, so wrap in Optional
        return Optional.ofNullable(identityKeyTable.getItem(key));
    }

    // =================================================================================
    // ONE-TIME PREKEY OPERATIONS
    // =================================================================================

    /**
     * Save a one-time prekey (usually called in batch for 100+ keys).
     */
    public void savePreKey(KeyEntities.PreKeyEntity preKey) {
        preKeyTable.putItem(preKey);
    }

    /**
     * Find a specific prekey by userId and keyId (rarely used).
     */
    public Optional<KeyEntities.PreKeyEntity> findPreKey(String userId, int keyId) {
        // Build a composite key (partition + sort)
        Key key = Key.builder().partitionValue(userId).sortValue(keyId).build();
        return Optional.ofNullable(preKeyTable.getItem(key));
    }

    /**
     * Find ANY ONE available prekey for a user (most common operation).
     * 
     * This uses DynamoDB Query (not Scan!) which is efficient:
     * 1. Query for userId (partition key)
     * 2. Limit results to 1 item
     * 3. Return the first available prekey
     * 
     * NOTE: This doesn't guarantee random selection - it returns whichever
     * prekey DynamoDB finds first. For better randomness, you could query
     * multiple keys and pick randomly.
     */
    public Optional<KeyEntities.PreKeyEntity> findOnePreKey(String userId) {
        Key key = Key.builder().partitionValue(userId).build();
        // Query with limit(1) for efficiency - only fetch what we need
        return preKeyTable.query(r -> r.queryConditional(QueryConditional.keyEqualTo(key)).limit(1))
                .items().stream().findFirst(); // Get first result from query
    }

    /**
     * Delete a one-time prekey after it has been consumed.
     * 
     * CRITICAL FOR SECURITY:
     * One-time prekeys must be deleted after use to ensure forward secrecy.
     * If the same prekey is used twice, the security guarantees are broken.
     */
    public void deletePreKey(String userId, int keyId) {
        Key key = Key.builder().partitionValue(userId).sortValue(keyId).build();
        preKeyTable.deleteItem(key); // DynamoDB DELETE operation
    }

    // =================================================================================
    // SIGNED PREKEY OPERATIONS
    // =================================================================================

    /**
     * Save a signed prekey (typically rotated weekly/monthly).
     */
    public void saveSignedPreKey(KeyEntities.SignedPreKeyEntity signedPreKey) {
        signedPreKeyTable.putItem(signedPreKey);
    }

    /**
     * Find a specific signed prekey by userId and keyId.
     * 
     * TODO: Add a method to fetch the "current" or "latest" signed prekey
     * without needing to know the keyId in advance.
     */
    public Optional<KeyEntities.SignedPreKeyEntity> findSignedPreKey(String userId, int keyId) {
        Key key = Key.builder().partitionValue(userId).sortValue(keyId).build();
        return Optional.ofNullable(signedPreKeyTable.getItem(key));
    }
}
