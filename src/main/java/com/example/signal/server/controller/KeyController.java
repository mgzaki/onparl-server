package com.example.signal.server.controller;

import com.example.signal.server.model.KeyEntities;
import com.example.signal.server.repository.DynamoDbKeyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
public class KeyController {

    private final DynamoDbKeyRepository keyRepository;

    public KeyController(DynamoDbKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    // --- Uploads ---

    @PostMapping("/identity")
    public void uploadIdentityKey(@RequestBody KeyEntities.IdentityKeyEntity identityKey) {
        keyRepository.saveIdentityKey(identityKey);
    }

    @PostMapping("/prekey")
    public void uploadPreKey(@RequestBody KeyEntities.PreKeyEntity preKey) {
        keyRepository.savePreKey(preKey);
    }

    @PostMapping("/signed-prekey")
    public void uploadSignedPreKey(@RequestBody KeyEntities.SignedPreKeyEntity signedPreKey) {
        keyRepository.saveSignedPreKey(signedPreKey);
    }

    // --- Fetches ---

    @GetMapping("/identity/{userId}")
    public ResponseEntity<KeyEntities.IdentityKeyEntity> getIdentityKey(@PathVariable String userId) {
        return keyRepository.findIdentityKey(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prekey/{userId}/{keyId}")
    public ResponseEntity<KeyEntities.PreKeyEntity> getPreKey(@PathVariable String userId, @PathVariable int keyId) {
        return keyRepository.findPreKey(userId, keyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/signed-prekey/{userId}/{keyId}")
    public ResponseEntity<KeyEntities.SignedPreKeyEntity> getSignedPreKey(@PathVariable String userId,
            @PathVariable int keyId) {
        return keyRepository.findSignedPreKey(userId, keyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bundle/{userId}")
    public ResponseEntity<Map<String, Object>> getKeyBundle(@PathVariable String userId) {
        // 1. Get Identity Key
        var identityKeyOpt = keyRepository.findIdentityKey(userId);
        if (identityKeyOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Map<String, Object> bundle = new HashMap<>();
        bundle.put("identityKey", identityKeyOpt.get());

        // 2. Get Signed PreKey (Assuming ID 456 for demo simplicity, or fetch latest)
        var signedPreKeyOpt = keyRepository.findSignedPreKey(userId, 456);
        signedPreKeyOpt.ifPresent(k -> bundle.put("signedPreKey", k));

        // 3. Get One One-Time PreKey (Pick any available)
        var preKeyOpt = keyRepository.findOnePreKey(userId);
        if (preKeyOpt.isPresent()) {
            var preKey = preKeyOpt.get();
            bundle.put("preKey", preKey);
            keyRepository.deletePreKey(userId, preKey.getKeyId());
        }

        return ResponseEntity.ok(bundle);
    }
}
