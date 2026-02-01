package com.onparl.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for sending emails.
 * 
 * Handles:
 * - Email delivery for OTP codes using AWS SES
 * - Email validation
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // Simple email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final JavaMailSender mailSender;

    @Value("${onparl.auth.email.sender:noreply@onparl.com}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send an OTP code via Email.
     * 
     * @param email The recipient email address
     * @param otp   The 6-digit OTP code
     */
    public void sendOtp(String email, String otp) {
        // Validate email
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject("Onparl OTP Verification");
            message.setText(String.format(
                    "Your Onparl verification code is: %s\n\nDo not share this code with anyone.\n\nExpires in 5 minutes.",
                    otp));

            mailSender.send(message);
            logger.info("Email sent to {} via SES", email);

        } catch (Exception e) {
            logger.error("Failed to send email to {}", email, e);
            // Fallback mock logging for dev/debugging if SES fails (e.g. sandbox issues)
            logger.warn("FALLBACK MOCK EMAIL to {}: {}", email, otp);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Validate an email address.
     * 
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
