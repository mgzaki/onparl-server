package com.example.onparl.server.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Container class for all cryptographic key entities used in the Signal
 * Protocol.
 * 
 * SIGNAL PROTOCOL KEY HIERARCHY:
 * The Signal Protocol (used by WhatsApp, Signal app, etc.) uses three types of
 * public keys:
 * 
 * 1. IDENTITY KEY: Long-term key that identifies a user (like a digital
 * fingerprint)
 * 2. SIGNED PREKEY: Medium-term key signed by the Identity Key (rotated
 * periodically, e.g., weekly)
 * 3. ONE-TIME PREKEYS: Short-lived keys used once and then deleted (for forward
 * secrecy)
 * 
 * These keys work together to establish secure encrypted sessions between users
 * using the
 * X3DH (Extended Triple Diffie-Hellman) key agreement protocol.
 * 
 * NOTE: This server only stores PUBLIC keys - private keys never leave the
 * client device!
 */
public class KeyEntities {

    /**
     * IDENTITY KEY ENTITY - The user's long-term cryptographic identity.
     * 
     * PURPOSE:
     * - Acts as the "root of trust" for a user's identity
     * - Used to verify signed prekeys (proving they belong to the same user)
     * - Rarely changes (similar to your passport)
     * 
     * STORAGE PATTERN:
     * - Stored in DynamoDB table: "onparl-identity-keys"
     * - Primary key: userId (partition key only, one identity per user)
     * - The publicKey is stored as Base64-encoded string for easy JSON
     * serialization
     * 
     * SIGNAL PROTOCOL ROLE:
     * - Part of the X3DH key exchange protocol
     * - Recipients use this to verify the authenticity of your signed prekey
     */
    @DynamoDbBean // Tells AWS SDK this class maps to a DynamoDB table
    public static class IdentityKeyEntity {
        private String userId; // Unique identifier for the user (e.g., "alice@example.com")
        private String publicKey; // Base64-encoded public key bytes (33 bytes for Curve25519)
        private int registrationId; // Random ID to detect reinstalls/key resets

        /**
         * Default constructor required by DynamoDB SDK for object instantiation.
         * AWS SDK uses reflection to create instances when reading from the database.
         */
        public IdentityKeyEntity() {
        }

        /**
         * Get the user ID (DynamoDB Partition Key).
         * 
         * @DynamoDbPartitionKey tells DynamoDB this field is the primary key for
         *                       lookups.
         *                       This means you can quickly find a user's identity key
         *                       with just their userId.
         */
        @DynamoDbPartitionKey
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        /**
         * Get the public key as a Base64-encoded string.
         * The actual key is 32 bytes of elliptic curve point data (Curve25519).
         */
        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        /**
         * Get the registration ID - a random number generated during key setup.
         * If this changes, it means the user reinstalled the app or reset their keys.
         */
        public int getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(int registrationId) {
            this.registrationId = registrationId;
        }
    }

    /**
     * ONE-TIME PREKEY ENTITY - Short-lived keys for forward secrecy.
     * 
     * PURPOSE:
     * - Used ONCE to establish an encrypted session, then deleted forever
     * - Provides "forward secrecy" - even if your long-term keys leak, old messages
     * stay secure
     * - Think of them as "burner phones" for encryption
     * 
     * STORAGE PATTERN:
     * - Stored in DynamoDB table: "onparl-pre-keys"
     * - Composite key: userId (partition) + keyId (sort key)
     * - Users typically upload 100 prekeys at a time
     * - Server deletes one each time someone initiates a conversation
     * 
     * LIFECYCLE:
     * 1. Client generates 100 prekeys and uploads them
     * 2. When Alice messages Bob, server gives Alice one of Bob's prekeys
     * 3. Server DELETES that prekey ("one-time" use)
     * 4. Alice uses it to establish a session with Bob
     * 5. When Bob runs low on prekeys, he uploads more
     */
    @DynamoDbBean
    public static class PreKeyEntity {
        private String userId; // Who owns this prekey
        private int keyId; // Unique ID for this specific prekey (0-65535)
        private String publicKey; // Base64-encoded public key

        public PreKeyEntity() {
        }

        /**
         * Partition key - groups all prekeys for a user together in DynamoDB.
         */
        @DynamoDbPartitionKey
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        /**
         * Sort key - allows efficient queries like "give me any one prekey for this
         * user".
         * The combination of userId + keyId uniquely identifies each prekey.
         * Example: user "alice" might have prekeys with IDs 0, 1, 2, ..., 99
         */
        @DynamoDbSortKey
        public int getKeyId() {
            return keyId;
        }

        public void setKeyId(int keyId) {
            this.keyId = keyId;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
    }

    /**
     * SIGNED PREKEY ENTITY - Medium-term key with cryptographic signature.
     * 
     * PURPOSE:
     * - Bridges the gap between long-term identity keys and one-time prekeys
     * - Includes a signature to prove it was created by the identity key owner
     * - Rotated periodically (e.g., weekly) for security hygiene
     * 
     * STORAGE PATTERN:
     * - Stored in DynamoDB table: "onparl-signed-pre-keys"
     * - Composite key: userId (partition) + keyId (sort key)
     * - Users typically maintain 1-5 signed prekeys at a time
     * 
     * WHY THE SIGNATURE?
     * - Without it, an attacker could upload fake prekeys pretending to be you
     * - The signature proves: "This prekey was created by whoever owns the identity
     * key"
     * - Signature = sign(prekey public bytes) using identity private key
     * 
     * SIGNAL PROTOCOL ROLE:
     * - Part of X3DH protocol - used when one-time prekeys run out
     * - Provides authentication (who you're talking to) AND encryption (secrecy)
     */
    @DynamoDbBean
    public static class SignedPreKeyEntity {
        private String userId;
        private int keyId;
        private String publicKey; // Base64-encoded public key
        private String signature; // Base64-encoded signature (identity key signed this prekey)

        public SignedPreKeyEntity() {
        }

        @DynamoDbPartitionKey
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @DynamoDbSortKey
        public int getKeyId() {
            return keyId;
        }

        public void setKeyId(int keyId) {
            this.keyId = keyId;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        /**
         * Get the cryptographic signature.
         * This signature proves the prekey is authentic and was created by the identity
         * key owner.
         * Verification: verify(signature, publicKey, identityPublicKey)
         */
        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }
}
