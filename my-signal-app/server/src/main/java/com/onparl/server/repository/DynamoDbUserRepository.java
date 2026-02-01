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
     * Find a user by unique identifier (partition key lookup - very fast).
     */
    public Optional<User> findById(String id) {
        Key key = Key.builder()
                .partitionValue(id)
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
    public void updateLastLogin(String id) {
        findById(id).ifPresent(user -> {
            user.setLastLoginAt(System.currentTimeMillis());
            saveUser(user);
        });
    }

    /**
     * Delete a user by id.
     */
    public void deleteUser(String id) {
        Key key = Key.builder()
                .partitionValue(id)
                .build();

        userTable.deleteItem(key);
    }

    /**
     * Check if a user exists by id.
     */
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }
}
