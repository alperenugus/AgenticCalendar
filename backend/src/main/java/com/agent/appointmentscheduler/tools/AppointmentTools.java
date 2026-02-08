package com.agent.appointmentscheduler.tools;

import com.agent.appointmentscheduler.model.Appointment;
import com.agent.appointmentscheduler.model.User;
import com.agent.appointmentscheduler.service.AppointmentService;
import com.agent.appointmentscheduler.service.InputValidationService;
import com.agent.appointmentscheduler.service.UserService;
import com.agent.appointmentscheduler.util.DateParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Configuration
public class AppointmentTools {

    private static final Logger log = LoggerFactory.getLogger(AppointmentTools.class);

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final ObjectMapper objectMapper;
    private final InputValidationService inputValidationService;

    public AppointmentTools(UserService userService, AppointmentService appointmentService, ObjectMapper objectMapper, InputValidationService inputValidationService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.objectMapper = objectMapper;
        this.inputValidationService = inputValidationService;
    }

    @Bean(name = "getUserFunction")
    @Description("Look up a user in the system by their first name, last name, and date of birth. This function is essential for identifying users before performing any appointment operations. The date of birth can be provided in any natural language format (e.g., '1990-01-01', 'January 1, 1990', 'born on 1990-01-01') and will be automatically parsed. Returns the user's ID (a numeric value) along with their full information. Always call this function first when a user mentions their name, as you need the userId to create, update, or view their appointments.")
    public FunctionCallback getUserFunction() {
        return FunctionCallbackWrapper.builder(
                (Function<Map<String, Object>, GetUserResponse>) arguments -> {
                    log.info("🔵 getUserFunction CALLED with arguments: {}", arguments);
                    try {
                        // Validate and extract inputs
                        String firstName = inputValidationService.validateName((String) arguments.get("firstName"));
                        String lastName = inputValidationService.validateName((String) arguments.get("lastName"));
                        LocalDate dob = null;
                        
                        // Parse date from various formats using DateParser utility
                        Object dobObj = arguments.get("dob");
                        if (dobObj != null) {
                            dob = DateParser.parseDate(dobObj);
                        }
                        
                        Optional<User> user = userService.getUser(firstName, lastName, dob);
                        
                        if (user.isPresent()) {
                            User u = user.get();
                            return new GetUserResponse(
                                    u.getId(),
                                    u.getFirstName(),
                                    u.getLastName(),
                                    u.getDob().toString(),
                                    u.getEmail(),
                                    "User found"
                            );
                        } else {
                            return new GetUserResponse(
                                    null,
                                    firstName,
                                    lastName,
                                    dob != null ? dob.toString() : null,
                                    null,
                                    "User not found. Please provide more details like date of birth (DOB) if not already provided."
                            );
                        }
                    } catch (Exception e) {
                        return new GetUserResponse(
                                null,
                                (String) arguments.get("firstName"),
                                (String) arguments.get("lastName"),
                                null,
                                null,
                                "Error looking up user: " + e.getMessage()
                        );
                    }
                }
        )
        .withName("getUser")
        .withDescription("Look up a user by first name, last name, and date of birth. The date of birth can be in any natural language format and will be parsed automatically. Returns the user's ID (numeric) and full information. Use this function first to identify users before any appointment operations. Required parameters: firstName (string), lastName (string), dob (string in any date format).")
        .build();
    }

