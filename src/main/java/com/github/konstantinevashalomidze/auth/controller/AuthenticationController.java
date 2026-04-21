package com.github.konstantinevashalomidze.auth.controller;

import com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication.AuthenticationTokenResponse;
import com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication.OtpRequest;
import com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication.OtpVerifyRequest;
import com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication.RequestOtpResponse;
import com.github.konstantinevashalomidze.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequestMapping("/api/v1/authentication")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Value("${spring.application.customConfigs.constants.domainName}")
    private String domainName;

    @PostMapping("/otp/request")
    public ResponseEntity<RequestOtpResponse> requestOtp(
            @RequestBody @Valid OtpRequest otpRequest) {
        RequestOtpResponse requestOtpResponse =
                authenticationService.requestOtp(otpRequest.email().trim().toLowerCase());
        return new ResponseEntity<>(requestOtpResponse, HttpStatus.OK);
    }



    @PostMapping("/otp/verify")
    public ResponseEntity<AuthenticationTokenResponse> verifyOtp(
            @RequestBody @Valid OtpVerifyRequest otpVerifyRequest) {
        AuthenticationTokenResponse authenticationTokenResponse
                = authenticationService.verifyOtp(
                otpVerifyRequest.email().trim().toLowerCase(),
                otpVerifyRequest.otp().trim()
        );

        ResponseCookie refreshCookie = ResponseCookie.from("Refresh-Token", authenticationTokenResponse.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/authentication")
                .sameSite("Lax")
                .domain(domainName)
                .maxAge(Duration.ofSeconds(authenticationTokenResponse.refreshTokenExpirationSeconds()))
                .build();

        ResponseCookie accessCookie = ResponseCookie.from("Access-Token", authenticationTokenResponse.accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .domain(domainName)
                .maxAge(Duration.ofSeconds(authenticationTokenResponse.accessTokenExpirationSeconds()))
                .build();



        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return new ResponseEntity<>(authenticationTokenResponse, headers, HttpStatus.OK);
    }



    @PostMapping("/token/refresh")
    public ResponseEntity<Void> refreshToken(@CookieValue("Refresh-Token") String token) {
        AuthenticationTokenResponse tokens = authenticationService.refreshToken(token);

        ResponseCookie newAccess = ResponseCookie.from("Access-Token", tokens.accessToken())
                .httpOnly(true).secure(true).path("/").sameSite("Lax").domain(domainName)
                .maxAge(Duration.ofSeconds(tokens.accessTokenExpirationSeconds())).build();

        ResponseCookie newRefresh = ResponseCookie.from("Refresh-Token", tokens.refreshToken())
                .httpOnly(true).secure(true).path("/api/v1/authentication")
                .sameSite("Lax").domain(domainName)
                .maxAge(Duration.ofSeconds(tokens.refreshTokenExpirationSeconds())).build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, newAccess.toString())
                .header(HttpHeaders.SET_COOKIE, newRefresh.toString())
                .build();
    }



    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "Refresh-Token") String token) {
        authenticationService.logout(token);

        ResponseCookie clearRefresh = ResponseCookie.from("Refresh-Token", "")
                .httpOnly(true).secure(true).path("/api/v1/authentication")
                .maxAge(0).build();

        ResponseCookie clearAccess = ResponseCookie.from("Access-Token", "")
                .httpOnly(true).secure(true).path("/")
                .maxAge(0).build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearRefresh.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearAccess.toString());

        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
    }
}