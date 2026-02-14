package com.agent.agenticcalendar.integration;

import com.agent.agenticcalendar.model.User;
import com.agent.agenticcalendar.repository.UserRepository;
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
 * Test to verify that test users can be created and retrieved.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class DataInitializationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateAndRetrieveTestUsers() {
        // Given: Create test users
        User alperen = new User("Alperen", "Ugus", LocalDate.of(1990, 1, 1), "alperen.ugus@example.com");
        User sarah = new User("Sarah", "Smith", LocalDate.of(1985, 5, 15), "sarah.smith@example.com");
        User john = new User("John", "Doe", LocalDate.of(1992, 8, 20), "john.doe@example.com");

        userRepository.save(alperen);
        userRepository.save(sarah);
        userRepository.save(john);

        // When: Retrieve users
        Optional<User> foundAlperen = userRepository.findByFirstNameAndLastNameAndDob("Alperen", "Ugus", LocalDate.of(1990, 1, 1));
        Optional<User> foundSarah = userRepository.findByFirstNameAndLastNameAndDob("Sarah", "Smith", LocalDate.of(1985, 5, 15));
        Optional<User> foundJohn = userRepository.findByFirstNameAndLastNameAndDob("John", "Doe", LocalDate.of(1992, 8, 20));

        // Then: Verify users exist
        assertThat(foundAlperen).isPresent();
        assertThat(foundAlperen.get().getEmail()).isEqualTo("alperen.ugus@example.com");

        assertThat(foundSarah).isPresent();
        assertThat(foundSarah.get().getEmail()).isEqualTo("sarah.smith@example.com");

        assertThat(foundJohn).isPresent();
        assertThat(foundJohn.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void shouldFindUserByExactMatch() {
        // Given
        User user = new User("Test", "User", LocalDate.of(2000, 1, 1), "test@example.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByFirstNameAndLastNameAndDob("Test", "User", LocalDate.of(2000, 1, 1));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Test");
    }

    @Test
    void shouldNotFindUserWithWrongDob() {
        // Given
        User user = new User("Test", "User", LocalDate.of(2000, 1, 1), "test@example.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByFirstNameAndLastNameAndDob("Test", "User", LocalDate.of(2001, 1, 1));

        // Then
        assertThat(found).isEmpty();
    }
}

