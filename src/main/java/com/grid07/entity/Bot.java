package com.grid07.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Represents an AI bot participant on the platform.
 *
 * <p>Each bot has a {@code persona_description} that characterises its
 * behaviour. The guardrail engine uses the bot's {@code id} to enforce
 * per-bot cooldown caps stored in Redis.</p>
 */
@Entity
@Table(
    name = "bots",
    indexes = @Index(name = "idx_bots_name", columnList = "name")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "createdAt")
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 500)
    @Column(name = "persona_description", nullable = false, length = 500)
    private String personaDescription;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