    @Bean(name = "createAppointmentFunction")
    @Description("Create a new appointment for a user. Use this when the user wants to book, schedule, or create a new appointment. Requires: userId (numeric ID from getUser function), appointmentDateTime (ISO format: yyyy-MM-ddTHH:mm:ss, e.g., '2024-12-25T14:00:00' for Dec 25, 2024 at 2 PM), and description (e.g., 'dental checkup', 'consultation'). The userId must be obtained first by calling getUser. The appointmentDateTime should be converted from natural language (e.g., 'December 25, 2024 at 2 PM' → '2024-12-25T14:00:00').")
    public FunctionCallback createAppointmentFunction() {
        return FunctionCallbackWrapper.builder(
                (Function<Map<String, Object>, CreateAppointmentResponse>) arguments -> {
                    log.info("🔵 createAppointmentFunction CALLED with arguments: {}", arguments);
                    Long userId = null;
                    LocalDateTime appointmentDateTime = null;
                    String description = null;
                    
                    try {
                        // Validate inputs - handle userId as either Number or String
                        Object userIdObj = arguments.get("userId");
                        if (userIdObj instanceof Number) {
                            userId = ((Number) userIdObj).longValue();
                        } else if (userIdObj instanceof String) {
                            userId = Long.parseLong((String) userIdObj);
                        } else {
                            throw new IllegalArgumentException("userId must be a number or numeric string");
                        }
                        description = inputValidationService.validateDescription((String) arguments.get("description"));
                        
                        // Parse date time from various formats
                        Object dateTimeObj = arguments.get("appointmentDateTime");
                        if (dateTimeObj != null) {
                            if (dateTimeObj instanceof String) {
                                appointmentDateTime = LocalDateTime.parse((String) dateTimeObj);
                            } else if (dateTimeObj instanceof LocalDateTime) {
                                appointmentDateTime = (LocalDateTime) dateTimeObj;
                            } else {
                                appointmentDateTime = objectMapper.convertValue(dateTimeObj, LocalDateTime.class);
                            }
                        }
                        
                        log.info("Creating appointment: userId={}, dateTime={}, description={}", userId, appointmentDateTime, description);
                        Appointment appointment = appointmentService.createAppointment(
                                userId,
                                appointmentDateTime,
                                description
                        );
                        log.info("Appointment created successfully: id={}", appointment.getId());
                        return new CreateAppointmentResponse(
                                appointment.getId(),
                                appointment.getUserId(),
                                appointment.getAppointmentDateTime().toString(),
                                appointment.getDescription(),
                                "Appointment created successfully"
                        );
                    } catch (Exception e) {
                        return new CreateAppointmentResponse(
                                null,
                                userId != null ? userId : 0L,
                                appointmentDateTime != null ? appointmentDateTime.toString() : null,
                                description != null ? description : (String) arguments.get("description"),
                                "Error creating appointment: " + e.getMessage()
                        );
                    }
                }
        )
        .withName("createAppointment")
        .withDescription("Create a new appointment for a user. Use when user wants to book/schedule a new appointment. CRITICAL: This function ONLY accepts userId (numeric, must be obtained from getUser first), appointmentDateTime (ISO format: yyyy-MM-ddTHH:mm:ss), and description (string). It does NOT accept firstName, lastName, or dob - you MUST call getUser first to get the userId. Convert natural language dates to ISO format (e.g., 'Dec 25, 2024 at 2 PM' → '2024-12-25T14:00:00').")
        .build();
    }

