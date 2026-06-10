package com.agent.agenticcalendar.tools;

import com.agent.agenticcalendar.model.Event;
import com.agent.agenticcalendar.model.EventStatus;
import com.agent.agenticcalendar.service.EventService;
import com.agent.agenticcalendar.service.InputValidationService;
import com.agent.agenticcalendar.util.RecurrenceParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calendar operations exposed to the AI agent. These methods are dispatched by
 * {@link com.agent.agenticcalendar.service.AgentService}'s hand-rolled ReAct loop
 * (a manual {@code switch} on the parsed action name) — NOT by LangChain4j's native
 * tool-calling. The LLM-facing tool descriptions are the single source of truth in the
 * agent's system prompt; each method returns a compact JSON string for the agent to read.
 */
@Service
public class CalendarToolService {

    private static final Logger log = LoggerFactory.getLogger(CalendarToolService.class);

    private final EventService eventService;
    private final InputValidationService inputValidationService;

    public CalendarToolService(
            EventService eventService,
            InputValidationService inputValidationService
    ) {
        this.eventService = eventService;
        this.inputValidationService = inputValidationService;
    }

    /**
     * Creates a calendar event. Times are ISO-8601 ({@code yyyy-MM-ddTHH:mm:ss});
     * {@code recurrenceRule} accepts natural language ("weekly", "every Monday") or an
     * RFC 5545 RRULE and is parsed by {@link RecurrenceParser}. Returns a JSON string.
     */
    public String createEvent(String title, String startTime, String endTime, String description, String location, String recurrenceRule, String sessionId, String googleUserId, String googleUserEmail) {
        log.info("🔵 createEvent CALLED with title={}, startTime={}, endTime={}, recurrenceRule={}, sessionId={}", 
                title, startTime, endTime, recurrenceRule, sessionId);
        try {
            String validatedTitle = inputValidationService.validateDescription(title);
            String validatedDescription = description != null ? inputValidationService.validateDescription(description) : null;
            String validatedLocation = location != null ? inputValidationService.validateDescription(location) : null;
            
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            
            if (end.isBefore(start) || end.equals(start)) {
                return "{\"error\": \"End time must be after start time\"}";
            }
            
            // Parse recurrence rule from natural language to RRULE format
            String parsedRecurrenceRule = null;
            if (recurrenceRule != null && !recurrenceRule.trim().isEmpty()) {
                parsedRecurrenceRule = RecurrenceParser.parseRecurrence(recurrenceRule, start);
                if (parsedRecurrenceRule == null) {
                    log.warn("Could not parse recurrence pattern: {}. Creating non-recurring event.", recurrenceRule);
                } else {
                    log.info("Parsed recurrence rule: {} -> {}", recurrenceRule, parsedRecurrenceRule);
                }
            }
            
            Event event = eventService.createEvent(validatedTitle, start, end, validatedDescription, 
                    parsedRecurrenceRule, sessionId, googleUserId, googleUserEmail);
            if (validatedLocation != null) {
                event.setLocation(validatedLocation);
                event = eventService.updateEvent(event.getId(), null, null, null, null, validatedLocation, null);
            }
            
            log.info("Event created successfully: id={}, recurrenceRule={}", event.getId(), event.getRecurrenceRule());
            
            String message = "Event created successfully";
            if (parsedRecurrenceRule != null) {
                message += " (recurring: " + parsedRecurrenceRule + ")";
            }
            
            return String.format(
                "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"recurrenceRule\": \"%s\", \"message\": \"%s\"}",
                event.getId(), event.getTitle(), event.getStartTime(), event.getEndTime(),
                event.getRecurrenceRule() != null ? event.getRecurrenceRule() : "",
                message
            );
        } catch (Exception e) {
            log.error("Error creating event: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error creating event: %s\"}", e.getMessage());
        }
    }

