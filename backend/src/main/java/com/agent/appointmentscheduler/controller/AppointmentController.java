package com.agent.appointmentscheduler.controller;

import com.agent.appointmentscheduler.model.Appointment;
import com.agent.appointmentscheduler.repository.AppointmentRepository;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentController(AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentRepository.findAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByUser(@PathVariable Long userId) {
        List<Appointment> appointments = appointmentRepository.findByUserId(userId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        Optional<Appointment> appointment = appointmentRepository.findById(id);
        return appointment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public ResponseEntity<AppointmentCountResponse> getAppointmentCount() {
        return ResponseEntity.ok(new AppointmentCountResponse(appointmentRepository.count()));
    }

    public record AppointmentCountResponse(long count) {}
}

