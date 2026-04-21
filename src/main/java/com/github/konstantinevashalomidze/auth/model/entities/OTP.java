package com.github.konstantinevashalomidze.auth.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;


@Getter @Setter
@Entity
@Table(name = "otps")
public class OTP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Email
    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    Integer attempts = 0;

    @Column(length = 1000,nullable = false)
    String otpHash;

    @Column(nullable = false)
    Instant expiresAt;

    Instant usedAt;

    @Column(nullable = false)
    @CreationTimestamp
    Instant createdAt;
}