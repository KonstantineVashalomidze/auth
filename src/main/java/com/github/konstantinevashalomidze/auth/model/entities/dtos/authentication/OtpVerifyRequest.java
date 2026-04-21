package com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication;

import jakarta.validation.constraints.*;

public record OtpVerifyRequest(
        @NotNull @Email
        String email,

        @NotNull @Size(min = 6, max = 6)
        String otp,

        @Size(min = 1, max = 80)
        String displayName,

        @Min(5) @Max(120)
        Integer age,

        @AssertTrue
        Boolean termsAccepted
) {}