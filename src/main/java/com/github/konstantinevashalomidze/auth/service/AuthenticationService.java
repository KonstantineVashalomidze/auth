package com.github.konstantinevashalomidze.auth.service;

import com.github.konstantinevashalomidze.auth.model.entities.OTP;
import com.github.konstantinevashalomidze.auth.model.entities.RefreshToken;
import com.github.konstantinevashalomidze.auth.model.entities.User;
import com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication.AuthenticationTokenResponse;
import com.github.konstantinevashalomidze.auth.model.entities.dtos.authentication.RequestOtpResponse;
import com.github.konstantinevashalomidze.auth.model.entities.enums.AccountStatus;
import com.github.konstantinevashalomidze.auth.repository.OTPRepository;
import com.github.konstantinevashalomidze.auth.repository.RefreshTokenRepository;
import com.github.konstantinevashalomidze.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Konstantine Vashalomidze
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final OTPRepository otpRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${spring.application.customConfigs.constants.otpTimeToLiveSeconds}")
    private Long OTP_TIME_TO_LIVE_SECONDS;
    @Value("${spring.application.customConfigs.constants.applicationEmail}")
    private String APPLICATION_EMAIL;
    @Value("${spring.application.customConfigs.constants.otpMaxAttempts}")
    private Integer OTP_MAX_ATTEMPTS;
    @Value("${spring.application.customConfigs.constants.refreshTokenExpirationSeconds}")
    private Long REFRESH_TOKEN_EXPIRATION_SECONDS;
    @Value("${spring.application.customConfigs.constants.accessTokenExpirationSeconds}")
    private Long ACCESS_TOKEN_EXPIRATION_SECONDS;

    @Transactional
    public RequestOtpResponse requestOtp(String email) {
        // Invalidate any existing valid OTP so there is at most one live code at a time.
        Optional<OTP> existingOpt = otpRepository.findFirstByEmailAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(email, Instant.now());
        existingOpt.ifPresent(existing -> {
            existing.setUsedAt(Instant.now()); // mark consumed so it fails future lookups
            otpRepository.save(existing);
        });

        String plainOtp = generateOtp();
        String otpHash = bCryptPasswordEncoder.encode(plainOtp);

        OTP otp = new OTP();
        otp.setEmail(email);
        otp.setOtpHash(otpHash);
        otp.setExpiresAt(Instant.now().plusSeconds(OTP_TIME_TO_LIVE_SECONDS));
        otpRepository.save(otp);

        emailService.sendOtp(email, plainOtp);

        AccountStatus status = userRepository.existsByEmail(email) ? AccountStatus.EXISTING : AccountStatus.NEW;
        return new RequestOtpResponse(status, OTP_TIME_TO_LIVE_SECONDS,
                "Please check your email, password is valid for only 5 mins. (if you didn't see email please check spam folder)");
    }

    @Transactional
    public AuthenticationTokenResponse verifyOtp(String email, String submittedOtp, Integer age, String displayName, Boolean termsAccepted) {
        OTP otp = otpRepository.findFirstByEmailAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(email, Instant.now())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No valid OTP found for this email. Please request a new code."
                ));

        otp.setAttempts(otp.getAttempts() + 1);

        if (otp.getAttempts() > OTP_MAX_ATTEMPTS) {
            otpRepository.save(otp);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Maximum number attempts exceeded. Please request a new code."
            );
        }

        boolean matches = bCryptPasswordEncoder.matches(submittedOtp, otp.getOtpHash());
        if (!matches) {
            otpRepository.save(otp);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Incorrect OTP. Please try again."
            );
        }

        // Mark the OTP as consumed so it cannot be reused.
        otp.setUsedAt(Instant.now());
        otpRepository.save(otp);

        // Resolve or create the user
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(email);
        User user;
        AccountStatus accountStatus;
        if (userOpt.isEmpty()) {
             user = registerNewUser(email, age, displayName, termsAccepted);
             accountStatus = AccountStatus.NEW;
        } else {
             user = userOpt.get();
             accountStatus = AccountStatus.EXISTING;
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        return new AuthenticationTokenResponse(user.getId(),
                accountStatus, accessToken, refreshToken,
                REFRESH_TOKEN_EXPIRATION_SECONDS, ACCESS_TOKEN_EXPIRATION_SECONDS);
    }

    @Transactional
    public AuthenticationTokenResponse refreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        System.out.println("RECEIVED raw: " + rawToken);
        System.out.println("RECEIVED hash: " + hashToken(rawToken));
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token not recognised."
                ));
        
        // Reuse-attack detection: if already revoked, revoke everything for this user.
        if (stored.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), Instant.now());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token has already been used. All sessions have been invalidated for security."
            );
        }
        
        if (Instant.now().isAfter(stored.getExpiresAt())) {
            stored.setRevokedAt(Instant.now());
            refreshTokenRepository.save(stored);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token has expired. Please sign in again.");
        }

        // Revoke old token.
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = issueRefreshToken(user);
        return new AuthenticationTokenResponse(
                user.getId(),
                AccountStatus.EXISTING,
                newAccessToken,
                newRefreshToken,
                REFRESH_TOKEN_EXPIRATION_SECONDS,
                ACCESS_TOKEN_EXPIRATION_SECONDS
        );
    }

    @Transactional
    public void logout(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token not recognised."));


        if (stored.getRevokedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Already logged out.");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

    }


    private User registerNewUser(String email, Integer age, String displayName, Boolean termsAccepted) {
        if (age == null) {
            throw new IllegalArgumentException("Age is required when creating new account.");
        }

        if (termsAccepted == null || !termsAccepted) {
            throw new IllegalArgumentException("You must accept the Terms & Conditions to create an account.");
        }

        User user = new User();
        user.setEmail(email);
        userRepository.save(user);

        return user;
    }


    private String issueRefreshToken(User user) {
        String rawToken = UUID.randomUUID() + "" + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(tokenHash);
        rt.setExpiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRATION_SECONDS));
        refreshTokenRepository.save(rt);

        return rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateOtp() {
        int code = (int) (Math.random() * 1_000_000);
        return String.format("%06d", code);
    }

}
