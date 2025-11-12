package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.UserMapper;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//admin-level operations.
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // Fetch user details by ID
    @Override
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    // Fetch user details by email
    @Override
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user with Email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    // Get list of all users (admin use only)
    @Override
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    // Delete user by ID
    @Transactional
    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete, User not found with ID: " + id));

        userRepository.delete(user);
        log.info("User deleted successfully: {}", id);
    }

    // Check if email already exists
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Total registered users
    @Override
    public long countUsers() {
        return userRepository.count();
    }

}
