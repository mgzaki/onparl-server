package com.onparl.server.controller;

import com.onparl.server.model.User;
import com.onparl.server.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/{userId}")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Get contacts for a user.
     */
    @GetMapping("/contacts")
    public ResponseEntity<Map<String, Object>> getContacts(@PathVariable String userId) {
        try {
            List<User> contacts = contactService.getContacts(userId);
            List<Map<String, Object>> contactDtos = contacts.stream()
                    .map(this::buildUserDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("contacts", contactDtos);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching contacts for user {}", userId, e);
            return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Add a contact.
     * Body: { "contactIdentifier": "userId OR phoneNumber" }
     */
    @PostMapping("/contacts")
    public ResponseEntity<Map<String, Object>> addContact(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String contactIdentifier = request.get("contactIdentifier");
        if (contactIdentifier == null || contactIdentifier.trim().isEmpty()) {
            return errorResponse("contactIdentifier is required", HttpStatus.BAD_REQUEST);
        }

        try {
            User contact = contactService.addContact(userId, contactIdentifier);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact added successfully");
            response.put("contact", buildUserDto(contact));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error adding contact for user {}", userId, e);
            return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove a contact.
     */
    @DeleteMapping("/contacts/{contactUserId}")
    public ResponseEntity<Map<String, Object>> removeContact(
            @PathVariable String userId,
            @PathVariable String contactUserId) {

        try {
            contactService.removeContact(userId, contactUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact removed successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error removing contact for user {}", userId, e);
            return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get blocked users.
     */
    @GetMapping("/blocked")
    public ResponseEntity<Map<String, Object>> getBlockedUsers(@PathVariable String userId) {
        try {
            List<User> blockedUsers = contactService.getBlockedUsers(userId);
            List<Map<String, Object>> blockedDtos = blockedUsers.stream()
                    .map(this::buildUserDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("blockedUsers", blockedDtos);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching blocked users for user {}", userId, e);
            return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Block a user.
     * Body: { "blockUserId": "userId" }
     */
    @PostMapping("/blocked")
    public ResponseEntity<Map<String, Object>> blockUser(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String blockUserId = request.get("blockUserId");
        if (blockUserId == null || blockUserId.trim().isEmpty()) {
            return errorResponse("blockUserId is required", HttpStatus.BAD_REQUEST);
        }

        try {
            User blockedUser = contactService.blockUser(userId, blockUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User blocked successfully");
            response.put("blockedUser", buildUserDto(blockedUser));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error blocking user for user {}", userId, e);
            return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Unblock a user.
     */
    @DeleteMapping("/blocked/{blockUserId}")
    public ResponseEntity<Map<String, Object>> unblockUser(
            @PathVariable String userId,
            @PathVariable String blockUserId) {

        try {
            contactService.unblockUser(userId, blockUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User unblocked successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error unblocking user for user {}", userId, e);
            return errorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> buildUserDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("userId", user.getUserId());
        dto.put("displayName", user.getDisplayName());
        dto.put("profilePicture", user.getProfilePicture());
        dto.put("mood", user.getMood());
        dto.put("phoneNumber", maskPhoneNumber(user.getPhoneNumber())); // Mask phone for privacy
        return dto;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4).replaceAll(".", "*")
                + phoneNumber.substring(phoneNumber.length() - 4);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return new ResponseEntity<>(response, status);
    }
}
