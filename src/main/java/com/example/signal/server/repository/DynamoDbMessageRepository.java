package com.example.signal.server.repository;

import com.example.signal.server.model.SignalMessageEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DynamoDbMessageRepository {

    private final DynamoDbTable<SignalMessageEntity> table;

    public DynamoDbMessageRepository(DynamoDbEnhancedClient enhancedClient) {
        // Bind to table "signal-messages" created in Tofu
        this.table = enhancedClient.table("signal-messages", TableSchema.fromBean(SignalMessageEntity.class));
    }

    public void save(SignalMessageEntity message) {
        table.putItem(message);
    }

    public List<SignalMessageEntity> findByRecipientId(String recipientId) {
        Key key = Key.builder().partitionValue(recipientId).build();
        return table.query(r -> r.queryConditional(QueryConditional.keyEqualTo(key)))
                .items().stream().collect(Collectors.toList());
    }
}
