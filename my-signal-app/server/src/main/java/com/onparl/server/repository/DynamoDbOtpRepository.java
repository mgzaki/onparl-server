package com.onparl.server.repository;

import com.onparl.server.model.OtpVerification;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

/**
 * Repository for OTP Verification entity operations with DynamoDB.
 * 
 * Manages OTP codes for phone number verification.
 * OTPs are automatically deleted by DynamoDB TTL after expiration.
 */
@Repository
public class DynamoDbOtpRepository {

    private final DynamoDbTable<OtpVerification> otpTable;

    public DynamoDbOtpRepository(DynamoDbEnhancedClient dynamoDbClient) {
        this.otpTable = dynamoDbClient.table("onparl-otp-verifications", TableSchema.fromBean(OtpVerification.class));
    }

    /**
     * Save or update an OTP verification record.
     */
    public void saveOtp(OtpVerification otp) {
        otpTable.putItem(otp);
    }

    /**
     * Find an OTP verification record by phone number.
     */
    public Optional<OtpVerification> findByPhoneNumber(String phoneNumber) {
        Key key = Key.builder()
                .partitionValue(phoneNumber)
                .build();

        OtpVerification otp = otpTable.getItem(key);
        return Optional.ofNullable(otp);
    }

    /**
     * Delete an OTP verification record by phone number.
     * Called after successful verification or when invalidating an OTP.
     */
    public void deleteByPhoneNumber(String phoneNumber) {
        Key key = Key.builder()
                .partitionValue(phoneNumber)
                .build();

        otpTable.deleteItem(key);
    }

    /**
     * Increment the failed attempts counter for an OTP.
     */
    public void incrementAttempts(String phoneNumber) {
        findByPhoneNumber(phoneNumber).ifPresent(otp -> {
            otp.setAttempts(otp.getAttempts() + 1);
            saveOtp(otp);
        });
    }

    /**
     * Check if an OTP exists for a phone number.
     */
    public boolean existsByPhoneNumber(String phoneNumber) {
        return findByPhoneNumber(phoneNumber).isPresent();
    }
}
