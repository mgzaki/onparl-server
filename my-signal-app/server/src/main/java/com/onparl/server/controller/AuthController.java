package com.onparl.server.controller;

import com.onparl.server.model.User;
import com.onparl.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for authentication (Phone/SMS and Email).
 * 
 * Endpoints:
 * - POST /api/auth/request-otp: Request an OTP
 * - POST /api/auth/verify-otp: Verify OTP and authenticate user
 * - POST /api/auth/resend-otp: Resend OTP
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${onparl.auth.dev-mode:false}")
    private boolean devMode;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Dev Login: Bypass OTP.
     * Only enabled if onparl.auth.dev-mode=true
     */
    @PostMapping("/dev-login")
    public ResponseEntity<Map<String, Object>> devLogin(@RequestBody Map<String, String> request) {
        if (!devMode) {
            return errorResponse("Dev mode is disabled.", HttpStatus.FORBIDDEN);
        }

        String identifier = request.get("identifier");
        String type = request.get("type");

        if (identifier == null || identifier.trim().isEmpty()) {
            return errorResponse("Identifier is required", HttpStatus.BAD_REQUEST);
        }
        if (type == null) {
            type = identifier.contains("@") ? "EMAIL" : "PHONE";
        }

        try {
            User user = authService.devLogin(identifier, type);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", user.getUserId());
            response.put("identifier", user.getId());
            response.put("isNewUser", false); // Assume verified for dev
            response.put("displayName", user.getDisplayName());
            response.put("profileComplete", user.isProfileComplete());
            response.put("devMode", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Dev login failed", e);
            return errorResponse("Dev login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Request an OTP to be sent to the identifier (phone or email).
     * 
     * Body: { "identifier": "+1415...", "type": "PHONE" }
     * OR: { "identifier": "user@example.com", "type": "EMAIL" }
     */
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, Object>> requestOtp(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String type = request.get("type"); // PHONE or EMAIL

        // Backward compatibility for old clients sending "phoneNumber"
        if (identifier == null && request.containsKey("phoneNumber")) {
            identifier = request.get("phoneNumber");
            type = "PHONE";
        }

        if (identifier == null || identifier.trim().isEmpty()) {
            return errorResponse("Identifier (phone or email) is required", HttpStatus.BAD_REQUEST);
        }

        if (type == null || type.trim().isEmpty()) {
            return errorResponse("Authentication type (PHONE or EMAIL) is required", HttpStatus.BAD_REQUEST);
        }

        try {
            authService.requestOtp(identifier, type);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP sent to " + maskIdentifier(identifier));
            response.put("expiresIn", 300); // 5 minutes

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid identifier: {}", identifier);
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (IllegalStateException e) {
            logger.warn("Rate limit exceeded for: {}", identifier);
            return errorResponse(e.getMessage(), HttpStatus.TOO_MANY_REQUESTS);

        } catch (Exception e) {
            logger.error("Error sending OTP to: {}", identifier, e);
            return errorResponse("Failed to send OTP. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify OTP and authenticate the user.
     * 
     * Body: { "identifier": "...", "otp": "...", "type": "..." }
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String otp = request.get("otp");
        String type = request.get("type");

        // Backward compatibility
        if (identifier == null && request.containsKey("phoneNumber")) {
            identifier = request.get("phoneNumber");
            type = "PHONE";
        }

        if (identifier == null || identifier.trim().isEmpty()) {
            return errorResponse("Identifier is required", HttpStatus.BAD_REQUEST);
        }

        if (otp == null || otp.trim().isEmpty()) {
            return errorResponse("OTP is required", HttpStatus.BAD_REQUEST);
        }

        try {
            // Check if this is a new user (before verification) - check if generic ID
            // exists
            boolean isNewUser = authService.getUserByIdentifier(identifier).isEmpty();

            // Verify OTP and authenticate
            User user = authService.verifyOtpAndLogin(identifier, otp, type);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", user.getUserId());
            response.put("identifier", user.getId());

            // Return specific fields based on type
            if (user.getPhoneNumber() != null)
                response.put("phoneNumber", user.getPhoneNumber());
            if (user.getEmail() != null)
                response.put("email", user.getEmail());

            response.put("isNewUser", isNewUser);
            response.put("displayName", user.getDisplayName());
            response.put("profileComplete", user.isProfileComplete());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid OTP for: {}", maskIdentifier(identifier));
            return errorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);

        } catch (Exception e) {
            logger.error("Error verifying OTP for: {}", identifier, e);
            return errorResponse("Authentication failed. " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Resend OTP.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> request) {
        return requestOtp(request);
    }

    /**
     * Get authentication status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@RequestParam String identifier) {
        try {
            long expirySeconds = authService.getOtpExpirySeconds(identifier);
            boolean hasActiveOtp = expirySeconds > 0;

            Map<String, Object> response = new HashMap<>();
            response.put("hasActiveOtp", hasActiveOtp);
            response.put("expirySeconds", expirySeconds);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting auth status", e);
            return errorResponse("Failed to get status", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "****";
        }
        return identifier.substring(0, Math.min(identifier.length(), 3)) + "****";
    }
}
