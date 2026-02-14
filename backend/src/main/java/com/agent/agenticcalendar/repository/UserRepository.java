package com.agent.agenticcalendar.repository;

import com.agent.agenticcalendar.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByFirstNameAndLastNameAndDob(String firstName, String lastName, LocalDate dob);
    Optional<User> findByEmail(String email);
    
    // Partial search methods for flexible user lookup
    List<User> findByFirstName(String firstName);
    List<User> findByLastName(String lastName);
    List<User> findByDob(LocalDate dob);
    List<User> findByFirstNameAndLastName(String firstName, String lastName);
    List<User> findByFirstNameAndDob(String firstName, LocalDate dob);
    List<User> findByLastNameAndDob(String lastName, LocalDate dob);
}

