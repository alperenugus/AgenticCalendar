package com.agent.appointmentscheduler.config;

import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

@TestConfiguration
@Profile("test")
public class TestDataInitializer {

    @Bean
    public TestDataSetup testDataSetup(UserRepository userRepository) {
        return new TestDataSetup(userRepository);
    }

    public static class TestDataSetup {
        private final UserRepository userRepository;

        public TestDataSetup(UserRepository userRepository) {
            this.userRepository = userRepository;
            initializeTestData();
        }

        private void initializeTestData() {
            if (userRepository.count() == 0) {
                // Create test users matching the production data
                User alperen = new User("Alperen", "Ugus", LocalDate.of(1990, 1, 1), "alperen.ugus@example.com");
                User sarah = new User("Sarah", "Smith", LocalDate.of(1985, 5, 15), "sarah.smith@example.com");
                User john = new User("John", "Doe", LocalDate.of(1992, 8, 20), "john.doe@example.com");
                
                userRepository.save(alperen);
                userRepository.save(sarah);
                userRepository.save(john);
            }
        }
    }
}

