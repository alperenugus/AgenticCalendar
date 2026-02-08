package com.agent.appointmentscheduler.tools;

import com.agent.appointmentscheduler.service.AppointmentService;
import com.agent.appointmentscheduler.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that AppointmentTools beans are created correctly.
 * The actual function calling is tested in integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AppointmentToolsTest {

    @Autowired
    private AppointmentTools appointmentTools;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void appointmentTools_ShouldBeCreated() {
        assertThat(appointmentTools).isNotNull();
    }

    @Test
    void getUserFunction_ShouldBeCreated() {
        var function = appointmentTools.getUserFunction();
        assertThat(function).isNotNull();
        assertThat(function.getName()).isEqualTo("getUser");
    }

    @Test
    void createAppointmentFunction_ShouldBeCreated() {
        var function = appointmentTools.createAppointmentFunction();
        assertThat(function).isNotNull();
        assertThat(function.getName()).isEqualTo("createAppointment");
    }

    @Test
    void updateAppointmentFunction_ShouldBeCreated() {
        var function = appointmentTools.updateAppointmentFunction();
        assertThat(function).isNotNull();
        assertThat(function.getName()).isEqualTo("updateAppointment");
    }

    @Test
    void deleteAppointmentFunction_ShouldBeCreated() {
        var function = appointmentTools.deleteAppointmentFunction();
        assertThat(function).isNotNull();
        assertThat(function.getName()).isEqualTo("deleteAppointment");
    }
}

