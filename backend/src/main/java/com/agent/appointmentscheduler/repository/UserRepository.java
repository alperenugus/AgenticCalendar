package com.agent.appointmentscheduler.repository;

import com.agent.appointmentscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByFirstNameAndLastNameAndDob(String firstName, String lastName, LocalDate dob);
    Optional<User> findByEmail(String email);
}

