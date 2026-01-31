package com.onparl.server.config;

import com.onparl.server.model.KeyEntities;
import com.onparl.server.model.SignalMessageEntity;
import com.onparl.server.model.User;
import com.onparl.server.model.OtpVerification;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

/**
 * TABLE INITIALIZER - Automatically creates DynamoDB tables on application
 * startup.
 * 
 * PURPOSE:
 * Ensures all required DynamoDB tables exist before the application tries to
 * use them.
 * Without this, the first API call would fail with "Table not found" error!
 * 
 * HOW IT WORKS:
 * 1. Spring Boot starts up and initializes all beans
 * 2. When initialization is complete, fires ApplicationReadyEvent
 * 3. This class listens for that event and creates tables
 * 4. If tables already exist, silently continues (idempotent)
 * 
 * IDEMPOTENT:
 * Running this multiple times is safe - it won't recreate existing tables.
 * ResourceInUseException = table exists = success!
 * 
 * PRODUCTION CONSIDERATIONS:
 * - In production, you'd typically use Terraform/CloudFormation to create
 * tables
 * - This approach is convenient for development and testing
 * - For production: Either disable this or ensure IAM role has CreateTable
 * permission
 * 
 * TABLES CREATED:
 * 1. onparl-identity-keys: Long-term user identity keys
 * 2. onparl-pre-keys: One-time prekeys for forward secrecy
 * 3. onparl-signed-pre-keys: Medium-term signed prekeys
 * 4. onparl-messages: Encrypted message storage
 */
@Component // Spring-managed component (singleton bean)
public class TableInitializer {

    private final DynamoDbEnhancedClient enhancedClient;

    public TableInitializer(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    /**
     * Triggered automatically when Spring Boot application is fully started.
     * 
     * @EventListener tells Spring to call this method when ApplicationReadyEvent
     *                fires.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void createTables() {
        // Create all DynamoDB tables (idempotent - safe to run multiple times)

        // Authentication tables
        createTable("onparl-users", User.class);
        createTable("onparl-otp-verifications", OtpVerification.class);

        // Signal Protocol key tables
        createTable("onparl-identity-keys", KeyEntities.IdentityKeyEntity.class);
        createTable("onparl-pre-keys", KeyEntities.PreKeyEntity.class);
        createTable("onparl-signed-pre-keys", KeyEntities.SignedPreKeyEntity.class);

        // Message storage
        createTable("onparl-messages", SignalMessageEntity.class);
    }

    /**
     * Generic method to create a DynamoDB table from a bean class.
     * 
     * @param tableName Name of the table to create
     * @param beanClass Entity class with @DynamoDbBean annotations
     * @param <T>       Type of the entity
     * 
     *                  SCHEMA DERIVATION:
     *                  - TableSchema.fromBean(beanClass) uses reflection to read
     *                  annotations
     *                  - Finds @DynamoDbPartitionKey and @DynamoDbSortKey
     *                  - Automatically creates table with correct schema!
     * 
     *                  ERROR HANDLING:
     *                  - ResourceInUseException = table exists = success (not an
     *                  error!)
     *                  - Other exceptions = log and continue (don't crash the app)
     */
    private <T> void createTable(String tableName, Class<T> beanClass) {
        try {
            // Create table using inferred schema from bean annotations
            enhancedClient.table(tableName, TableSchema.fromBean(beanClass)).createTable();
            System.out.println("Created table: " + tableName);
        } catch (ResourceInUseException e) {
            // Table already exists - this is fine! (idempotent operation)
            System.out.println("Table already exists: " + tableName);
        } catch (Exception e) {
            // Other error (permissions, network, etc.) - log but don't crash
            System.err.println("Failed to create table " + tableName + ": " + e.getMessage());
        }
    }
}
