package com.example.onparl.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ONPARL SERVER APPLICATION - Main entry point for the Signal Protocol
 * messaging server.
 * 
 * WHAT IS THIS?
 * This is the "main" class that starts the entire Spring Boot application.
 * When you run `java -jar server.jar`, this main() method is invoked.
 * 
 * @SpringBootApplication ANNOTATION:
 *                        This single annotation is actually a combination of
 *                        three:
 * 
 *                        1. @Configuration: Marks this as a source of Spring
 *                        bean definitions
 *                        2. @EnableAutoConfiguration: Tells Spring Boot to
 *                        automatically configure based on:
 *                        - Dependencies in build.gradle (e.g.,
 *                        spring-boot-starter-web → embed Tomcat)
 *                        - Application.properties settings
 *                        - Sensible defaults for common use cases
 * 
 *                        3. @ComponentScan: Automatically finds and registers:
 *                        - @Controller classes (KeyController,
 *                        MessageController)
 *                        - @Repository classes (DynamoDbKeyRepository,
 *                        DynamoDbMessageRepository)
 *                        - @Component classes (TableInitializer)
 *                        - @Configuration classes (WebSocketConfig, CorsConfig)
 *                        - Scans the package: com.example.onparl.server and all
 *                        sub-packages
 * 
 *                        WHAT HAPPENS WHEN THIS RUNS?
 *                        1. Spring Boot initializes (loads configurations,
 *                        creates beans)
 *                        2. Embedded Tomcat server starts on port 8080
 *                        (default)
 *                        3. TableInitializer creates DynamoDB tables (if
 *                        needed)
 *                        4. REST APIs and WebSocket endpoints become available
 *                        5. Application Ready Event fires → server is ready to
 *                        accept requests
 * 
 *                        EMBEDDED SERVER:
 *                        This application includes an embedded Tomcat server
 *                        (from spring-boot-starter-web).
 *                        You DON'T need to install Tomcat separately - just
 *                        run: java -jar server.jar
 */
@SpringBootApplication
public class OnparlServerApplication {

    /**
     * Main entry point - starts the Spring Boot application.
     * 
     * SpringApplication.run() does the heavy lifting:
     * - Creates ApplicationContext (the Spring container)
     * - Scans for components and registers beans
     * - Starts embedded web server (Tomcat)
     * - Initializes all configured services
     * 
     * @param args Command-line arguments (can be used for configuration)
     */
    public static void main(String[] args) {
        SpringApplication.run(OnparlServerApplication.class, args);
    }
}
