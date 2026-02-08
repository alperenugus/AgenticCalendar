package com.agent.appointmentscheduler.controller;

import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/count")
    public ResponseEntity<UserCountResponse> getUserCount() {
        return ResponseEntity.ok(new UserCountResponse(userRepository.count()));
    }

    public record UserCountResponse(long count) {}
}

