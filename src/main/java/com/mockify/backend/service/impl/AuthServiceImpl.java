package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.auth.LoginRequest;
import com.mockify.backend.dto.request.auth.RegisterRequest;
import com.mockify.backend.dto.response.auth.AuthResponse;
import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.exception.UnauthorizedException;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        //  Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // Create and save new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        // Generate token
        String accessToken = jwtTokenProvider.generateToken(savedUser.getId());

        // Build response
        AuthResponse.TokenInfo tokens = AuthResponse.TokenInfo.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .build();

        AuthResponse response = AuthResponse.builder()
                .status("success")
                .message("User registered successfully")
                .tokens(tokens)
                .user(userMapper.toResponse(savedUser))
                .build();

        log.info("User registered successfully with id: {}", savedUser.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate token
        String accessToken = jwtTokenProvider.generateToken(user.getId());

        // Build response
        AuthResponse.TokenInfo tokens = AuthResponse.TokenInfo.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .build();

        AuthResponse response = AuthResponse.builder()
                .status("success")
                .message("Login successful")
                .tokens(tokens)
                .user(userMapper.toResponse(user))
                .build();

        log.info("User logged in successfully: {}", user.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    public void logout() {
        // Stateless logout: just discard tokens client-side
        System.out.println("User logging out.");
    }
}
