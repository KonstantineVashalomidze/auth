package com.github.konstantinevashalomidze.auth.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.net.InetAddress;
import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    @Column(length = 1000, nullable = false)
    String tokenHash;
    @Column(nullable = false)
    Instant expiresAt;
    Instant revokedAt;
    @Column(nullable = false)
    @CreationTimestamp
    Instant createdAt;
    String userAgent;
    @Column(length = 45)
    InetAddress ipAddress;
}