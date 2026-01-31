package com.onparl.server.controller;

import com.onparl.server.model.KeyEntities;
import com.onparl.server.repository.DynamoDbKeyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * KEY CONTROLLER - The Key Distribution Center (KDC) for Signal Protocol.
 * 
 * WHAT IS THIS?
 * This REST API allows clients to upload their public cryptographic keys and
 * download other users' keys to establish encrypted communication sessions.
 * 
 * LIFECYCLE:
 * 1. NEW USER SETUP: Alice uploads her identity key, signed prekey, and 100
 * one-time prekeys
 * 2. SESSION ESTABLISHMENT: When Bob wants to message Alice, he fetches her
 * "key bundle"
 * 3. KEY CONSUMPTION: Server gives Bob one of Alice's prekeys and DELETES it
 * (one-time use)
 * 4. ENCRYPTION: Bob uses the bundle to perform X3DH and establish a session
 * 5. MAINTENANCE: Alice periodically uploads new prekeys as they get consumed
 * 
 * SECURITY NOTE:
 * - This server NEVER sees private keys or decrypted messages
 * - It only facilitates public key exchange (like a phonebook for encryption
 * keys)
 * - All encryption/decryption happens on client devices
 * 
 * API ENDPOINTS:
 * - POST /api/keys/* - Upload keys (called by key owners)
 * - GET /api/keys/* - Fetch keys (called by people wanting to message the key
 * owner)
 */
@RestController
@RequestMapping("/api/keys")
public class KeyController {

    private final DynamoDbKeyRepository keyRepository;

