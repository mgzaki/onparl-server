package com.example.signal.server.config;

import com.example.signal.server.model.KeyEntities;
import com.example.signal.server.model.SignalMessageEntity;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

@Component
public class TableInitializer {

    private final DynamoDbEnhancedClient enhancedClient;

    public TableInitializer(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createTables() {
        createTable("signal-identity-keys", KeyEntities.IdentityKeyEntity.class);
        createTable("signal-pre-keys", KeyEntities.PreKeyEntity.class);
        createTable("signal-signed-pre-keys", KeyEntities.SignedPreKeyEntity.class);
        createTable("signal-messages", SignalMessageEntity.class);
    }

    private <T> void createTable(String tableName, Class<T> beanClass) {
        try {
            enhancedClient.table(tableName, TableSchema.fromBean(beanClass)).createTable();
            System.out.println("Created table: " + tableName);
        } catch (ResourceInUseException e) {
            System.out.println("Table already exists: " + tableName);
        } catch (Exception e) {
            System.err.println("Failed to create table " + tableName + ": " + e.getMessage());
        }
    }
}
