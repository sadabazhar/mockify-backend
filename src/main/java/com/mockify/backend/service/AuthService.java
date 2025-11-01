package com.mockify.backend.service;

import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RefreshTokenRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;

public interface AuthService {

    // Register a new user
    AuthResponse register(RegisterRequest request);

    // Login with email & password
    AuthResponse login(LoginRequest request);

    // Fetch details of currently authenticated user
    UserResponse getCurrentUser(Long userId);

    // Logout user and invalidate tokens
    void logout();

    // Refresh access_token using refresh_token
    AuthResponse refreshToken(RefreshTokenRequest refreshToken);

//     Change current user's password
//  void changePassword(String oldPassword, String newPassword);

}