    /**
     * Constructor with dependency injection.
     * Spring automatically provides the DynamoDbKeyRepository instance.
     */
    public KeyController(DynamoDbKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    // =================================================================================
    // KEY UPLOAD ENDPOINTS - Called by users to publish their public keys
    // =================================================================================

    /**
     * Upload your identity key (called once during initial setup).
     * 
     * POST /api/keys/identity
     * 
     * REQUEST BODY: {"userId": "alice", "publicKey": "base64...", "registrationId":
     * 12345}
     * 
     * WHO CALLS THIS: The key owner (Alice) when first setting up their account.
     * FREQUENCY: Once initially, and again only if resetting all keys.
     */
    @PostMapping("/identity")
    public void uploadIdentityKey(@RequestBody KeyEntities.IdentityKeyEntity identityKey) {
        keyRepository.saveIdentityKey(identityKey);
    }

    /**
     * Upload a one-time prekey (typically uploaded in batches of 100).
     * 
     * POST /api/keys/prekey
     * 
     * REQUEST BODY: {"userId": "alice", "keyId": 0, "publicKey": "base64..."}
     * 
     * WHO CALLS THIS: The key owner, repeatedly to maintain a pool of prekeys.
     * FREQUENCY: Initially upload ~100, then refresh when running low.
     */
    @PostMapping("/prekey")
    public void uploadPreKey(@RequestBody KeyEntities.PreKeyEntity preKey) {
        keyRepository.savePreKey(preKey);
    }

    /**
     * Upload a signed prekey (rotated weekly for security).
     * 
     * POST /api/keys/signed-prekey
     * 
     * REQUEST BODY: {"userId": "alice", "keyId": 456, "publicKey": "base64...",
     * "signature": "base64..."}
     * 
     * WHO CALLS THIS: The key owner when rotating their signed prekey.
     * FREQUENCY: Weekly or monthly (security best practice).
     */
    @PostMapping("/signed-prekey")
    public void uploadSignedPreKey(@RequestBody KeyEntities.SignedPreKeyEntity signedPreKey) {
        keyRepository.saveSignedPreKey(signedPreKey);
    }

    // =================================================================================
    // KEY FETCH ENDPOINTS - Called by others to retrieve public keys
    // =================================================================================

    /**
     * Fetch a user's identity key.
     * 
     * GET /api/keys/identity/{userId}
     * 
     * RESPONSE: {"userId": "alice", "publicKey": "base64...", "registrationId":
     * 12345}
     * 
     * WHO CALLS THIS: Anyone who wants to verify Alice's signed prekey signature.
     * Used during initial session setup as part of X3DH protocol.
     */
    @GetMapping("/identity/{userId}")
    public ResponseEntity<KeyEntities.IdentityKeyEntity> getIdentityKey(@PathVariable String userId) {
        return keyRepository.findIdentityKey(userId)
                .map(ResponseEntity::ok) // If found, return 200 OK with the key
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404
    }

    /**
     * Fetch a specific one-time prekey by ID (rarely used directly).
     * 
     * GET /api/keys/prekey/{userId}/{keyId}
     * 
     * NOTE: Usually you'd use /bundle instead, which picks a random prekey for you.
     * This endpoint is mainly for testing or specific use cases.
     */
    @GetMapping("/prekey/{userId}/{keyId}")
    public ResponseEntity<KeyEntities.PreKeyEntity> getPreKey(@PathVariable String userId, @PathVariable int keyId) {
        return keyRepository.findPreKey(userId, keyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Fetch a specific signed prekey by ID.
     * 
     * GET /api/keys/signed-prekey/{userId}/{keyId}
     * 
     * NOTE: Usually you'd use /bundle instead, which includes the current signed
     * prekey.
     * This endpoint exists for key verification or debugging.
     */
    @GetMapping("/signed-prekey/{userId}/{keyId}")
    public ResponseEntity<KeyEntities.SignedPreKeyEntity> getSignedPreKey(@PathVariable String userId,
            @PathVariable int keyId) {
        return keyRepository.findSignedPreKey(userId, keyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * *** MOST IMPORTANT ENDPOINT *** - Get a complete key bundle to start
     * messaging a user.
     * 
     * GET /api/keys/bundle/{userId}
     * 
     * This implements the "prekey bundle" concept from the Signal Protocol
     * specification.
     * 
     * WHAT IT RETURNS:
     * {
     * "identityKey": {...}, // User's long-term identity
     * "signedPreKey": {...}, // Current signed prekey + signature
     * "preKey": {...} // ONE one-time prekey (if available)
     * }
     * 
     * HOW IT WORKS:
     * 1. Fetch the user's identity key (required - returns 404 if not found)
     * 2. Fetch the current signed prekey (hardcoded to ID 456 in this demo)
     * 3. Fetch and DELETE one random one-time prekey (forward secrecy!)
     * 
     * WHO CALLS THIS:
     * Bob calls this when he wants to send his first message to Alice.
     * He uses this bundle to perform X3DH key agreement and establish a session.
     * 
     * SECURITY NOTE:
     * The one-time prekey is DELETED after being fetched, ensuring it can only
     * be used once. This provides forward secrecy - even if keys leak later,
     * past sessions remain secure.
     */
    @GetMapping("/bundle/{userId}")
    public ResponseEntity<Map<String, Object>> getKeyBundle(@PathVariable String userId) {
        // 1. Get Identity Key (mandatory - without this, we can't establish trust)
        var identityKeyOpt = keyRepository.findIdentityKey(userId);
        if (identityKeyOpt.isEmpty())
            return ResponseEntity.notFound().build(); // User hasn't set up keys yet

        Map<String, Object> bundle = new HashMap<>();
        bundle.put("identityKey", identityKeyOpt.get());

        // 2. Get Signed PreKey (using hardcoded ID 456 for simplicity)
        // TODO: In production, fetch the latest/current signed prekey dynamically
        var signedPreKeyOpt = keyRepository.findSignedPreKey(userId, 456);
        signedPreKeyOpt.ifPresent(k -> bundle.put("signedPreKey", k));

        // 3. Get One One-Time PreKey (if any are available)
        var preKeyOpt = keyRepository.findOnePreKey(userId);
        if (preKeyOpt.isPresent()) {
            var preKey = preKeyOpt.get();
            bundle.put("preKey", preKey);
            // CRITICAL: Delete the prekey immediately after giving it out
            // This ensures "one-time" usage and provides forward secrecy
            keyRepository.deletePreKey(userId, preKey.getKeyId());
        }
        // NOTE: If no one-time prekeys available, bundle still works (uses signed
        // prekey only)

        return ResponseEntity.ok(bundle);
    }
}
