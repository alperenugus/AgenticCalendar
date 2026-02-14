package com.agent.agenticcalendar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("!test") // Don't run in test profile
    CommandLineRunner initDatabase() {
        return args -> {
            log.info("Calendar application initialized. No test data needed - users will authenticate via Google OAuth.");
        };
    }
}

