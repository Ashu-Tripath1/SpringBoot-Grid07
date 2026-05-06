package com.grid07.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root endpoint providing a self-describing API info response.
 *
 * <p>Replaces the default Spring Boot Whitelabel Error Page on GET /,
 * giving API consumers an immediate overview of available endpoints.</p>
 */
@RestController
public class ApiInfoController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "Grid07 Virality Engine");
        info.put("version", "1.0.0");
        info.put("status", "UP");
        info.put("timestamp", Instant.now().toString());
        info.put("description",
                "Spring Boot microservice acting as the central API gateway and guardrail system " +
                "for a social platform with AI bot participants.");

        Map<String, Object> endpoints = new LinkedHashMap<>();

        endpoints.put("users", Map.of(
                "POST /api/users",   "Create a human user",
                "GET  /api/users/{id}", "Get a user by ID"
        ));
        endpoints.put("bots", Map.of(
                "POST /api/bots",    "Register a bot",
                "GET  /api/bots/{id}", "Get a bot by ID"
        ));
        endpoints.put("posts", Map.of(
                "POST /api/posts",              "Create a post (by user or bot)",
                "GET  /api/posts/{id}",         "Get post + live virality score",
                "POST /api/posts/{id}/comments","Add a comment (guardrails apply for bots)",
                "POST /api/posts/{id}/like",    "Human likes a post (+20 virality)"
        ));
        endpoints.put("tools", Map.of(
                "GET  /h2-console",          "H2 database browser UI",
                "GET  /actuator/health",     "Application health check",
                "GET  /actuator/metrics",    "Application metrics"
        ));
        info.put("endpoints", endpoints);

        Map<String, Object> guardrails = new LinkedHashMap<>();
        guardrails.put("verticalCap",   "Bot comment depth ≤ 20 → HTTP 429 VERTICAL_CAP_EXCEEDED");
        guardrails.put("cooldownCap",   "Bot can interact with same human once per 10 min → HTTP 429 COOLDOWN_CAP_ACTIVE");
        guardrails.put("horizontalCap", "Max 100 bot replies per post (atomic Lua) → HTTP 429 HORIZONTAL_CAP_EXCEEDED");
        info.put("redisGuardrails", guardrails);

        Map<String, Object> virality = new LinkedHashMap<>();
        virality.put("humanLike",    "+20 points");
        virality.put("humanComment", "+50 points");
        virality.put("botReply",     "+1 point");
        info.put("viralityScoring", virality);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}

