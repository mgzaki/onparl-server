package com.onparl.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS CONFIGURATION - Cross-Origin Resource Sharing security.
 * 
 * WHAT IS CORS?
 * - Browser security feature that blocks requests from other domains
 * - Example: If your frontend is at http://localhost:5173 and API is at
 * http://localhost:8080,
 * the browser blocks the request unless CORS headers say it's allowed
 * 
 * WHY DO WE NEED THIS?
 * - Our React frontend (port 5173) needs to call our Spring Boot API (port
 * 8080)
 * - Without CORS configuration, browsers would block all API calls
 * - CORS headers tell the browser "It's okay, this cross-origin request is
 * allowed"
 * 
 * SECURITY WARNING:
 * - This configuration is VERY PERMISSIVE (allows all origins, all headers)
 * - Good for development, but DANGEROUS in production!
 * 
 * FOR PRODUCTION:
 * - Replace .allowedOriginPatterns("*") with specific domain(s)
 * - Example: .allowedOrigins("https://yourdomain.com",
 * "https://app.yourdomain.com")
 * - Be specific with allowed headers and methods
 * - Consider removing .allowCredentials(true) if not using cookies/sessions
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Add CORS mappings for all endpoints.
     * 
     * CONFIGURATION BREAKDOWN:
     * - addMapping("/**") = Apply to all URLs
     * - allowedOriginPatterns("*") = Accept requests from ANY domain (⚠️
     * permissive!)
     * - allowedMethods(...) = Allow these HTTP verbs
     * - allowedHeaders("*") = Accept any request headers
     * - allowCredentials(true) = Allow cookies/auth headers to be sent
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply CORS to all endpoints
                .allowedOriginPatterns("*") // Allow all origins (⚠️ change for production!)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // HTTP methods
                .allowedHeaders("*") // Allow all request headers
                .allowCredentials(true); // Allow cookies and authorization headers
    }
}
