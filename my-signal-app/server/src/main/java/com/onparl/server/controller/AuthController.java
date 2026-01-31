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
 * REST API Controller for phone-based authentication.
 * 
 * Endpoints:
 * - POST /api/auth/request-otp: Request an OTP to be sent via SMS
 * - POST /api/auth/verify-otp: Verify OTP and authenticate user
 * - POST /api/auth/resend-otp: Resend OTP (same as request-otp with rate
 * limits)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Request an OTP to be sent to the phone number.
     * 
     * POST /api/auth/request-otp
     * Body: { "phoneNumber": "+14155552671" }
     * 
     * Success Response (200):
     * {
     * "success": true,
     * "message": "OTP sent to +1415555****",
     * "expiresIn": 300
     * }
     * 
     * Error Responses:
     * - 400: Invalid phone number
     * - 429: Rate limit exceeded
     * - 500: SMS delivery failure
     */
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, Object>> requestOtp(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return errorResponse("Phone number is required", HttpStatus.BAD_REQUEST);
        }

        try {
            authService.requestOtp(phoneNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP sent to " + maskPhoneNumber(phoneNumber));
            response.put("expiresIn", 300); // 5 minutes

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid phone number: {}", phoneNumber);
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (IllegalStateException e) {
            logger.warn("Rate limit exceeded for: {}", phoneNumber);
            return errorResponse(e.getMessage(), HttpStatus.TOO_MANY_REQUESTS);

        } catch (Exception e) {
            logger.error("Error sending OTP to: {}", phoneNumber, e);
            return errorResponse("Failed to send OTP. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify OTP and authenticate the user.
     * 
     * POST /api/auth/verify-otp
     * Body: { "phoneNumber": "+14155552671", "otp": "123456" }
     * 
     * Success Response (200):
     * {
     * "success": true,
     * "userId": "user_abc123",
     * "phoneNumber": "+14155552671",
     * "isNewUser": false
     * }
     * 
     * Error Responses:
     * - 400: Missing fields
     * - 401: Invalid or expired OTP
     * - 500: Server error
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String otp = request.get("otp");

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return errorResponse("Phone number is required", HttpStatus.BAD_REQUEST);
        }

        if (otp == null || otp.trim().isEmpty()) {
            return errorResponse("OTP is required", HttpStatus.BAD_REQUEST);
        }

        try {
            // Check if this is a new user (before verification)
            boolean isNewUser = authService.getUserByPhoneNumber(phoneNumber).isEmpty();

            // Verify OTP and authenticate
            User user = authService.verifyOtpAndLogin(phoneNumber, otp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", user.getUserId());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("isNewUser", isNewUser);
            response.put("displayName", user.getDisplayName());
            response.put("profileComplete", user.isProfileComplete());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid OTP for phone: {}", maskPhoneNumber(phoneNumber));
            return errorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED);

        } catch (Exception e) {
            logger.error("Error verifying OTP for: {}", phoneNumber, e);
            return errorResponse("Authentication failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Resend OTP (same as request-otp, with rate limiting).
     * 
     * POST /api/auth/resend-otp
     * Body: { "phoneNumber": "+14155552671" }
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> request) {
        // Resend is the same as requesting a new OTP
        return requestOtp(request);
    }

    /**
     * Get authentication status for a phone number (for debugging).
     * 
     * GET /api/auth/status?phoneNumber=+14155552671
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@RequestParam String phoneNumber) {
        try {
            long expirySeconds = authService.getOtpExpirySeconds(phoneNumber);
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

    /**
     * Helper method to create error responses.
     */
    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Mask phone number for logging and responses.
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }

        int visibleDigits = 7;
        String visible = phoneNumber.substring(0, Math.min(phoneNumber.length(), visibleDigits));
        return visible + "****";
    }
}
