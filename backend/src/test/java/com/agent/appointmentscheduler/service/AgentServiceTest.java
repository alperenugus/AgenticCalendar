package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AgentService.
 * These tests verify that the service layer works correctly with the database.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.ai.ollama.base-url=http://localhost:11434",
    "spring.ai.ollama.chat.options.model=llama3.2"
})
@Transactional
class AgentServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        // Create test users
        User alperen = new User("Alperen", "Ugus", LocalDate.of(1990, 1, 1), "alperen.ugus@example.com");
        User sarah = new User("Sarah", "Smith", LocalDate.of(1985, 5, 15), "sarah.smith@example.com");
        
        userRepository.save(alperen);
        userRepository.save(sarah);
    }

    @Test
    void userService_ShouldFindUserByFirstNameLastNameAndDob() {
        // When
        Optional<User> result = userService.getUser("Alperen", "Ugus", LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Alperen");
        assertThat(result.get().getLastName()).isEqualTo("Ugus");
        assertThat(result.get().getEmail()).isEqualTo("alperen.ugus@example.com");
    }

    @Test
    void userService_WhenUserNotFound_ShouldReturnEmpty() {
        // When
        Optional<User> result = userService.getUser("Unknown", "User", LocalDate.of(2000, 1, 1));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void userService_ShouldFindMultipleUsers() {
        // When
        Optional<User> alperen = userService.getUser("Alperen", "Ugus", LocalDate.of(1990, 1, 1));
        Optional<User> sarah = userService.getUser("Sarah", "Smith", LocalDate.of(1985, 5, 15));

        // Then
        assertThat(alperen).isPresent();
        assertThat(sarah).isPresent();
        assertThat(alperen.get().getFirstName()).isEqualTo("Alperen");
        assertThat(sarah.get().getFirstName()).isEqualTo("Sarah");
    }
}

