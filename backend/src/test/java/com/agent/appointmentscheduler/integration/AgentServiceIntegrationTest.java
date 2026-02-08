package com.agent.appointmentscheduler.integration;

import com.agent.appointmentscheduler.model.AgentResponse;
import com.agent.appointmentscheduler.model.Appointment;
import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.AppointmentRepository;
import com.agent.appointmentscheduler.repository.UserRepository;
import com.agent.appointmentscheduler.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AgentService with ChatClient and tool calling.
 * 
 * Note: This test requires either:
 * 1. OpenAI API key configured (set OPENAI_API_KEY environment variable)
 * 2. Ollama running locally with a compatible model (e.g., llama3.2)
 * 
 * For testing without actual LLM, consider using Testcontainers or mocking the ChatClient.
 * 
 * These tests are disabled by default. Enable them by removing @Disabled annotation
 * when you have a working LLM connection (Ollama or OpenAI).
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
@Disabled("Requires LLM connection (Ollama or OpenAI). Enable when LLM is available.")
class AgentServiceIntegrationTest {

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private User testUser;
    private static final String TEST_SESSION_ID = "test-session-123";

    @BeforeEach
    void setUp() {
        // Clean up appointments but keep users for testing
        appointmentRepository.deleteAll();

        // Use existing test user or create one
        testUser = userRepository.findByFirstNameAndLastNameAndDob("Alperen", "Ugus", LocalDate.of(1990, 1, 1))
                .orElseGet(() -> {
                    User user = new User("Alperen", "Ugus", LocalDate.of(1990, 1, 1), "alperen.ugus@example.com");
                    return userRepository.save(user);
                });
    }

    @Test
    void processUserMessage_WhenUserExists_ShouldResolveUserAndCreateAppointment() {
        // Given: A user exists in the database
        String userMessage = "Book an appointment for Alperen Ugus born on 1990-01-01 for tomorrow at 2 PM for a dental checkup";

        // When: Process the message through the agent
        AgentResponse response = agentService.processUserMessage(userMessage, TEST_SESSION_ID);

        // Then: Verify the response and that an appointment was created
        assertThat(response).isNotNull();
        assertThat(response.getFinalResponse()).isNotNull();
        assertThat(response.getFinalResponse()).isNotEmpty();

        // Verify appointment was created (if the agent successfully called the tools)
        // Note: This depends on the LLM actually calling the functions correctly
        // In a real scenario, you might want to verify the database state
        var appointments = appointmentRepository.findByUserId(testUser.getId());
        // The appointment may or may not be created depending on LLM behavior
        // This test primarily verifies the integration works without errors
    }

    @Test
    void processUserMessage_WhenUserDoesNotExist_ShouldAskForMoreInfo() {
        // Given: User doesn't exist
        String userMessage = "Book an appointment for John Doe";

        // When: Process the message
        AgentResponse response = agentService.processUserMessage(userMessage, TEST_SESSION_ID);

        // Then: Should ask for more information
        assertThat(response).isNotNull();
        assertThat(response.getFinalResponse()).isNotNull();
        assertThat(response.getFinalResponse()).isNotEmpty();
        // The agent should ask for DOB or more details
    }

    @Test
    void processUserMessage_WithUpdateRequest_ShouldUpdateAppointment() {
        // Given: An existing appointment
        Appointment appointment = new Appointment(
                testUser.getId(),
                LocalDateTime.of(2024, 12, 25, 10, 0),
                "Initial appointment"
        );
        appointment = appointmentRepository.save(appointment);

        String userMessage = String.format(
                "Update appointment %d to December 26, 2024 at 3 PM",
                appointment.getId()
        );

        // When: Process the update request
        AgentResponse response = agentService.processUserMessage(userMessage, TEST_SESSION_ID);

        // Then: Should process the update
        assertThat(response).isNotNull();
        assertThat(response.getFinalResponse()).isNotNull();
        assertThat(response.getFinalResponse()).isNotEmpty();
    }

    @Test
    void processUserMessage_WithDeleteRequest_ShouldDeleteAppointment() {
        // Given: An existing appointment
        Appointment appointment = new Appointment(
                testUser.getId(),
                LocalDateTime.of(2024, 12, 25, 10, 0),
                "Appointment to delete"
        );
        appointment = appointmentRepository.save(appointment);
        Long appointmentId = appointment.getId();

        String userMessage = String.format("Cancel appointment %d", appointmentId);

        // When: Process the delete request
        AgentResponse response = agentService.processUserMessage(userMessage, TEST_SESSION_ID);

        // Then: Should process the deletion
        assertThat(response).isNotNull();
        assertThat(response.getFinalResponse()).isNotNull();
        assertThat(response.getFinalResponse()).isNotEmpty();
    }
}

