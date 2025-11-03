package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

//Manages users (typically admin-level operations).
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private  UserRepository userRepository;
    // Fetch user details by ID
    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    // Fetch user details by email
    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    // Get list of all users (admin use only)
    @Override
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList())
    }

    // Delete user by ID
    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);

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
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

}
