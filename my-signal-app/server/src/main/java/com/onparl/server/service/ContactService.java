package com.onparl.server.service;

import com.onparl.server.model.User;
import com.onparl.server.repository.DynamoDbUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing contacts and blocked users.
 */
@Service
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final DynamoDbUserRepository userRepository;

    public ContactService(DynamoDbUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get list of contacts for a user.
     * returns a list of User objects (excluding sensitive info in controller/DTO
     * layer).
     */
    public List<User> getContacts(String userId) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User user = userOpt.get();
        Set<String> contactIds = user.getContacts();

        if (contactIds == null || contactIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch each contact's user details
        // In a real production app with many contacts, this should be done with a
        // BatchGetItem
        // For now, we'll iterate which is fine for small contact lists
        return contactIds.stream()
                .map(userRepository::findByUserId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Add a contact by User ID or Phone Number.
     */
    public User addContact(String userId, String contactIdentifier) {
        logger.info("Adding contact {} for user {}", contactIdentifier, userId);

        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User user = userOpt.get();

        // Find the contact user
        Optional<User> contactUserOpt = userRepository.findByUserId(contactIdentifier);
        if (contactUserOpt.isEmpty()) {
            // Try by phone number (partition key lookup, which matches "id" in User model)
            // Note: User.id is the partition key and holds phone number or email
            // But we might be passed just a phone number string
            contactUserOpt = userRepository.findById(contactIdentifier);
        }

        if (contactUserOpt.isEmpty()) {
            throw new IllegalArgumentException("Contact not found with identifier: " + contactIdentifier);
        }

        User contactUser = contactUserOpt.get();
        if (contactUser.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cannot add yourself as a contact");
        }

        Set<String> contacts = user.getContacts();
        if (contacts == null) {
            contacts = new HashSet<>();
            user.setContacts(contacts);
        }

        contacts.add(contactUser.getUserId());
        userRepository.saveUser(user);

        return contactUser;
    }

    /**
     * Remove a contact.
     */
    public void removeContact(String userId, String contactUserId) {
        logger.info("Removing contact {} for user {}", contactUserId, userId);

        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User user = userOpt.get();

        Set<String> contacts = user.getContacts();
        if (contacts != null && contacts.remove(contactUserId)) {
            userRepository.saveUser(user);
        }
    }

    /**
     * Get list of blocked users.
     */
    public List<User> getBlockedUsers(String userId) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        User user = userOpt.get();
        Set<String> blockedIds = user.getBlockedUsers();

        if (blockedIds == null || blockedIds.isEmpty()) {
            return Collections.emptyList();
        }

        return blockedIds.stream()
                .map(userRepository::findByUserId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Block a user.
     */
    public User blockUser(String userId, String blockUserId) {
        logger.info("Blocking user {} for user {}", blockUserId, userId);

        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User user = userOpt.get();

        // Verify the user to block exists
        Optional<User> blockUserOpt = userRepository.findByUserId(blockUserId);
        if (blockUserOpt.isEmpty()) {
            throw new IllegalArgumentException("User to block not found: " + blockUserId);
        }

        if (userId.equals(blockUserId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        Set<String> blockedUsers = user.getBlockedUsers();
        if (blockedUsers == null) {
            blockedUsers = new HashSet<>();
            user.setBlockedUsers(blockedUsers);
        }

        blockedUsers.add(blockUserId);
        userRepository.saveUser(user);

        return blockUserOpt.get();
    }

    /**
     * Unblock a user.
     */
    public void unblockUser(String userId, String blockUserId) {
        logger.info("Unblocking user {} for user {}", blockUserId, userId);

        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User user = userOpt.get();

        Set<String> blockedUsers = user.getBlockedUsers();
        if (blockedUsers != null && blockedUsers.remove(blockUserId)) {
            userRepository.saveUser(user);
        }
    }
}