    @Bean(name = "updateAppointmentFunction")
    @Description("Update an existing appointment's date and/or time. Use this when the user wants to change, reschedule, modify, or move an appointment. Keywords that indicate update: 'change', 'reschedule', 'update', 'modify', 'move', 'change time', 'change date'. If the appointmentId is not provided in the user's message, first call getUser to get the userId, then call getAppointmentsByUser to find the user's appointments, and use the appropriate appointmentId. Requires: appointmentId (numeric), newDateTime (ISO format: yyyy-MM-ddTHH:mm:ss). IMPORTANT: This updates the existing appointment - do NOT create a new one or delete the old one.")
    public FunctionCallback updateAppointmentFunction() {
        return FunctionCallbackWrapper.builder(
                (Function<Map<String, Object>, UpdateAppointmentResponse>) arguments -> {
                    log.info("🔵 updateAppointmentFunction CALLED with arguments: {}", arguments);
                    try {
                        // Handle appointmentId as either Number or String
                        Long appointmentId;
                        Object appointmentIdObj = arguments.get("appointmentId");
                        if (appointmentIdObj instanceof Number) {
                            appointmentId = ((Number) appointmentIdObj).longValue();
                        } else if (appointmentIdObj instanceof String) {
                            appointmentId = Long.parseLong((String) appointmentIdObj);
                        } else {
                            throw new IllegalArgumentException("appointmentId must be a number or numeric string");
                        }
                        
                        // Parse date time
                        LocalDateTime newDateTime = null;
                        Object dateTimeObj = arguments.get("newDateTime");
                        if (dateTimeObj != null) {
                            if (dateTimeObj instanceof String) {
                                newDateTime = LocalDateTime.parse((String) dateTimeObj);
                            } else if (dateTimeObj instanceof LocalDateTime) {
                                newDateTime = (LocalDateTime) dateTimeObj;
                            } else {
                                newDateTime = objectMapper.convertValue(dateTimeObj, LocalDateTime.class);
                            }
                        }
                        
                        log.info("Updating appointment: appointmentId={}, newDateTime={}", appointmentId, newDateTime);
                        Appointment appointment = appointmentService.updateAppointment(
                                appointmentId,
                                newDateTime
                        );
                        log.info("Appointment updated successfully: id={}, newDateTime={}", appointment.getId(), appointment.getAppointmentDateTime());
                        return new UpdateAppointmentResponse(
                                appointment.getId(),
                                appointment.getAppointmentDateTime().toString(),
                                "Appointment updated successfully"
                        );
                    } catch (Exception e) {
                        log.error("Error updating appointment: {}", e.getMessage());
                        Object appointmentIdObj = arguments.get("appointmentId");
                        Long appointmentId = null;
                        if (appointmentIdObj instanceof Number) {
                            appointmentId = ((Number) appointmentIdObj).longValue();
                        } else if (appointmentIdObj instanceof String) {
                            try {
                                appointmentId = Long.parseLong((String) appointmentIdObj);
                            } catch (NumberFormatException ignored) {}
                        }
                        return new UpdateAppointmentResponse(
                                appointmentId != null ? appointmentId : 0L,
                                null,
                                "Error updating appointment: " + e.getMessage()
                        );
                    }
                }
        )
        .withName("updateAppointment")
        .withDescription("Update an existing appointment's date/time. Use when user says 'change', 'reschedule', 'update', 'modify', or 'move'. If appointmentId not provided, use getUser then getAppointmentsByUser to find it. Requires: appointmentId (numeric), newDateTime (ISO: yyyy-MM-ddTHH:mm:ss). This UPDATES the appointment - do not create or delete.")
        .build();
    }

    @Bean(name = "getAppointmentsByUserFunction")
    @Description("Retrieve all appointments for a specific user. Use this function when you need to find a user's existing appointments, such as when they want to update or cancel an appointment but haven't specified the appointment ID. This function returns a list of all appointments for the user, including appointment IDs, dates/times, and descriptions. Chain this after getUser to find appointments when the user mentions 'my appointment' or 'change appointment' without specifying an ID. Requires: userId (numeric ID from getUser function).")
    public FunctionCallback getAppointmentsByUserFunction() {
        return FunctionCallbackWrapper.builder(
                (Function<Map<String, Object>, GetAppointmentsResponse>) arguments -> {
                    log.info("🔵 getAppointmentsByUserFunction CALLED with arguments: {}", arguments);
                    try {
                        // Handle userId as either Number or String
                        Long userId;
                        Object userIdObj = arguments.get("userId");
                        if (userIdObj instanceof Number) {
                            userId = ((Number) userIdObj).longValue();
                        } else if (userIdObj instanceof String) {
                            userId = Long.parseLong((String) userIdObj);
                        } else {
                            throw new IllegalArgumentException("userId must be a number or numeric string");
                        }
                        
                        List<Appointment> appointments = appointmentService.getAppointmentsByUserId(userId);
                        log.info("Found {} appointments for userId={}", appointments.size(), userId);
                        
                        if (appointments.isEmpty()) {
                            return new GetAppointmentsResponse(
                                    userId,
                                    List.of(),
                                    "No appointments found for this user"
                            );
                        }
                        
                        List<AppointmentInfo> appointmentInfos = appointments.stream()
                                .map(apt -> new AppointmentInfo(
                                        apt.getId(),
                                        apt.getAppointmentDateTime().toString(),
                                        apt.getDescription()
                                ))
                                .toList();
                        
                        return new GetAppointmentsResponse(
                                userId,
                                appointmentInfos,
                                "Found " + appointments.size() + " appointment(s)"
                        );
                    } catch (Exception e) {
                        log.error("Error getting appointments: {}", e.getMessage());
                        return new GetAppointmentsResponse(
                                null,
                                List.of(),
                                "Error getting appointments: " + e.getMessage()
                        );
                    }
                }
        )
        .withName("getAppointmentsByUser")
        .withDescription("Get all appointments for a user. Use when user wants to update/cancel but hasn't specified appointmentId. Chain after getUser. Requires: userId (numeric from getUser). Returns list of appointments with IDs, dates, and descriptions.")
        .build();
    }

