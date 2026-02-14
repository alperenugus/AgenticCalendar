package com.agent.agenticcalendar.tools;

import com.agent.agenticcalendar.model.Appointment;
import com.agent.agenticcalendar.model.User;
import com.agent.agenticcalendar.service.AppointmentService;
import com.agent.agenticcalendar.service.InputValidationService;
import com.agent.agenticcalendar.service.UserService;
import com.agent.agenticcalendar.util.DateParser;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentToolService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentToolService.class);

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final InputValidationService inputValidationService;

    public AppointmentToolService(
            UserService userService,
            AppointmentService appointmentService,
            InputValidationService inputValidationService
    ) {
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.inputValidationService = inputValidationService;
    }

    @Tool("Look up users in the system by their first name, last name, and/or date of birth. " +
          "This function is flexible and can work with partial information - you can provide just firstName, " +
          "just lastName, just dob, or any combination. The date of birth can be provided in any natural " +
          "language format (e.g., '1990-01-01', 'January 1, 1990', 'born on 1990-01-01') and will be " +
          "automatically parsed. If multiple users match, the function will return a list of matching users " +
          "so you can ask the user which one they want. Returns the user's ID (a numeric value) along with " +
          "their full information. Always call this function first when a user mentions their name, as you " +
          "need the userId to create, update, or view their appointments.")
    public String getUser(String firstName, String lastName, String dob) {
        log.info("🔵 getUser CALLED with firstName={}, lastName={}, dob={}", firstName, lastName, dob);
        try {
            // Extract inputs - all are optional now
            String firstNameValidated = null;
            String lastNameValidated = null;
            LocalDate dobParsed = null;
            
            // Validate and extract firstName if provided
            if (firstName != null && !firstName.trim().isEmpty()) {
                firstNameValidated = inputValidationService.validateName(firstName);
            }
            
            // Validate and extract lastName if provided
            if (lastName != null && !lastName.trim().isEmpty()) {
                lastNameValidated = inputValidationService.validateName(lastName);
            }
            
            // Parse date from various formats using DateParser utility
            if (dob != null && !dob.trim().isEmpty()) {
                dobParsed = DateParser.parseDate(dob);
            }
            
            // Use flexible search
            List<User> users = userService.searchUsersFlexible(firstNameValidated, lastNameValidated, dobParsed);
            
            if (users.isEmpty()) {
                return String.format(
                    "{\"userId\": null, \"message\": \"No users found matching the provided information. Please provide more details (first name, last name, or date of birth).\"}"
                );
            } else if (users.size() == 1) {
                // Single match - return directly
                User u = users.get(0);
                return String.format(
                    "{\"userId\": %d, \"firstName\": \"%s\", \"lastName\": \"%s\", \"dob\": \"%s\", \"email\": \"%s\", \"message\": \"User found\"}",
                    u.getId(), u.getFirstName(), u.getLastName(), u.getDob(), u.getEmail()
                );
            } else {
                // Multiple matches - return list for user selection
                StringBuilder json = new StringBuilder("{\"hasMultipleMatches\": true, \"users\": [");
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    if (i > 0) json.append(", ");
                    json.append(String.format(
                        "{\"userId\": %d, \"firstName\": \"%s\", \"lastName\": \"%s\", \"dob\": \"%s\", \"email\": \"%s\"}",
                        u.getId(), u.getFirstName(), u.getLastName(), u.getDob(), u.getEmail()
                    ));
                }
                json.append("], \"message\": \"Found ").append(users.size())
                    .append(" matching users. Please specify which user you want to work with by providing more details (like date of birth) or selecting from the list.\"}");
                return json.toString();
            }
        } catch (Exception e) {
            log.error("Error in getUser: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error looking up user: %s\"}", e.getMessage());
        }
    }

    @Tool("Retrieve all appointments for a specific user. Use this function when you need to find a user's " +
          "existing appointments, such as when they want to update or cancel an appointment but haven't " +
          "specified the appointment ID. This function returns a list of all appointments for the user, " +
          "including appointment IDs, dates/times, and descriptions. Chain this after getUser to find " +
          "appointments when the user mentions 'my appointment' or 'change appointment' without specifying an ID. " +
          "Requires: userId (numeric ID from getUser function).")
    public String getAppointmentsByUser(Long userId) {
        log.info("🔵 getAppointmentsByUser CALLED with userId={}", userId);
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByUserId(userId);
            log.info("Found {} appointments for userId={}", appointments.size(), userId);
            
            if (appointments.isEmpty()) {
                return String.format(
                    "{\"userId\": %d, \"appointments\": [], \"message\": \"No appointments found for this user\"}",
                    userId
                );
            }
            
            StringBuilder json = new StringBuilder("{\"userId\": ").append(userId).append(", \"appointments\": [");
            for (int i = 0; i < appointments.size(); i++) {
                Appointment apt = appointments.get(i);
                if (i > 0) json.append(", ");
                json.append(String.format(
                    "{\"appointmentId\": %d, \"appointmentDateTime\": \"%s\", \"description\": \"%s\"}",
                    apt.getId(), apt.getAppointmentDateTime(), apt.getDescription()
                ));
            }
            json.append("], \"message\": \"Found ").append(appointments.size()).append(" appointment(s)\"}");
            return json.toString();
        } catch (Exception e) {
            log.error("Error getting appointments: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting appointments: %s\"}", e.getMessage());
        }
    }

    @Tool("Create a new appointment for a user. Use this when the user wants to book, schedule, or create " +
          "a new appointment. Requires: userId (numeric ID from getUser function), appointmentDateTime " +
          "(ISO format: yyyy-MM-ddTHH:mm:ss, e.g., '2024-12-25T14:00:00' for Dec 25, 2024 at 2 PM), and " +
          "description (e.g., 'dental checkup', 'consultation'). The userId must be obtained first by " +
          "calling getUser. The appointmentDateTime should be converted from natural language " +
          "(e.g., 'December 25, 2024 at 2 PM' → '2024-12-25T14:00:00').")
    public String createAppointment(Long userId, String appointmentDateTime, String description) {
        log.info("🔵 createAppointment CALLED with userId={}, appointmentDateTime={}, description={}", 
                userId, appointmentDateTime, description);
        try {
            String validatedDescription = inputValidationService.validateDescription(description);
            LocalDateTime dateTime = LocalDateTime.parse(appointmentDateTime);
            
            log.info("Creating appointment: userId={}, dateTime={}, description={}", userId, dateTime, validatedDescription);
            Appointment appointment = appointmentService.createAppointment(userId, dateTime, validatedDescription);
            log.info("Appointment created successfully: id={}", appointment.getId());
            
            return String.format(
                "{\"appointmentId\": %d, \"userId\": %d, \"appointmentDateTime\": \"%s\", \"description\": \"%s\", \"message\": \"Appointment created successfully\"}",
                appointment.getId(), appointment.getUserId(), appointment.getAppointmentDateTime(), appointment.getDescription()
            );
        } catch (Exception e) {
            log.error("Error creating appointment: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error creating appointment: %s\"}", e.getMessage());
        }
    }

    @Tool("Update an existing appointment's date and/or time. Use this when the user wants to change, " +
          "reschedule, modify, or move an appointment. Keywords that indicate update: 'change', 'reschedule', " +
          "'update', 'modify', 'move', 'change time', 'change date'. If the appointmentId is not provided " +
          "in the user's message, first call getUser to get the userId, then call getAppointmentsByUser to " +
          "find the user's appointments, and use the appropriate appointmentId. Requires: appointmentId " +
          "(numeric), newDateTime (ISO format: yyyy-MM-ddTHH:mm:ss). IMPORTANT: This updates the existing " +
          "appointment - do NOT create a new one or delete the old one.")
    public String updateAppointment(Long appointmentId, String newDateTime) {
        log.info("🔵 updateAppointment CALLED with appointmentId={}, newDateTime={}", appointmentId, newDateTime);
        try {
            LocalDateTime dateTime = LocalDateTime.parse(newDateTime);
            
            log.info("Updating appointment: appointmentId={}, newDateTime={}", appointmentId, dateTime);
            Appointment appointment = appointmentService.updateAppointment(appointmentId, dateTime);
            log.info("Appointment updated successfully: id={}, newDateTime={}", appointment.getId(), appointment.getAppointmentDateTime());
            
            return String.format(
                "{\"appointmentId\": %d, \"appointmentDateTime\": \"%s\", \"message\": \"Appointment updated successfully\"}",
                appointment.getId(), appointment.getAppointmentDateTime()
            );
        } catch (Exception e) {
            log.error("Error updating appointment: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error updating appointment: %s\"}", e.getMessage());
        }
    }

    @Tool("Delete (cancel) an appointment permanently. Use this when the user explicitly wants to cancel, " +
          "delete, or remove an appointment. Keywords: 'cancel', 'delete', 'remove', 'cancel appointment'. " +
          "If the appointmentId is not provided, first call getUser to get the userId, then call " +
          "getAppointmentsByUser to find the appointment. Requires: appointmentId (numeric). This permanently " +
          "removes the appointment - use updateAppointment if the user wants to change the time instead. " +
          "SECURITY: You can only delete one appointment at a time. You cannot delete all appointments in the system. " +
          "If a user wants to delete all their appointments, you can delete them one by one, but you must never " +
          "delete all appointments in the entire system.")
    public String deleteAppointment(Long appointmentId) {
        log.info("🔵 deleteAppointment CALLED with appointmentId={}", appointmentId);
        try {
            log.info("Deleting appointment: appointmentId={}", appointmentId);
            appointmentService.deleteAppointment(appointmentId);
            log.info("Appointment deleted successfully: id={}", appointmentId);
            
            return String.format(
                "{\"appointmentId\": %d, \"message\": \"Appointment deleted successfully\"}",
                appointmentId
            );
        } catch (SecurityException e) {
            log.warn("Security violation: {}", e.getMessage());
            return String.format("{\"error\": \"Security restriction: %s\"}", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting appointment: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error deleting appointment: %s\"}", e.getMessage());
        }
    }

    @Tool("Create a new user in the system. Use this when the user wants to register, add, or create a new user. " +
          "The LLM should collect all required information (first name, last name, date of birth, email) from the user " +
          "before calling this function. The date of birth can be provided in any natural language format " +
          "(e.g., '1990-01-01', 'January 1, 1990', 'born on 1990-01-01') and will be automatically parsed. " +
          "Requires: firstName, lastName, dob (date string in any format), email. Returns the created user's ID.")
    public String createUser(String firstName, String lastName, String dob, String email) {
        log.info("🔵 createUser CALLED with firstName={}, lastName={}, dob={}, email={}", firstName, lastName, dob, email);
        try {
            String validatedFirstName = inputValidationService.validateName(firstName);
            String validatedLastName = inputValidationService.validateName(lastName);
            LocalDate dobParsed = DateParser.parseDate(dob);
            String validatedEmail = inputValidationService.validateEmail(email);
            
            log.info("Creating user: firstName={}, lastName={}, dob={}, email={}", validatedFirstName, validatedLastName, dobParsed, validatedEmail);
            User user = userService.createUser(validatedFirstName, validatedLastName, dobParsed, validatedEmail);
            log.info("User created successfully: id={}", user.getId());
            
            return String.format(
                "{\"userId\": %d, \"firstName\": \"%s\", \"lastName\": \"%s\", \"dob\": \"%s\", \"email\": \"%s\", \"message\": \"User created successfully\"}",
                user.getId(), user.getFirstName(), user.getLastName(), user.getDob(), user.getEmail()
            );
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error creating user: %s\"}", e.getMessage());
        }
    }

    @Tool("Update an existing user's information. Use this when the user wants to change, modify, or update " +
          "their personal information (name, date of birth, or email). The LLM should collect the updated " +
          "information from the user. Requires: userId (numeric ID from getUser function), and any combination " +
          "of: firstName, lastName, dob (date string in any format), email. Only provide the fields that need " +
          "to be updated. The date of birth can be provided in any natural language format.")
    public String updateUser(Long userId, String firstName, String lastName, String dob, String email) {
        log.info("🔵 updateUser CALLED with userId={}, firstName={}, lastName={}, dob={}, email={}", 
                userId, firstName, lastName, dob, email);
        try {
            String validatedFirstName = null;
            String validatedLastName = null;
            LocalDate dobParsed = null;
            String validatedEmail = null;
            
            if (firstName != null && !firstName.trim().isEmpty()) {
                validatedFirstName = inputValidationService.validateName(firstName);
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                validatedLastName = inputValidationService.validateName(lastName);
            }
            if (dob != null && !dob.trim().isEmpty()) {
                dobParsed = DateParser.parseDate(dob);
            }
            if (email != null && !email.trim().isEmpty()) {
                validatedEmail = inputValidationService.validateEmail(email);
            }
            
            log.info("Updating user: userId={}, firstName={}, lastName={}, dob={}, email={}", 
                    userId, validatedFirstName, validatedLastName, dobParsed, validatedEmail);
            User user = userService.updateUser(userId, validatedFirstName, validatedLastName, dobParsed, validatedEmail);
            log.info("User updated successfully: id={}", user.getId());
            
            return String.format(
                "{\"userId\": %d, \"firstName\": \"%s\", \"lastName\": \"%s\", \"dob\": \"%s\", \"email\": \"%s\", \"message\": \"User updated successfully\"}",
                user.getId(), user.getFirstName(), user.getLastName(), user.getDob(), user.getEmail()
            );
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error updating user: %s\"}", e.getMessage());
        }
    }

    @Tool("Delete a user from the system permanently. Use this when the user explicitly wants to remove, " +
          "delete, or unregister a user account. Keywords: 'delete user', 'remove user', 'unregister'. " +
          "If the userId is not provided, first call getUser to find the user. Requires: userId (numeric). " +
          "SECURITY: Before deleting a user, you MUST first check if they have appointments using getAppointmentsByUser. " +
          "If the user has appointments, inform the user that they need to delete all appointments first, or ask for " +
          "confirmation to delete the user and all their appointments. You cannot delete the last user in the system. " +
          "WARNING: This permanently removes the user and all their appointments. Use with caution.")
    public String deleteUser(Long userId) {
        log.info("🔵 deleteUser CALLED with userId={}", userId);
        try {
            // Security: Check if user has appointments before deletion
            List<Appointment> userAppointments = appointmentService.getAppointmentsByUserId(userId);
            if (userAppointments != null && !userAppointments.isEmpty()) {
                return String.format(
                    "{\"error\": \"Cannot delete user %d: User has %d active appointment(s). " +
                    "Please delete all appointments first using deleteAppointment, or explicitly confirm deletion of user and all appointments.\"}",
                    userId, userAppointments.size()
                );
            }
            
            log.info("Deleting user: userId={}", userId);
            userService.deleteUser(userId);
            log.info("User deleted successfully: id={}", userId);
            
            return String.format(
                "{\"userId\": %d, \"message\": \"User deleted successfully\"}",
                userId
            );
        } catch (SecurityException e) {
            log.warn("Security violation: {}", e.getMessage());
            return String.format("{\"error\": \"Security restriction: %s\"}", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error deleting user: %s\"}", e.getMessage());
        }
    }
}

