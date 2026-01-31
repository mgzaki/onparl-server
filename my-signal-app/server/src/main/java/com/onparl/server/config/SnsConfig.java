package com.onparl.server.config;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * AWS SNS Configuration for SMS sending.
 * 
 * This creates the SnsTemplate bean needed for SMS functionality.
 * In mock mode, it creates a dummy bean to avoid initialization errors.
 */
@Configuration
public class SnsConfig {

    /**
     * Create SnsTemplate bean when NOT in mock mode.
     */
    @Bean
    @ConditionalOnProperty(name = "sms.mock.enabled", havingValue = "false", matchIfMissing = false)
    public SnsTemplate snsTemplate() {
        SnsClient snsClient = SnsClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        return new SnsTemplate(snsClient);
    }

    /**
     * Create a mock/dummy SnsTemplate when in mock mode.
     * This prevents bean creation errors while SMS is mocked.
     */
    @Bean
    @ConditionalOnProperty(name = "sms.mock.enabled", havingValue = "true", matchIfMissing = true)
    public SnsTemplate mockSnsTemplate() {
        // Create a dummy SNS client that won't actually be used in mock mode
        // This is just to satisfy Spring's dependency injection
        SnsClient dummyClient = SnsClient.builder()
                .region(Region.US_EAST_1)
                .build();

        return new SnsTemplate(dummyClient);
    }
}
