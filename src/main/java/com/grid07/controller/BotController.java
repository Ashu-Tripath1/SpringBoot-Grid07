package com.grid07.controller;

import com.grid07.entity.Bot;
import com.grid07.repository.BotRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Lightweight controller for bot management.
 * Primarily used to seed test data via the Postman collection.
 */
@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotController {

    private final BotRepository botRepository;

    @PostMapping
    public ResponseEntity<Bot> createBot(@Valid @RequestBody Bot bot) {
        return ResponseEntity.status(HttpStatus.CREATED).body(botRepository.save(bot));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bot> getBot(@PathVariable Long id) {
        return botRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
