package com.example.onparl.server.controller;

import com.example.onparl.server.model.SignalMessageEntity;
import com.example.onparl.server.repository.DynamoDbMessageRepository;
import com.example.onparl.server.model.SignalMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MESSAGE CONTROLLER - Handles encrypted message storage and delivery.
 * 
 * CRITICAL SECURITY CONCEPT:
 * This server acts as a "dumb relay" - it stores and forwards ENCRYPTED
 * messages
 * without ever decrypting them. The server has NO IDEA what the messages say!
 * 
 * MESSAGE FLOW:
 * 1. Alice encrypts a message using her session with Bob (Double Ratchet
 * algorithm)
 * 2. Alice sends the encrypted payload to this server via POST /api/messages
 * 3. Server stores the encrypted blob in DynamoDB (no decryption happens!)
 * 4. Server pushes the encrypted message to Bob via WebSocket (if he's online)
 * 5. Bob's client decrypts the message locally using his session state
 * 
 * WHY DO WE STORE MESSAGES?
 * - Offline delivery: If Bob is offline, messages wait for him in the database
 * - Message history: Users can sync their message history across devices
 * - Persistence: Messages survive server restarts
 * 
 * WEBSOCKET INTEGRATION:
 * - Uses STOMP protocol over SockJS for real-time delivery
 * - Messages published to /topic/messages/{recipientId}
 * - Clients subscribe to their userId topic to receive live updates
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final DynamoDbMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate; // For WebSocket push

    /**
     * Constructor with dependency injection.
     * Spring automatically provides both repository and WebSocket template.
     */
    public MessageController(DynamoDbMessageRepository messageRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send a message (stores encrypted payload and pushes to recipient).
     * 
     * POST /api/messages
     * 
     * REQUEST BODY:
     * {
     * "senderId": "alice",
     * "recipientId": "bob",
     * "content": "base64_encrypted_blob", // ENCRYPTED! Server can't read this
     * "timestamp": 1234567890
     * }
     * 
     * STEPS:
     * 1. Convert DTO (Data Transfer Object) to Entity for database storage
     * 2. Save encrypted message to DynamoDB
     * 3. Push notification to recipient via WebSocket (if online)
     * 
     * ENCRYPTION NOTE:
     * The "content" field contains an encrypted blob that only the recipient can
     * decrypt.
     * The server blindly stores and forwards it without knowing what it says.
     */
    @PostMapping
    public void sendMessage(@RequestBody SignalMessage message) {
        // Convert the incoming message DTO to a DynamoDB entity
        SignalMessageEntity entity = new SignalMessageEntity();
        entity.setRecipientId(message.getRecipientId());
        entity.setSenderId(message.getSenderId());
        entity.setContent(message.getContent()); // This is encrypted!
        entity.setTimestamp(message.getTimestamp());

        // Persist to DynamoDB (for offline delivery and message history)
        messageRepository.save(entity);

        // Real-time push: Send encrypted message to recipient's WebSocket topic
        // If recipient is online and subscribed to "/topic/messages/{recipientId}",
        // they'll receive this immediately without polling
        messagingTemplate.convertAndSend("/topic/messages/" + message.getRecipientId(), message);
    }

    /**
     * Retrieve all stored messages for a recipient.
     * 
     * GET /api/messages/{recipientId}
     * 
     * RESPONSE: Array of messages (all ENCRYPTED - client must decrypt them)
     * [
     * {"senderId": "alice", "recipientId": "bob", "content": "encrypted",
     * "timestamp": 123},
     * {...}
     * ]
     * 
     * USE CASES:
     * - Initial load: When Bob opens the app, fetch all pending messages
     * - Offline catch-up: Download messages that arrived while offline
     * - Device sync: New device fetches message history
     * 
     * IMPLEMENTATION:
     * - Query DynamoDB for all messages where recipientId = {recipientId}
     * - Convert DynamoDB entities to DTOs
     * - Return as JSON array (messages still encrypted!)
     */
    @GetMapping("/{recipientId}")
    public List<SignalMessage> getMessages(@PathVariable String recipientId) {
        // Fetch all messages for this recipient from DynamoDB
        return messageRepository.findByRecipientId(recipientId).stream()
                // Convert each DynamoDB entity to a SignalMessage DTO
                .map(entity -> new SignalMessage(
                        entity.getSenderId(),
                        entity.getRecipientId(),
                        entity.getContent(), // Still encrypted!
                        entity.getTimestamp()))
                .collect(Collectors.toList()); // Collect into a List
    }
}
