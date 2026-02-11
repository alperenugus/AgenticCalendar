package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.model.Appointment;
import com.agent.appointmentscheduler.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment createAppointment(Long userId, LocalDateTime appointmentDateTime, String description) {
        Appointment appointment = new Appointment(userId, appointmentDateTime, description);
        return appointmentRepository.save(appointment);
    }

    public Optional<Appointment> getAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId);
    }

    public Appointment updateAppointment(Long appointmentId, LocalDateTime newDateTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));
        appointment.setAppointmentDateTime(newDateTime);
        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new IllegalArgumentException("Appointment not found with id: " + appointmentId);
        }
        
        // Security: Prevent bulk deletion - ensure we're not deleting all appointments
        long totalAppointments = appointmentRepository.count();
        if (totalAppointments <= 1) {
            throw new SecurityException("Cannot delete the last appointment in the system. This is a security measure to prevent database wipe.");
        }
        
        appointmentRepository.deleteById(appointmentId);
    }
    
    public long getTotalAppointmentCount() {
        return appointmentRepository.count();
    }

    public List<Appointment> getAppointmentsByUserId(Long userId) {
        return appointmentRepository.findByUserId(userId);
    }
}

