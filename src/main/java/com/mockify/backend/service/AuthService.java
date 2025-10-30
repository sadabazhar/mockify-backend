package com.mockify.backend.service;

import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;

public interface AuthService {

    // Register a new user
    UserResponse register(RegisterRequest request);

    // Login with email & password
    AuthResponse login(LoginRequest request);

    // Refresh access token using refresh token
    AuthResponse refreshToken(String refreshToken);

    // Fetch details of currently authenticated user
    UserResponse getCurrentUser();

    // Change current user's password
    void changePassword(String oldPassword, String newPassword);

    // Logout user and invalidate tokens
    void logout();
}
