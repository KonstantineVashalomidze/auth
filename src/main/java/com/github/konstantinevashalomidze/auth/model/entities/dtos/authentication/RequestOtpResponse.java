package com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication;


import com.github.konstantinevashalomidze.auth.model.entities.enums.AccountStatus;

/**
 * @author Konstantine Vashalomidze
 */
public record RequestOtpResponse(
        AccountStatus accountStatus,

        Long otpExpiresInSeconds,

        String message
) {}