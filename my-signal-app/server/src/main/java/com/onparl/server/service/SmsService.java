package com.onparl.server.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS messages via AWS SNS.
 * 
 * Handles:
 * - SMS delivery for OTP codes
 * - Phone number validation and formatting
 * - Mock mode for local development (no actual SMS sent)
 * - Error handling and logging
 */
@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    private final SnsTemplate snsTemplate;
    private final PhoneNumberUtil phoneNumberUtil;

    @Value("${sms.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${sms.sender.id:Onparl}")
    private String senderId;

    public SmsService(SnsTemplate snsTemplate) {
        this.snsTemplate = snsTemplate;
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
    }

    /**
     * Send an OTP code via SMS.
     * 
     * @param phoneNumber Phone number in E.164 format (+1234567890)
     * @param otp         The 6-digit OTP code
     * @throws IllegalArgumentException if phone number is invalid
     */
    public void sendOtp(String phoneNumber, String otp) {
        // Validate phone number
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }

        // Format the SMS message
        String message = String.format(
                "Your Onparl verification code is: %s\n\nDo not share this code with anyone.\n\nExpires in 5 minutes.",
                otp);

        // Mock mode for local development
        if (mockEnabled) {
            logger.info("📱 MOCK SMS to {}: {}", maskPhoneNumber(phoneNumber), otp);
            System.out.println("========================================");
            System.out.println("📱 SMS MOCK MODE");
            System.out.println("To: " + phoneNumber);
            System.out.println("Message: " + message);
            System.out.println("========================================");
            return;
        }

        try {
            // Send SMS via AWS SNS
            snsTemplate.sendNotification(phoneNumber, message, "Onparl OTP Verification");
            logger.info("✅ SMS sent successfully to {}", maskPhoneNumber(phoneNumber));
        } catch (Exception e) {
            logger.error("❌ Failed to send SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            throw new RuntimeException("Failed to send SMS. Please try again later.", e);
        }
    }

    /**
     * Validate a phone number using libphonenumber.
     * 
     * @param phoneNumber Phone number to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        try {
            // Parse the phone number
            PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);

            // Validate it
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException e) {
            logger.warn("Invalid phone number format: {}", phoneNumber);
            return false;
        }
    }

    /**
     * Format a phone number to E.164 format.
     * 
     * @param phoneNumber   Phone number in any format
     * @param defaultRegion Default region code (e.g., "US")
     * @return Phone number in E.164 format (+1234567890)
     * @throws IllegalArgumentException if phone number cannot be parsed
     */
    public String formatToE164(String phoneNumber, String defaultRegion) {
        try {
            PhoneNumber number = phoneNumberUtil.parse(phoneNumber, defaultRegion);
            return phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Cannot parse phone number: " + phoneNumber, e);
        }
    }

    /**
     * Mask a phone number for logging (security).
     * Example: +14155552671 -> +1415555****
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }

        int visibleDigits = 7; // Show country code + first few digits
        String visible = phoneNumber.substring(0, Math.min(phoneNumber.length(), visibleDigits));
        return visible + "****";
    }

    /**
     * Check if SMS service is in mock mode.
     */
    public boolean isMockMode() {
        return mockEnabled;
    }
}
