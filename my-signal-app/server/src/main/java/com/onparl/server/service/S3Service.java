package com.onparl.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Base64;
import java.util.UUID;

/**
 * Service for managing profile pictures in AWS S3.
 * 
 * Handles upload, deletion, and URL generation for profile pictures.
 * Images are stored in S3 with public-read access for easy retrieval.
 */
@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = { "image/png", "image/jpeg", "image/jpg" };

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.profile-pictures-prefix:profile-pictures/}")
    private String profilePicturesPrefix;

    @Value("${aws.region:us-east-1}")
    private String region;

    /**
     * Upload a profile picture to S3.
     * 
     * @param userId      User ID (used in filename)
     * @param base64Image Base64 data URI (data:image/png;base64,...)
     * @return S3 URL of uploaded image
     * @throws IllegalArgumentException if image is invalid
     */
    public String uploadProfilePicture(String userId, String base64Image) {
        logger.info("Uploading profile picture for user: {}", userId);

        // Extract image data from data URI
        ImageData imageData = extractImageData(base64Image);

        // Validate image
        validateImage(imageData);

        // Generate unique filename
        String fileName = generateFileName(userId, imageData.contentType);
        String key = profilePicturesPrefix + fileName;

        try {
            // Upload to S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(imageData.contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(imageData.data));

            // Generate public URL
            String url = generatePublicUrl(key);

            logger.info("Profile picture uploaded successfully: {}", url);
            return url;

        } catch (Exception e) {
            logger.error("Failed to upload profile picture to S3", e);
            throw new RuntimeException("Failed to upload profile picture: " + e.getMessage());
        }
    }

    /**
     * Delete a profile picture from S3.
     * 
     * @param s3Url S3 URL of the image to delete
     */
    public void deleteProfilePicture(String s3Url) {
        if (s3Url == null || s3Url.isEmpty()) {
            return;
        }

        try {
            // Extract key from URL
            String key = extractKeyFromUrl(s3Url);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            logger.info("Profile picture deleted from S3: {}", key);

        } catch (Exception e) {
            logger.warn("Failed to delete profile picture from S3: {}", s3Url, e);
            // Don't throw exception - deletion failure shouldn't block the operation
        }
    }

    /**
     * Extract image data from Base64 data URI.
     * 
     * Format: data:image/png;base64,iVBORw0KGgoAAAANS...
     */
    private ImageData extractImageData(String dataUri) {
        if (dataUri == null || !dataUri.startsWith("data:image/")) {
            throw new IllegalArgumentException("Invalid image data URI format");
        }

        try {
            // Split into metadata and data
            String[] parts = dataUri.split(",", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid data URI structure");
            }

            // Extract content type
            String metadata = parts[0]; // data:image/png;base64
            String base64Data = parts[1];

            String contentType = metadata
                    .replace("data:", "")
                    .replace(";base64", "")
                    .trim();

            // Decode Base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            return new ImageData(imageBytes, contentType);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode image data: " + e.getMessage());
        }
    }

    /**
     * Validate image size and type.
     */
    private void validateImage(ImageData imageData) {
        // Check size
        if (imageData.data.length > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Image too large. Maximum size is " + (MAX_IMAGE_SIZE / 1024 / 1024) + "MB");
        }

        // Check type
        boolean validType = false;
        for (String allowedType : ALLOWED_TYPES) {
            if (imageData.contentType.toLowerCase().equals(allowedType)) {
                validType = true;
                break;
            }
        }

        if (!validType) {
            throw new IllegalArgumentException(
                    "Invalid image type. Only PNG and JPG are allowed");
        }
    }

    /**
     * Generate unique filename for profile picture.
     * 
     * Format: {userId}_{timestamp}_{uuid}.{ext}
     */
    private String generateFileName(String userId, String contentType) {
        String extension = getFileExtension(contentType);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s_%s_%s.%s", userId, timestamp, uuid, extension);
    }

    /**
     * Get file extension from content type.
     */
    private String getFileExtension(String contentType) {
        switch (contentType.toLowerCase()) {
            case "image/png":
                return "png";
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            default:
                return "jpg";
        }
    }

    /**
     * Generate public S3 URL for a key.
     */
    private String generatePublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, key);
    }

    /**
     * Extract S3 key from full URL.
     * 
     * Example: https://bucket.s3.region.amazonaws.com/profile-pictures/file.png
     * Returns: profile-pictures/file.png
     */
    private String extractKeyFromUrl(String url) {
        // Handle both formats:
        // https://bucket.s3.region.amazonaws.com/key
        // https://s3.region.amazonaws.com/bucket/key

        if (url.contains(".s3.")) {
            // Format: https://bucket.s3.region.amazonaws.com/key
            int keyStart = url.indexOf(".com/") + 5;
            if (keyStart > 4) {
                return url.substring(keyStart);
            }
        } else if (url.contains("s3.")) {
            // Format: https://s3.region.amazonaws.com/bucket/key
            String[] parts = url.split("/");
            if (parts.length >= 5) {
                // Reconstruct key from parts after bucket
                return String.join("/", java.util.Arrays.copyOfRange(parts, 4, parts.length));
            }
        }

        throw new IllegalArgumentException("Cannot extract key from URL: " + url);
    }

    /**
     * Inner class to hold image data and metadata.
     */
    private static class ImageData {
        final byte[] data;
        final String contentType;

        ImageData(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }
    }
}
