package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.auth.UserResponse;
import com.mockify.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

//Manages users (typically admin-level operations).
@Service
public class UserServiceImpl implements UserService {

    // Fetch user details by ID
    @Override
    public UserResponse getUserById(Long id) {
        return null;
    }

    // Fetch user details by email
    @Override
    public UserResponse getUserByEmail(String email) {
        return null;
    }

    // Get list of all users (admin use only)
    @Override
    public List<UserResponse> getAllUsers() {
        return List.of();
    }

    // Delete user by ID
    @Override
    public void deleteUser(Long id) {

    }

    // Check if email already exists
    @Override
    public boolean existsByEmail(String email) {
        return false;
    }

    // Total registered users
    @Override
    public long countUsers() {
        return 0;
    }

}
