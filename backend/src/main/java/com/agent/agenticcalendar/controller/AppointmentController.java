package com.agent.agenticcalendar.controller;

import com.agent.agenticcalendar.model.Appointment;
import com.agent.agenticcalendar.model.User;
import com.agent.agenticcalendar.repository.AppointmentRepository;
import com.agent.agenticcalendar.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public ResponseEntity<List<AppointmentWithUserResponse>> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        List<AppointmentWithUserResponse> response = appointments.stream()
                .map(appointment -> {
                    Optional<User> user = userRepository.findById(appointment.getUserId());
                    String userName = user.map(u -> u.getFirstName() + " " + u.getLastName())
                            .orElse("Unknown User");
                    return new AppointmentWithUserResponse(
                            appointment.getId(),
                            appointment.getUserId(),
                            userName,
                            appointment.getAppointmentDateTime(),
                            appointment.getDescription()
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
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
    
    public record AppointmentWithUserResponse(
            Long id,
            Long userId,
            String userName,
            java.time.LocalDateTime appointmentDateTime,
            String description
    ) {}
}

