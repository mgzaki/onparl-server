package com.onparl.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configuration for AWS S3 client used for profile picture storage.
 */
@Configuration
public class S3Config {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.s3.endpoint:#{null}}")
    private String endpoint;

    /**
     * Creates S3 client bean.
     * 
     * For local development with LocalStack, set
     * aws.s3.endpoint=http://localhost:4566
     * For production, endpoint will be null and use default AWS S3.
     */
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region));

        // Use custom endpoint for LocalStack in development
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(java.net.URI.create(endpoint));
        }

        return builder.build();
    }
}
