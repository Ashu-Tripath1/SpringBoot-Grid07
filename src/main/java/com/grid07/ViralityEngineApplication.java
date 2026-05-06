package com.grid07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Grid07 Virality Engine microservice.
 *
 * <p>This application acts as the central API gateway and guardrail system,
 * providing Redis-backed atomic locks, virality scoring, and a smart
 * notification batching engine.</p>
 */
@SpringBootApplication
@EnableScheduling
public class ViralityEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ViralityEngineApplication.class, args);
    }
}
