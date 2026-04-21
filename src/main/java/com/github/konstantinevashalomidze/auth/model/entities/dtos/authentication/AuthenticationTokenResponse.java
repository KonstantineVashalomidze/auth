package com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.konstantinevashalomidze.auth.model.entities.enums.AccountStatus;

public record AuthenticationTokenResponse(
        Long userId,

        AccountStatus accountStatus,
        @JsonIgnore String accessToken,
        @JsonIgnore String refreshToken,
        @JsonIgnore Long refreshTokenExpirationSeconds,
        @JsonIgnore Long accessTokenExpirationSeconds
) {}
