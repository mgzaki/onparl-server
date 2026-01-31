package com.onparl.server.repository;

import com.onparl.server.model.User;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Optional;

/**
 * Repository for User entity operations with DynamoDB.
 * 
 * Provides CRUD operations for users authenticated via phone numbers.
 * Supports lookups by both phoneNumber (partition key) and userId (GSI).
 */
@Repository
public class DynamoDbUserRepository {

    private final DynamoDbTable<User> userTable;
    private final DynamoDbIndex<User> userIdIndex;

    public DynamoDbUserRepository(DynamoDbEnhancedClient dynamoDbClient) {
        this.userTable = dynamoDbClient.table("onparl-users", TableSchema.fromBean(User.class));
        this.userIdIndex = userTable.index("userId-index");
    }

    /**
     * Save or update a user.
     */
    public void saveUser(User user) {
        userTable.putItem(user);
    }

    /**
     * Find a user by phone number (partition key lookup - very fast).
     */
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        Key key = Key.builder()
                .partitionValue(phoneNumber)
                .build();

        User user = userTable.getItem(key);
        return Optional.ofNullable(user);
    }

    /**
     * Find a user by userId using the GSI (Global Secondary Index).
     * Slightly slower than partition key lookup but still efficient.
     */
    public Optional<User> findByUserId(String userId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(userId).build());

        return userIdIndex.query(queryConditional)
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }

    /**
     * Update the last login timestamp for a user.
     */
    public void updateLastLogin(String phoneNumber) {
        findByPhoneNumber(phoneNumber).ifPresent(user -> {
            user.setLastLoginAt(System.currentTimeMillis());
            saveUser(user);
        });
    }

    /**
     * Delete a user by phone number.
     */
    public void deleteUser(String phoneNumber) {
        Key key = Key.builder()
                .partitionValue(phoneNumber)
                .build();

        userTable.deleteItem(key);
    }

    /**
     * Check if a user exists by phone number.
     */
    public boolean existsByPhoneNumber(String phoneNumber) {
        return findByPhoneNumber(phoneNumber).isPresent();
    }
}
