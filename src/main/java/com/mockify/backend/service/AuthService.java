package com.mockify.backend.service;

import com.mockify.backend.dto.response.auth.AuthResult;
import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.model.User;
import com.mockify.backend.security.oauth2.OAuth2UserInfo;

import java.util.UUID;

public interface AuthService {

    // user registration request
    public void requestRegistration(RegisterRequest request);

    // Completes the user registration after successful email verification
    public void completeRegistration(String token);

    // Login with email & password
    AuthResult login(LoginRequest request);

    // Fetch details of currently authenticated user
    UserResponse getCurrentUser(UUID userId);

    // Logout user and invalidate tokens
    void logout(String refreshToken);

    // Refresh access_token using refresh_token
    AuthResult refresh(String refreshToken);

    // Forget password request
    void forgotPassword(String email);

    // Reset password via mail
    void resetPassword(String token, String newPassword);

    // Register or login the OAuth user
    public User findOrCreateOAuthUser(String provider, OAuth2UserInfo userInfo);

}