    /** Returns all active (non-cancelled) events for the user/session as a JSON string. */
    public String getEvents(String sessionId, String googleUserId) {
        log.info("🔵 getEvents CALLED with sessionId={}, googleUserId={}", sessionId, googleUserId);
        try {
            List<Event> events;
            if (googleUserId != null && !googleUserId.isEmpty()) {
                events = eventService.getAllActiveEventsByGoogleUser(googleUserId);
            } else {
                events = eventService.getAllActiveEvents(sessionId);
            }
            
            if (events.isEmpty()) {
                return "{\"events\": [], \"message\": \"No events found in your calendar\"}";
            }
            
            StringBuilder json = new StringBuilder("{\"events\": [");
            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                if (i > 0) json.append(", ");
                json.append(String.format(
                    "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"description\": \"%s\", \"location\": \"%s\", \"status\": \"%s\"}",
                    event.getId(), 
                    escapeJson(event.getTitle()), 
                    event.getStartTime(), 
                    event.getEndTime(),
                    escapeJson(event.getDescription() != null ? event.getDescription() : ""),
                    escapeJson(event.getLocation() != null ? event.getLocation() : ""),
                    event.getStatus()
                ));
            }
            json.append("], \"message\": \"Found ").append(events.size()).append(" event(s)\"}");
            return json.toString();
        } catch (Exception e) {
            log.error("Error getting events: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting events: %s\"}", e.getMessage());
        }
    }

    /**
     * Returns events overlapping [startDate, endDate] (ISO-8601), with recurring events
     * expanded into individual occurrences. Returns a JSON string.
     */
    public String getEventsByDateRange(String sessionId, String startDate, String endDate, String googleUserId) {
        log.info("🔵 getEventsByDateRange CALLED with sessionId={}, startDate={}, endDate={}", 
                sessionId, startDate, endDate);
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            List<Event> events;
            if (googleUserId != null && !googleUserId.isEmpty()) {
                events = eventService.getEventsByGoogleUserIdAndDateRange(googleUserId, start, end);
            } else {
                events = eventService.getEventsBySessionIdAndDateRange(sessionId, start, end);
            }
            
            if (events.isEmpty()) {
                return String.format("{\"events\": [], \"message\": \"No events found between %s and %s\"}", startDate, endDate);
            }
            
            StringBuilder json = new StringBuilder("{\"events\": [");
            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                if (i > 0) json.append(", ");
                json.append(String.format(
                    "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"description\": \"%s\", \"location\": \"%s\"}",
                    event.getId(), 
                    escapeJson(event.getTitle()), 
                    event.getStartTime(), 
                    event.getEndTime(),
                    escapeJson(event.getDescription() != null ? event.getDescription() : ""),
                    escapeJson(event.getLocation() != null ? event.getLocation() : "")
                ));
            }
            json.append("], \"message\": \"Found ").append(events.size()).append(" event(s) in the specified date range\"}");
            return json.toString();
        } catch (Exception e) {
            log.error("Error getting events by date range: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting events: %s\"}", e.getMessage());
        }
    }

    /** Returns the details of a single event by id as a JSON string. */
    public String getEvent(Long eventId) {
        log.info("🔵 getEvent CALLED with eventId={}", eventId);
        try {
            Event event = eventService.getEventById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
            
            return String.format(
                "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", " +
                "\"description\": \"%s\", \"location\": \"%s\", \"status\": \"%s\", \"type\": \"%s\"}",
                event.getId(), 
                escapeJson(event.getTitle()), 
                event.getStartTime(), 
                event.getEndTime(),
                escapeJson(event.getDescription() != null ? event.getDescription() : ""),
                escapeJson(event.getLocation() != null ? event.getLocation() : ""),
                event.getStatus(),
                event.getType()
            );
        } catch (Exception e) {
            log.error("Error getting event: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting event: %s\"}", e.getMessage());
        }
    }

    /** Returns events that overlap [startTime, endTime] (recurring events expanded) as a JSON string. */
    public String checkConflicts(String sessionId, String startTime, String endTime, String googleUserId) {
        log.info("🔵 checkConflicts CALLED with sessionId={}, startTime={}, endTime={}", sessionId, startTime, endTime);
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            
            List<Event> conflicts;
            if (googleUserId != null && !googleUserId.isEmpty()) {
                conflicts = eventService.findConflictingEventsByGoogleUser(googleUserId, start, end);
            } else {
                conflicts = eventService.findConflictingEvents(sessionId, start, end);
            }
            
            if (conflicts.isEmpty()) {
                return "{\"hasConflicts\": false, \"conflicts\": [], \"message\": \"No conflicts found - time slot is available\"}";
            }
            
            StringBuilder json = new StringBuilder("{\"hasConflicts\": true, \"conflicts\": [");
            for (int i = 0; i < conflicts.size(); i++) {
                Event conflict = conflicts.get(i);
                if (i > 0) json.append(", ");
                json.append(String.format(
                    "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\"}",
                    conflict.getId(), 
                    escapeJson(conflict.getTitle()), 
                    conflict.getStartTime(), 
                    conflict.getEndTime()
                ));
            }
            json.append("], \"message\": \"Found ").append(conflicts.size())
                .append(" conflicting event(s). Consider choosing a different time.\"}");
            return json.toString();
        } catch (Exception e) {
            log.error("Error checking conflicts: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error checking conflicts: %s\"}", e.getMessage());
        }
    }

    /**
     * Updates an event. Only non-null fields are changed; the rest are left as-is.
     * Returns a JSON string.
     */
    public String updateEvent(Long eventId, String startTime, String endTime, String title, String description, String location, String status) {
        log.info("🔵 updateEvent CALLED with eventId={}, startTime={}, endTime={}, title={}", 
                eventId, startTime, endTime, title);
        try {
            LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : null;
            LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : null;
            
            if (start != null && end != null && (end.isBefore(start) || end.equals(start))) {
                return "{\"error\": \"End time must be after start time\"}";
            }
            
            String validatedTitle = title != null ? inputValidationService.validateDescription(title) : null;
            String validatedDescription = description != null ? inputValidationService.validateDescription(description) : null;
            String validatedLocation = location != null ? inputValidationService.validateDescription(location) : null;
            EventStatus eventStatus = status != null ? EventStatus.valueOf(status.toUpperCase()) : null;
            
            Event event = eventService.updateEvent(eventId, start, end, validatedTitle, validatedDescription, validatedLocation, eventStatus);
            
            log.info("Event updated successfully: id={}", event.getId());
            
            return String.format(
                "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"message\": \"Event updated successfully\"}",
                event.getId(), event.getTitle(), event.getStartTime(), event.getEndTime()
            );
        } catch (Exception e) {
            log.error("Error updating event: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error updating event: %s\"}", e.getMessage());
        }
    }

    /** Permanently deletes an event by id. Returns a JSON string. */
    public String deleteEvent(Long eventId) {
        log.info("🔵 deleteEvent CALLED with eventId={}", eventId);
        try {
            eventService.deleteEvent(eventId);
            log.info("Event deleted successfully: id={}", eventId);
            
            return String.format(
                "{\"eventId\": %d, \"message\": \"Event deleted successfully\"}",
                eventId
            );
        } catch (Exception e) {
            log.error("Error deleting event: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error deleting event: %s\"}", e.getMessage());
        }
    }

    /** Returns up to 10 upcoming events ordered by start time as a JSON string. */
    public String getUpcomingEvents(String sessionId, String googleUserId) {
        log.info("🔵 getUpcomingEvents CALLED with sessionId={}, googleUserId={}", sessionId, googleUserId);
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Event> events;
            
            if (googleUserId != null && !googleUserId.isEmpty()) {
                events = eventService.getUpcomingEventsByGoogleUser(googleUserId, now);
            } else {
                events = eventService.getUpcomingEvents(sessionId, now);
            }
            
            if (events.isEmpty()) {
                return "{\"events\": [], \"message\": \"No upcoming events found\"}";
            }
            
            // Limit to next 10 events
            events = events.stream().limit(10).collect(Collectors.toList());
            
            StringBuilder json = new StringBuilder("{\"events\": [");
            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                if (i > 0) json.append(", ");
                json.append(String.format(
                    "{\"eventId\": %d, \"title\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"description\": \"%s\"}",
                    event.getId(), 
                    escapeJson(event.getTitle()), 
                    event.getStartTime(), 
                    event.getEndTime(),
                    escapeJson(event.getDescription() != null ? event.getDescription() : "")
                ));
            }
            json.append("], \"message\": \"Found ").append(events.size()).append(" upcoming event(s)\"}");
            return json.toString();
        } catch (Exception e) {
            log.error("Error getting upcoming events: {}", e.getMessage(), e);
            return String.format("{\"error\": \"Error getting upcoming events: %s\"}", e.getMessage());
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

