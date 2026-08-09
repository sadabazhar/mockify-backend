package com.mockify.backend.service.impl;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.dto.request.auth.PendingRegistration;
import com.mockify.backend.dto.response.auth.AuthResult;
import com.mockify.backend.dto.response.auth.TokenPair;
import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.exception.UnauthorizedException;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.PasswordResetToken;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.PasswordResetTokenRepository;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.security.RefreshTokenBlacklist;
import com.mockify.backend.service.AuthService;
import com.mockify.backend.service.EmailVerificationService;
import com.mockify.backend.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final CookieUtil cookieUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final RefreshTokenBlacklist refreshTokenBlacklist;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordBaseUrl;
    @Value("${app.verification.email.frontend-url}")
    private String emailVerificationBaseUrl;

    // Constants
    private static final String AUTH_PROVIDER_LOCAL = "local";
    private static final String TOKEN_TYPE_BEARER = "Bearer";


    @Override
    @Transactional
    public void requestRegistration(RegisterRequest request) {

        // Prevent duplicate registrations at the entry point.
        // This avoids sending verification emails for already-registered users.
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User registration failed email={} reason = already_exists", request.getEmail());
            throw new DuplicateResourceException("Email already registered");
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        // Create a short-lived, single-use verification token
        // and store pending registration data in Redis.
        String token = emailVerificationService.createVerification(
                request.getName(),
                request.getEmail(),
                encodedPassword
        );

        // Build frontend verification link
        String verifyLink = emailVerificationBaseUrl + "?token=" + token;

        // Send verification email.
        mailService.sendEmailVerificationMail(
                request.getEmail(),
                verifyLink
        );

        log.info("Registration verification email sent | email={}", request.getEmail());
    }

    @Override
    @Transactional
    public void completeRegistration(String token) {

        // Validate verification token and consume it atomically.
        // Token is removed immediately after successful validation.
        PendingRegistration pending =
                emailVerificationService.verifyAndConsume(token);

        // Re-check email existence to avoid race conditions
        // (e.g. same email verified twice in parallel).
        if (userRepository.existsByEmail(pending.getEmail())) {
            log.warn("User registration failed email={} reason=already_exists", pending.getEmail());
            throw new DuplicateResourceException("Email already registered");
        }

        // Create user only after successful email verification.
        User user = new User();
        user.setName(pending.getName());
        user.setEmail(pending.getEmail());
        user.setRole(UserRole.USER);            // set role USER to prevent accidental admin creation
        user.setEmailVerified(true);
        user.setPassword(pending.getEncodedPassword());
        user.setProviderName(AUTH_PROVIDER_LOCAL);
        user.setUsername(
                pending.getEmail().split("@")[0].toLowerCase()
        );

        // Save user info in database
        userRepository.save(user);

    }


    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));

        // Block non-local authentication methods
        if (!AUTH_PROVIDER_LOCAL.equals(user.getProviderName())) {
            throw new UnauthorizedException("This account uses " + user.getProviderName() + " login. Please login with that provider.");
        }

        // Block guest accounts from using password-based login
        if (user.isGuest()) {
            throw new UnauthorizedException(
                    "Guest sessions cannot use password authentication. " +
                            "Use your sandbox token to resume your session."
            );
        }

        //  BLOCK unverified users
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid password");
        }

        // Generate token based on role
        TokenPair tokens = new TokenPair(
                jwtTokenProvider.generateAccessToken(user.getId(), user.getRole()),
                jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole())
        );

        // Build cookie
        ResponseCookie cookie = cookieUtil.createRefreshToken(tokens.refreshToken());

        // Build response body
        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userMapper.toResponse(user))
                .build();

        log.info("Login successful userId={}", user.getId());

        return new AuthResult(response, cookie);
    }

    @Override
    @Transactional
    public AuthResult refresh(String refreshToken) {

        log.info("Token refresh requested");

        // Token validation
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token missing");
        }

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("Token refresh failed reason=invalid_refresh_token");
            throw new UnauthorizedException("Invalid refresh token");
        }

        // Check Redis blacklist
        String jti = jwtTokenProvider.getJti(refreshToken);
        if (refreshTokenBlacklist.isBlacklisted(jti)) {
            log.warn("Blocked refresh attempt using blacklisted token jti={}", jti);
            throw new UnauthorizedException("Refresh token has been invalidated");
        }

        // User validation
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Generate token based on role
        TokenPair tokens = new TokenPair(
                jwtTokenProvider.generateAccessToken(userId, user.getRole()),
                jwtTokenProvider.generateRefreshToken(userId, user.getRole())
        );

        // Blacklist old refresh token after new token generation
        Date expiration = jwtTokenProvider.getExpiration(refreshToken);
        Duration ttl = Duration.between(Instant.now(), expiration.toInstant());
        if (ttl.isNegative() || ttl.isZero()) {
            throw new UnauthorizedException("Refresh token expired");
        }
        refreshTokenBlacklist.blacklist(jti, ttl);

        // Build cookie
        ResponseCookie cookie = cookieUtil.createRefreshToken(tokens.refreshToken());

        // Build response body
        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();

        log.info("Token refreshed userId={}", userId);

        return new AuthResult(response, cookie);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {

        log.debug("Fetching user profile userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public void logout(String refreshToken) {

        log.info("Logout requested");

        // missing token should not fail logout
        if (refreshToken == null || refreshToken.isBlank()) {
            log.info("Logout completed without refresh token");
            return;
        }

        try {
            // Validate token structure & type
            if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
                log.warn("Skipping blacklist: invalid refresh token");
                return;
            }

            String jti = jwtTokenProvider.getJti(refreshToken);

            Date expiration = jwtTokenProvider.getExpiration(refreshToken);

            if (jti == null || expiration == null) {
                log.warn("Skipping blacklist: missing jti or expiration");
                return;
            }

            Duration ttl = Duration.between(
                    Instant.now(),
                    expiration.toInstant()
            );

            // Blacklist refresh token by jti for some TTL
            refreshTokenBlacklist.blacklist(jti, ttl);

            log.info("Refresh token invalidated jti={}", jti);

        } catch (Exception ex) {
            // Logout must NEVER break user flow
            log.error("Logout failed silently: {}", ex.getMessage());
        }

        log.info("Logout completed");
    }


    @Override
    @Transactional
    public void forgotPassword(String email) {

        // If user exists, run logic
        // If user does NOT exist, do nothing
        // Method still returns 200 OK

        userRepository.findByEmail(email).ifPresent(user -> {

            // Only local users can reset password
            if (!AUTH_PROVIDER_LOCAL.equals(user.getProviderName())) {
                log.info("Password reset skipped for oauth user email={}", email);
                return;
            }

            // TODO: Enable later to skip processing if email is not verified
            //  if(user.isEmailVerified()) return;

            // Generate raw token
            String rawToken = UUID.randomUUID().toString();

            // Hash token (for now bcrypt is ok, letter SHA-256 token optimization)
            String hashedToken = passwordEncoder.encode(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hashedToken)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Create reset link with raw token
            String resetLink = resetPasswordBaseUrl + "?token=" + rawToken;

            // Send mail to register user with reset link
            mailService.sendPasswordResetMail(user.getEmail(),resetLink);

            log.info("Password reset token created userId={}", user.getId());
        });

        // Always return success (no email existence leak)
    }


    // TODO: Currently only newPassword is accepted. Add confirmPassword parameter in the future.
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {

        // Fetch unused and non expire tokens
        List<PasswordResetToken> tokens =
                passwordResetTokenRepository
                        .findByUsedFalseAndExpiresAtAfter(LocalDateTime.now());


        // Filter token: raw token matches with hashed token
        PasswordResetToken validToken = tokens.stream()
                .filter(t -> passwordEncoder.matches(token, t.getTokenHash()))
                .findFirst()

                // If token not matches all rules, Throw error
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid or expired reset token")
                );

        User user = validToken.getUser();

        if (!AUTH_PROVIDER_LOCAL.equals(user.getProviderName())) {
            throw new UnauthorizedException("Password reset not allowed for OAuth users");
        }

        // Check: new password must be different from old password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from old password");
        }


        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Set new password and mark token as used, prevent token reuse
        validToken.setUsed(true);
        passwordResetTokenRepository.save(validToken);

        log.info("Password reset successful userId={}", user.getId());
    }
}
