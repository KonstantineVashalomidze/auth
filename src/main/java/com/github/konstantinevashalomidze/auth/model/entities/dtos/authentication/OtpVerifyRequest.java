package com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication;

import jakarta.validation.constraints.*;

public record OtpVerifyRequest(
        @NotNull @Email
        String email,

        @NotNull @Size(min = 6, max = 6)
        String otp

) {}


