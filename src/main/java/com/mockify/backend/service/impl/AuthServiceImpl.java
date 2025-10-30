package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.service.AuthService;
import org.springframework.stereotype.Service;

//Handles user registration, login, and authentication flows
@Service
public class AuthServiceImpl implements AuthService {

    // Register a new user
    @Override
    public UserResponse register(RegisterRequest request) {
        return null;
    }

    // Login with email & password
    @Override
    public AuthResponse login(LoginRequest request) {
        return null;
    }

    // Refresh access token using refresh token
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        return null;
    }

    // Fetch details of currently authenticated user
    @Override
    public UserResponse getCurrentUser() {
        return null;
    }

    // Change current user's password
    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }

    // Logout user and invalidate tokens
    @Override
    public void logout() {

    }
}