    @Bean(name = "deleteAppointmentFunction")
    @Description("Delete (cancel) an appointment permanently. Use this when the user explicitly wants to cancel, delete, or remove an appointment. Keywords: 'cancel', 'delete', 'remove', 'cancel appointment'. If the appointmentId is not provided, first call getUser to get the userId, then call getAppointmentsByUser to find the appointment. Requires: appointmentId (numeric). This permanently removes the appointment - use updateAppointment if the user wants to change the time instead.")
    public FunctionCallback deleteAppointmentFunction() {
        return FunctionCallbackWrapper.builder(
                (Function<Map<String, Object>, DeleteAppointmentResponse>) arguments -> {
                    log.info("🔵 deleteAppointmentFunction CALLED with arguments: {}", arguments);
                    try {
                        // Handle appointmentId as either Number or String
                        Long appointmentId;
                        Object appointmentIdObj = arguments.get("appointmentId");
                        if (appointmentIdObj instanceof Number) {
                            appointmentId = ((Number) appointmentIdObj).longValue();
                        } else if (appointmentIdObj instanceof String) {
                            appointmentId = Long.parseLong((String) appointmentIdObj);
                        } else {
                            throw new IllegalArgumentException("appointmentId must be a number or numeric string");
                        }
                        
                        log.info("Deleting appointment: appointmentId={}", appointmentId);
                        appointmentService.deleteAppointment(appointmentId);
                        log.info("Appointment deleted successfully: id={}", appointmentId);
                        return new DeleteAppointmentResponse(
                                appointmentId,
                                "Appointment deleted successfully"
                        );
                    } catch (Exception e) {
                        log.error("Error deleting appointment: {}", e.getMessage());
                        Object appointmentIdObj = arguments.get("appointmentId");
                        Long appointmentId = null;
                        if (appointmentIdObj instanceof Number) {
                            appointmentId = ((Number) appointmentIdObj).longValue();
                        } else if (appointmentIdObj instanceof String) {
                            try {
                                appointmentId = Long.parseLong((String) appointmentIdObj);
                            } catch (NumberFormatException ignored) {}
                        }
                        return new DeleteAppointmentResponse(
                                appointmentId != null ? appointmentId : 0L,
                                "Error deleting appointment: " + e.getMessage()
                        );
                    }
                }
        )
        .withName("deleteAppointment")
        .withDescription("Delete/cancel an appointment permanently. Use when user says 'cancel', 'delete', or 'remove'. If appointmentId not provided, use getUser then getAppointmentsByUser to find it. Requires: appointmentId (numeric). This DELETES the appointment - use updateAppointment if user wants to change time.")
        .build();
    }

    // Request/Response Records
    public record GetUserRequest(
            @Description("First name of the user") String firstName,
            @Description("Last name of the user") String lastName,
            @Description("Date of birth in format yyyy-MM-dd") LocalDate dob
    ) {}

    public record GetUserResponse(
            Long userId,
            String firstName,
            String lastName,
            String dob,
            String email,
            String message
    ) {}

    public record CreateAppointmentRequest(
            @Description("User ID (must be obtained first using getUser function)") Long userId,
            @Description("Appointment date and time in format yyyy-MM-ddTHH:mm:ss") LocalDateTime appointmentDateTime,
            @Description("Description of the appointment") String description
    ) {}

    public record CreateAppointmentResponse(
            Long appointmentId,
            Long userId,
            String appointmentDateTime,
            String description,
            String message
    ) {}

    public record UpdateAppointmentRequest(
            @Description("Appointment ID to update") Long appointmentId,
            @Description("New date and time in format yyyy-MM-ddTHH:mm:ss") LocalDateTime newDateTime
    ) {}

    public record UpdateAppointmentResponse(
            Long appointmentId,
            String appointmentDateTime,
            String message
    ) {}

    public record DeleteAppointmentRequest(
            @Description("Appointment ID to delete") Long appointmentId
    ) {}

    public record DeleteAppointmentResponse(
            Long appointmentId,
            String message
    ) {}
    
    public record GetAppointmentsResponse(
            Long userId,
            List<AppointmentInfo> appointments,
            String message
    ) {}
    
    public record AppointmentInfo(
            Long appointmentId,
            String appointmentDateTime,
            String description
    ) {}
}

