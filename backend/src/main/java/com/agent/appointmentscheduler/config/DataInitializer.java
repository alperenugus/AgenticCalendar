package com.agent.appointmentscheduler.config;

import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("!test") // Don't run in test profile
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                log.info("Initializing database with test users...");
                
                // Create test users
                User alperen = new User("Alperen", "Ugus", LocalDate.of(1990, 1, 1), "alperen.ugus@example.com");
                User sarah = new User("Sarah", "Smith", LocalDate.of(1985, 5, 15), "sarah.smith@example.com");
                User john = new User("John", "Doe", LocalDate.of(1992, 8, 20), "john.doe@example.com");
                User emily = new User("Emily", "Johnson", LocalDate.of(1988, 3, 10), "emily.johnson@example.com");
                User michael = new User("Michael", "Brown", LocalDate.of(1995, 11, 25), "michael.brown@example.com");
                
                userRepository.save(alperen);
                userRepository.save(sarah);
                userRepository.save(john);
                userRepository.save(emily);
                userRepository.save(michael);
                
                log.info("Created {} test users", userRepository.count());
                log.info("Test users created:");
                log.info("  - Alperen Ugus (DOB: 1990-01-01) - Email: alperen.ugus@example.com");
                log.info("  - Sarah Smith (DOB: 1985-05-15) - Email: sarah.smith@example.com");
                log.info("  - John Doe (DOB: 1992-08-20) - Email: john.doe@example.com");
                log.info("  - Emily Johnson (DOB: 1988-03-10) - Email: emily.johnson@example.com");
                log.info("  - Michael Brown (DOB: 1995-11-25) - Email: michael.brown@example.com");
            } else {
                log.info("Database already contains {} users, skipping initialization", userRepository.count());
            }
        };
    }
}

