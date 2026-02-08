package com.agent.appointmentscheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for validating and sanitizing user input to prevent security issues.
 */
@Service
public class InputValidationService {

    private static final Logger log = LoggerFactory.getLogger(InputValidationService.class);
    
    // Flag to enable/disable validations (kept for future use)
    private static final boolean VALIDATION_ENABLED = false; // Currently disabled
    
    // Maximum input length
    private static final int MAX_INPUT_LENGTH = 2000;
    
    // Patterns for potentially harmful content
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|onerror|onload)"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script>|javascript:|onerror=|onload=|eval\\(|alert\\()"
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(;|\\||&|`|\\$|\\(|\\)|\\{|\\}|<|>|\\n|\\r)"
    );

    /**
     * Validates and sanitizes user input.
     * 
     * @param input The user input to validate
     * @return Validated and sanitized input
     * @throws IllegalArgumentException if input is invalid or potentially harmful
     */
    public String validateAndSanitize(String input) {
        if (!VALIDATION_ENABLED) {
            // Validations disabled - just return trimmed input
            return input != null ? input.trim() : "";
        }
        
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        // Trim whitespace
        String sanitized = input.trim();

        // Check length
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }

        if (sanitized.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("Input exceeds maximum length of " + MAX_INPUT_LENGTH + " characters");
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            log.warn("Potential SQL injection attempt detected: {}", sanitized.substring(0, Math.min(50, sanitized.length())));
            throw new IllegalArgumentException("Invalid input detected. Please rephrase your request.");
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(sanitized).find()) {
            log.warn("Potential XSS attempt detected: {}", sanitized.substring(0, Math.min(50, sanitized.length())));
            throw new IllegalArgumentException("Invalid input detected. Please rephrase your request.");
        }

        // Check for command injection patterns (less strict, as some are needed for natural language)
        // Only flag if multiple suspicious patterns are found
        long suspiciousCount = COMMAND_INJECTION_PATTERN.matcher(sanitized).results().count();
        if (suspiciousCount > 5) {
            log.warn("Potential command injection attempt detected: {}", sanitized.substring(0, Math.min(50, sanitized.length())));
            throw new IllegalArgumentException("Invalid input detected. Please rephrase your request.");
        }

        // Remove any remaining control characters except newlines and tabs (for natural language)
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        return sanitized;
    }

    /**
     * Validates that a name contains only safe characters.
     */
    public String validateName(String name) {
        if (!VALIDATION_ENABLED) {
            // Validations disabled - just return trimmed name
            return name != null ? name.trim() : "";
        }
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        String sanitized = name.trim();
        
        // Allow letters, spaces, hyphens, apostrophes (for names like O'Brien)
        if (!sanitized.matches("^[a-zA-Z\\s\\-'']+$")) {
            throw new IllegalArgumentException("Name contains invalid characters");
        }

        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Name exceeds maximum length");
        }

        return sanitized;
    }

    /**
     * Validates that a description is safe.
     */
    public String validateDescription(String description) {
        if (!VALIDATION_ENABLED) {
            // Validations disabled - just return trimmed description
            return description != null ? description.trim() : "";
        }
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }

        String sanitized = description.trim();

        if (sanitized.length() > 500) {
            throw new IllegalArgumentException("Description exceeds maximum length of 500 characters");
        }

        // Remove any HTML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        return sanitized;
    }
}

