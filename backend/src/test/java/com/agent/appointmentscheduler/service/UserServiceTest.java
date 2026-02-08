package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Alperen", "Ugus", LocalDate.of(1990, 1, 1), "alperen@example.com");
        testUser.setId(1L);
    }

    @Test
    void getUser_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findByFirstNameAndLastNameAndDob(
                "Alperen", "Ugus", LocalDate.of(1990, 1, 1)))
                .thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUser("Alperen", "Ugus", LocalDate.of(1990, 1, 1));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Alperen");
        assertThat(result.get().getLastName()).isEqualTo("Ugus");
        verify(userRepository).findByFirstNameAndLastNameAndDob("Alperen", "Ugus", LocalDate.of(1990, 1, 1));
    }

    @Test
    void getUser_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Given
        when(userRepository.findByFirstNameAndLastNameAndDob(any(), any(), any()))
                .thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUser("John", "Doe", LocalDate.of(2000, 1, 1));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void saveUser_ShouldSaveAndReturnUser() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.saveUser(testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).save(testUser);
    }
}

