package com.agent.agenticcalendar.service;

import com.agent.agenticcalendar.model.Appointment;
import com.agent.agenticcalendar.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        testAppointment = new Appointment(1L, LocalDateTime.of(2024, 12, 25, 10, 0), "Test appointment");
        testAppointment.setId(1L);
    }

    @Test
    void createAppointment_ShouldSaveAndReturnAppointment() {
        // Given
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        // When
        Appointment result = appointmentService.createAppointment(
                1L,
                LocalDateTime.of(2024, 12, 25, 10, 0),
                "Test appointment"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Test appointment");
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void updateAppointment_WhenAppointmentExists_ShouldUpdateAndReturn() {
        // Given
        LocalDateTime newDateTime = LocalDateTime.of(2024, 12, 26, 14, 0);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        // When
        Appointment result = appointmentService.updateAppointment(1L, newDateTime);

        // Then
        assertThat(result).isNotNull();
        verify(appointmentRepository).findById(1L);
        verify(appointmentRepository).save(testAppointment);
    }

    @Test
    void updateAppointment_WhenAppointmentDoesNotExist_ShouldThrowException() {
        // Given
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> appointmentService.updateAppointment(1L, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    void deleteAppointment_WhenAppointmentExists_ShouldDelete() {
        // Given
        when(appointmentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(appointmentRepository).deleteById(1L);

        // When
        appointmentService.deleteAppointment(1L);

        // Then
        verify(appointmentRepository).existsById(1L);
        verify(appointmentRepository).deleteById(1L);
    }

    @Test
    void deleteAppointment_WhenAppointmentDoesNotExist_ShouldThrowException() {
        // Given
        when(appointmentRepository.existsById(1L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> appointmentService.deleteAppointment(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment not found");
    }
}

