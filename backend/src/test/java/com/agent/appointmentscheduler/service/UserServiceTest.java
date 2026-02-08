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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserService.
 * These tests verify that the service layer works correctly with the database,
 * including the new flexible search functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "langchain4j.ollama.base-url=http://localhost:11434",
    "langchain4j.ollama.model=llama3.1"
})
@Transactional
class UserServiceTest {

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
        User john1 = new User("John", "Doe", LocalDate.of(1992, 8, 20), "john.doe@example.com");
        User john2 = new User("John", "Smith", LocalDate.of(1995, 11, 25), "john.smith@example.com");
        User emily = new User("Emily", "Johnson", LocalDate.of(1988, 3, 10), "emily.johnson@example.com");
        
        userRepository.save(alperen);
        userRepository.save(sarah);
        userRepository.save(john1);
        userRepository.save(john2);
        userRepository.save(emily);
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

    // Tests for flexible search functionality

    @Test
    void searchUsersFlexible_WithAllParameters_ShouldReturnSingleUser() {
        // When
        List<User> result = userService.searchUsersFlexible("Alperen", "Ugus", LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Alperen");
        assertThat(result.get(0).getLastName()).isEqualTo("Ugus");
    }

    @Test
    void searchUsersFlexible_WithFirstNameOnly_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersFlexible("John", null, null);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getFirstName).containsOnly("John");
        assertThat(result).extracting(User::getLastName).contains("Doe", "Smith");
    }

    @Test
    void searchUsersFlexible_WithLastNameOnly_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersFlexible(null, "Smith", null);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getLastName).containsOnly("Smith");
        assertThat(result).extracting(User::getFirstName).contains("Sarah", "John");
    }

    @Test
    void searchUsersFlexible_WithDobOnly_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersFlexible(null, null, LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Alperen");
        assertThat(result.get(0).getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void searchUsersFlexible_WithFirstNameAndLastName_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersFlexible("John", "Doe", null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(0).getLastName()).isEqualTo("Doe");
    }

    @Test
    void searchUsersFlexible_WithFirstNameAndDob_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersFlexible("Alperen", null, LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Alperen");
        assertThat(result.get(0).getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void searchUsersFlexible_WithLastNameAndDob_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersFlexible(null, "Ugus", LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualTo("Ugus");
        assertThat(result.get(0).getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void searchUsersFlexible_WithNoParameters_ShouldReturnEmpty() {
        // When
        List<User> result = userService.searchUsersFlexible(null, null, null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void searchUsersFlexible_WithNonExistentUser_ShouldReturnEmpty() {
        // When
        List<User> result = userService.searchUsersFlexible("NonExistent", null, null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void searchUsersByFirstName_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersByFirstName("John");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getFirstName).containsOnly("John");
    }

    @Test
    void searchUsersByLastName_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersByLastName("Smith");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getLastName).containsOnly("Smith");
    }

    @Test
    void searchUsersByDob_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersByDob(LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void searchUsersByFirstNameAndLastName_ShouldReturnMatchingUsers() {
        // When
        List<User> result = userService.searchUsersByFirstNameAndLastName("John", "Doe");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(0).getLastName()).isEqualTo("Doe");
    }
}
