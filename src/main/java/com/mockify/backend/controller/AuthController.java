package com.mockify.backend.controller;

import com.mockify.backend.dto.request.auth.*;
import com.mockify.backend.dto.response.auth.AuthResult;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {


        authService.requestRegistration(request);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/register/verify")
    public ResponseEntity<?> verify(
            @RequestParam String token) {

        authService.completeRegistration(token);

        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request) {

        AuthResult authResult = authService.login(request);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authResult.refreshCookie().toString())
                .body(authResult.response());
    }

    /**
     * Returns the current authenticated user's profile.
     * SECURITY: JWT-only.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication auth) {

        // API keys must not reach this endpoint
        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UserResponse user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false)
            String refreshToken) {

        AuthResult authResult = authService.refresh(refreshToken);


        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authResult.refreshCookie().toString())
                .body(authResult.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false)
            String refreshToken) {

        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.clearRefreshToken().toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request.getEmail());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request.getToken(), request.getNewPassword());

        // Temporary approach: clear the refresh token cookie after password reset
        // TODO: Replace with token versioning to support proper global logout

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.clearRefreshToken().toString())
                .build();
    }
}



