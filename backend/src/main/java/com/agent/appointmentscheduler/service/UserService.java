package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUser(String firstName, String lastName, LocalDate dob) {
        return userRepository.findByFirstNameAndLastNameAndDob(firstName, lastName, dob);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    // Partial search methods for flexible user lookup
    public List<User> searchUsersByFirstName(String firstName) {
        return userRepository.findByFirstName(firstName);
    }
    
    public List<User> searchUsersByLastName(String lastName) {
        return userRepository.findByLastName(lastName);
    }
    
    public List<User> searchUsersByDob(LocalDate dob) {
        return userRepository.findByDob(dob);
    }
    
    public List<User> searchUsersByFirstNameAndLastName(String firstName, String lastName) {
        return userRepository.findByFirstNameAndLastName(firstName, lastName);
    }
    
    public List<User> searchUsersByFirstNameAndDob(String firstName, LocalDate dob) {
        return userRepository.findByFirstNameAndDob(firstName, dob);
    }
    
    public List<User> searchUsersByLastNameAndDob(String lastName, LocalDate dob) {
        return userRepository.findByLastNameAndDob(lastName, dob);
    }
    
    /**
     * Flexible user search that handles partial information
     * Returns list of matching users based on available information
     */
    public List<User> searchUsersFlexible(String firstName, String lastName, LocalDate dob) {
        // Check which parameters we have
        boolean hasFirstName = firstName != null && !firstName.trim().isEmpty();
        boolean hasLastName = lastName != null && !lastName.trim().isEmpty();
        boolean hasDob = dob != null;
        
        // If no parameters provided, return empty list
        if (!hasFirstName && !hasLastName && !hasDob) {
            return List.of();
        }
        
        // Try combinations based on available parameters (most specific first)
        if (hasFirstName && hasLastName && hasDob) {
            // All three - exact match
            return userRepository.findByFirstNameAndLastNameAndDob(firstName, lastName, dob)
                    .map(List::of)
                    .orElse(List.of());
        } else if (hasFirstName && hasLastName) {
            // First and last name
            return userRepository.findByFirstNameAndLastName(firstName, lastName);
        } else if (hasFirstName && hasDob) {
            // First name and DOB
            return userRepository.findByFirstNameAndDob(firstName, dob);
        } else if (hasLastName && hasDob) {
            // Last name and DOB
            return userRepository.findByLastNameAndDob(lastName, dob);
        } else if (hasFirstName) {
            // Just first name
            return userRepository.findByFirstName(firstName);
        } else if (hasLastName) {
            // Just last name
            return userRepository.findByLastName(lastName);
        } else if (hasDob) {
            // Just DOB
            return userRepository.findByDob(dob);
        }
        
        return List.of();
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User createUser(String firstName, String lastName, LocalDate dob, String email) {
        // Check if user with same email already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        
        User user = new User(firstName, lastName, dob, email);
        return userRepository.save(user);
    }
    
    public User updateUser(Long userId, String firstName, String lastName, LocalDate dob, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " not found"));
        
        // Check if email is being changed and if new email already exists
        if (email != null && !email.equals(user.getEmail())) {
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                throw new IllegalArgumentException("User with email " + email + " already exists");
            }
        }
        
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (dob != null) {
            user.setDob(dob);
        }
        if (email != null) {
            user.setEmail(email);
        }
        
        return userRepository.save(user);
    }
    
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " not found");
        }
        
        // Security: Prevent deleting the last user in the system
        long totalUsers = userRepository.count();
        if (totalUsers <= 1) {
            throw new SecurityException("Cannot delete the last user in the system. This is a security measure to prevent database wipe.");
        }
        
        userRepository.deleteById(userId);
    }
    
    public long getTotalUserCount() {
        return userRepository.count();
    }
}

