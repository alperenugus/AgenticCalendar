package com.agent.agenticcalendar.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A username/password account for users who sign in without Google OAuth.
 *
 * This is intentionally separate from {@link User} (the legacy calendar-domain
 * entity). Calendar data is owned by an opaque owner key; for a local account
 * that key is {@code "local-" + id}, which flows through the same channel as a
 * Google {@code sub} so event scoping is unchanged.
 */
@Entity
@Table(name = "local_accounts")
public class LocalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public LocalAccount() {
    }

    public LocalAccount(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
