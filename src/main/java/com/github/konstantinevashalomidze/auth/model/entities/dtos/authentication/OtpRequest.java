package com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * @author Konstantine Vashalomidze
 */
public record OtpRequest(
        @NotNull @Email
        String email
) {}

