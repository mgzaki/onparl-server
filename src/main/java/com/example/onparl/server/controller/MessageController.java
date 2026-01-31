package com.example.onparl.server.controller;

import com.example.onparl.server.model.SignalMessageEntity;
import com.example.onparl.server.repository.DynamoDbMessageRepository;
import com.example.onparl.server.model.SignalMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final DynamoDbMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(DynamoDbMessageRepository messageRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public void sendMessage(@RequestBody SignalMessage message) {
        SignalMessageEntity entity = new SignalMessageEntity();
        entity.setRecipientId(message.getRecipientId());
        entity.setSenderId(message.getSenderId());
        entity.setContent(message.getContent());
        entity.setTimestamp(message.getTimestamp());

        messageRepository.save(entity);

        // Push notification to recipient
        messagingTemplate.convertAndSend("/topic/messages/" + message.getRecipientId(), message);
    }

    @GetMapping("/{recipientId}")
    public List<SignalMessage> getMessages(@PathVariable String recipientId) {
        return messageRepository.findByRecipientId(recipientId).stream()
                .map(entity -> new SignalMessage(
                        entity.getSenderId(),
                        entity.getRecipientId(),
                        entity.getContent(),
                        entity.getTimestamp()))
                .collect(Collectors.toList());
    }
}
