package com.grid07.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Represents a human user of the platform.
 *
 * <p>The {@code is_premium} flag is used by the guardrail engine to apply
 * different rate-limit tiers in future iterations.</p>
 */
@Entity
@Table(
    name = "users",
    indexes = @Index(name = "idx_users_username", columnList = "username", unique = true)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "createdAt")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonProperty("isPremium")
    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
