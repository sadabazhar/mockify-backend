package com.mockify.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Defines the PasswordEncoder bean in isolation.
 *
 * Kept separate from SecurityConfig intentionally: PasswordEncoder is used
 * by AuthServiceImpl, SandboxServiceImpl, and OAuth2AuthenticationSuccessHandler.
 * Defining it in SecurityConfig creates a circular dependency whenever any of
 * those services are transitively required by a SecurityConfig dependency.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}