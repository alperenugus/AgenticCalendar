package com.agent.agenticcalendar.service;

import com.agent.agenticcalendar.model.Event;
import com.agent.agenticcalendar.model.EventStatus;
import com.agent.agenticcalendar.repository.EventRepository;
import com.agent.agenticcalendar.util.RecurrenceExpander;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event createEvent(String title, LocalDateTime startTime, LocalDateTime endTime, 
                            String description, String sessionId, String googleUserId, String googleUserEmail) {
        return createEvent(title, startTime, endTime, description, null, sessionId, googleUserId, googleUserEmail);
    }

    public Event createEvent(String title, LocalDateTime startTime, LocalDateTime endTime, 
                            String description, String recurrenceRule, String sessionId, String googleUserId, String googleUserEmail) {
        Event event = new Event(title, startTime, endTime, description, sessionId);
        if (googleUserId != null) {
            event.setGoogleUserId(googleUserId);
            event.setGoogleUserEmail(googleUserEmail);
        }
        if (recurrenceRule != null && !recurrenceRule.trim().isEmpty()) {
            event.setRecurrenceRule(recurrenceRule);
        }
        return eventRepository.save(event);
    }

    public Optional<Event> getEventById(Long eventId) {
        return eventRepository.findById(eventId);
    }

    public Event updateEvent(Long eventId, LocalDateTime startTime, LocalDateTime endTime, 
                            String title, String description, String location, EventStatus status) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
        
        if (startTime != null) event.setStartTime(startTime);
        if (endTime != null) event.setEndTime(endTime);
        if (title != null) event.setTitle(title);
        if (description != null) event.setDescription(description);
        if (location != null) event.setLocation(location);
        if (status != null) event.setStatus(status);
        
        return eventRepository.save(event);
    }

    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with id: " + eventId);
        }
        eventRepository.deleteById(eventId);
    }

    public List<Event> getEventsBySessionId(String sessionId) {
        return eventRepository.findBySessionId(sessionId);
    }

    public List<Event> getEventsByGoogleUserId(String googleUserId) {
        return eventRepository.findByGoogleUserId(googleUserId);
    }

    /**
     * Gets events in a date range, expanding recurring events into individual occurrences.
     * This matches Google Calendar's behavior - recurring events are expanded on-the-fly.
     */
    public List<Event> getEventsBySessionIdAndDateRange(String sessionId, LocalDateTime start, LocalDateTime end) {
        // Get all events that might have occurrences in the range
        // This includes:
        // 1. Non-recurring events in the range
        // 2. Recurring events that start before or during the range
        List<Event> candidateEvents = eventRepository.findBySessionIdAndStartTimeBefore(sessionId, end);
        
        // Expand recurring events and filter to range
        return expandAndFilterEvents(candidateEvents, start, end);
    }

    public List<Event> getEventsByGoogleUserIdAndDateRange(String googleUserId, LocalDateTime start, LocalDateTime end) {
        // Get all events that might have occurrences in the range
        List<Event> candidateEvents = eventRepository.findByGoogleUserIdAndStartTimeBefore(googleUserId, end);
        
        // Expand recurring events and filter to range
        return expandAndFilterEvents(candidateEvents, start, end);
    }

    /**
     * Expands recurring events and filters to the date range.
     */
    private List<Event> expandAndFilterEvents(List<Event> events, LocalDateTime start, LocalDateTime end) {
        List<Event> result = new ArrayList<>();
        
        for (Event event : events) {
            if (event.getRecurrenceRule() != null && !event.getRecurrenceRule().trim().isEmpty()) {
                // Recurring event - expand it
                List<Event> occurrences = RecurrenceExpander.expandEvent(event, start, end);
                result.addAll(occurrences);
            } else {
                // Non-recurring event - include if in range
                if (!event.getEndTime().isBefore(start) && !event.getStartTime().isAfter(end)) {
                    result.add(event);
                }
            }
        }
        
        return result;
    }

    /**
     * Finds conflicting events, including occurrences from recurring events.
     */
    public List<Event> findConflictingEvents(String sessionId, LocalDateTime startTime, LocalDateTime endTime) {
        // Get all events that might conflict (including recurring events)
        List<Event> candidateEvents = eventRepository.findBySessionIdAndStartTimeBefore(sessionId, endTime);
        
        // Expand recurring events and check for conflicts
        return findConflictsInExpandedEvents(candidateEvents, startTime, endTime);
    }

    public List<Event> findConflictingEventsByGoogleUser(String googleUserId, LocalDateTime startTime, LocalDateTime endTime) {
        // Get all events that might conflict (including recurring events)
        List<Event> candidateEvents = eventRepository.findByGoogleUserIdAndStartTimeBefore(googleUserId, endTime);
        
        // Expand recurring events and check for conflicts
        return findConflictsInExpandedEvents(candidateEvents, startTime, endTime);
    }

    /**
     * Expands events and finds conflicts with the given time range.
     */
    private List<Event> findConflictsInExpandedEvents(List<Event> events, LocalDateTime startTime, LocalDateTime endTime) {
        List<Event> conflicts = new ArrayList<>();
        
        for (Event event : events) {
            if (event.getStatus() == EventStatus.CANCELLED) {
                continue;
            }
            
            if (event.getRecurrenceRule() != null && !event.getRecurrenceRule().trim().isEmpty()) {
                // Recurring event - expand and check each occurrence
                List<Event> occurrences = RecurrenceExpander.expandEvent(event, startTime, endTime);
                for (Event occurrence : occurrences) {
                    if (eventsOverlap(occurrence.getStartTime(), occurrence.getEndTime(), startTime, endTime)) {
                        conflicts.add(occurrence);
                    }
                }
            } else {
                // Non-recurring event - check directly
                if (eventsOverlap(event.getStartTime(), event.getEndTime(), startTime, endTime)) {
                    conflicts.add(event);
                }
            }
        }
        
        return conflicts;
    }

    /**
     * Checks if two time ranges overlap.
     */
    private boolean eventsOverlap(LocalDateTime start1, LocalDateTime end1, 
                                  LocalDateTime start2, LocalDateTime end2) {
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    public List<Event> getUpcomingEvents(String sessionId, LocalDateTime startTime) {
        return eventRepository.findBySessionIdAndStartTimeAfterOrderByStartTimeAsc(sessionId, startTime);
    }

    public List<Event> getUpcomingEventsByGoogleUser(String googleUserId, LocalDateTime startTime) {
        return eventRepository.findByGoogleUserIdAndStartTimeAfterOrderByStartTimeAsc(googleUserId, startTime);
    }

    public long getTotalEventCount() {
        return eventRepository.count();
    }

    public List<Event> getAllActiveEvents(String sessionId) {
        return eventRepository.findBySessionIdAndStatusNot(sessionId, EventStatus.CANCELLED);
    }

    public List<Event> getAllActiveEventsByGoogleUser(String googleUserId) {
        return eventRepository.findByGoogleUserIdAndStatusNot(googleUserId, EventStatus.CANCELLED);
    }
}

