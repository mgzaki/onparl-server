package com.example.signal.server.repository;

import com.example.signal.server.model.KeyEntities;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DynamoDbKeyRepository {

    private final DynamoDbTable<KeyEntities.IdentityKeyEntity> identityKeyTable;
    private final DynamoDbTable<KeyEntities.PreKeyEntity> preKeyTable;
    private final DynamoDbTable<KeyEntities.SignedPreKeyEntity> signedPreKeyTable;

    public DynamoDbKeyRepository(DynamoDbEnhancedClient enhancedClient) {
        this.identityKeyTable = enhancedClient.table("signal-identity-keys",
                TableSchema.fromBean(KeyEntities.IdentityKeyEntity.class));
        this.preKeyTable = enhancedClient.table("signal-pre-keys",
                TableSchema.fromBean(KeyEntities.PreKeyEntity.class));
        this.signedPreKeyTable = enhancedClient.table("signal-signed-pre-keys",
                TableSchema.fromBean(KeyEntities.SignedPreKeyEntity.class));
    }

    // Identity Key
    public void saveIdentityKey(KeyEntities.IdentityKeyEntity identityKey) {
        identityKeyTable.putItem(identityKey);
    }

    public Optional<KeyEntities.IdentityKeyEntity> findIdentityKey(String userId) {
        Key key = Key.builder().partitionValue(userId).build();
        return Optional.ofNullable(identityKeyTable.getItem(key));
    }

    // One-Time PreKey
    public void savePreKey(KeyEntities.PreKeyEntity preKey) {
        preKeyTable.putItem(preKey);
    }

    public Optional<KeyEntities.PreKeyEntity> findPreKey(String userId, int keyId) {
        Key key = Key.builder().partitionValue(userId).sortValue(keyId).build();
        return Optional.ofNullable(preKeyTable.getItem(key));
    }

    public Optional<KeyEntities.PreKeyEntity> findOnePreKey(String userId) {
        Key key = Key.builder().partitionValue(userId).build();
        return preKeyTable.query(r -> r.queryConditional(QueryConditional.keyEqualTo(key)).limit(1))
                .items().stream().findFirst();
    }

    public void deletePreKey(String userId, int keyId) {
        Key key = Key.builder().partitionValue(userId).sortValue(keyId).build();
        preKeyTable.deleteItem(key);
    }

    // Signed PreKey
    public void saveSignedPreKey(KeyEntities.SignedPreKeyEntity signedPreKey) {
        signedPreKeyTable.putItem(signedPreKey);
    }

    public Optional<KeyEntities.SignedPreKeyEntity> findSignedPreKey(String userId, int keyId) {
        Key key = Key.builder().partitionValue(userId).sortValue(keyId).build();
        return Optional.ofNullable(signedPreKeyTable.getItem(key));
    }
}
