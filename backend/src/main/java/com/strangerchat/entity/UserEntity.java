package com.strangerchat.entity;

import com.strangerchat.dto.Gender;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an anonymous participant. Phase 1 has no login/auth -
 * a row is created (or touched) the first time a browser connects,
 * keyed by the client-generated/handshake-assigned userId.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private String id; // matches the WS Principal / userId, not a DB-generated UUID

    @Column(nullable = false)
    private Instant firstSeen;

    @Column(nullable = false)
    private Instant lastSeen;

    private boolean banned = false;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    public UserEntity() {}

    public UserEntity(String id) {
        this.id = id;
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Instant firstSeen) { this.firstSeen = firstSeen; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
}
