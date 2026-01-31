package com.onparl.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WEBSOCKET CONFIGURATION - Enables real-time encrypted message delivery.
 * 
 * WHAT IS WEBSOCKET?
 * - A protocol that allows two-way communication between client and server
 * - Unlike HTTP (request-response), WebSocket maintains a persistent connection
 * - Perfect for real-time messaging, notifications, and live updates
 * 
 * STOMP PROTOCOL:
 * - STOMP = Simple Text Oriented Messaging Protocol
 * - Runs on top of WebSocket, adding structure for publish/subscribe patterns
 * - Clients "subscribe" to topics like /topic/messages/alice
 * - Server "publishes" messages to topics, and all subscribers receive them
 * 
 * SOCKJS FALLBACK:
 * - Not all browsers/networks support WebSocket (firewalls, old browsers)
 * - SockJS provides fallbacks: long-polling, iframe streaming, etc.
 * - Transparent to the application - works the same either way
 * 
 * ARCHITECTURE:
 * 1. Client connects to /ws-signal endpoint
 * 2. Client subscribes to /topic/messages/{theirUserId}
 * 3. When someone sends a message, server publishes to recipient's topic
 * 4. Recipient's browser receives it instantly (no polling!)
 */
@Configuration // Spring configuration class
@EnableWebSocketMessageBroker // Enable STOMP over WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker (the pub/sub engine).
     * 
     * SIMPLE IN-MEMORY BROKER:
     * - Good for single-server deployments
     * - Messages only exist in memory (lost on restart)
     * - ALL topics starting with /topic are handled by this broker
     * 
     * FOR PRODUCTION SCALING:
     * - Replace with external broker (RabbitMQ, Redis, Amazon MQ)
     * - Allows multiple server instances to share WebSocket messages
     * - Required for horizontal scaling (see ROADMAP.md)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for /topic/* destinations
        config.enableSimpleBroker("/topic");
        // Messages FROM clients are prefixed with /app (not used in current
        // implementation)
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints (where clients connect).
     * 
     * ENDPOINT: /ws-signal
     * - Clients connect here to establish WebSocket connection
     * - Example: new SockJS('http://localhost:8080/ws-signal')
     * 
     * SECURITY CONSIDERATION:
     * - setAllowedOriginPatterns("*") allows ANY origin (permissive!)
     * - FOR PRODUCTION: Restrict to your frontend domain(s)
     * - Example: .setAllowedOrigins("https://yourdomain.com")
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-signal") // WebSocket endpoint URL
                .setAllowedOriginPatterns("*") // CORS: allow all origins (change for production!)
                .withSockJS(); // Enable SockJS fallback for browsers without WebSocket
    }
}
