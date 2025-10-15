package com.mockify.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class MockifyBackendApplication {
	public static void main(String[] args) {
		// Load environment variables from .env file at project root
		// Using ignoreIfMissing() so app won't crash if .env is missing
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		// Set DB credentials as system properties for Spring to read in application.properties
		System.setProperty("DB_URL", dotenv.get("DB_URL"));
		System.setProperty("DB_USER", dotenv.get("DB_USER"));
		System.setProperty("DB_PASS", dotenv.get("DB_PASS"));

		// Log message to confirm environment variables were loaded successfully
		System.out.println("Environment variables loaded");
		SpringApplication.run(MockifyBackendApplication.class, args);
	}
}
