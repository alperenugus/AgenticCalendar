package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.model.Event;
import com.agent.appointmentscheduler.model.EventStatus;
import com.agent.appointmentscheduler.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        Event event = new Event(title, startTime, endTime, description, sessionId);
        if (googleUserId != null) {
            event.setGoogleUserId(googleUserId);
            event.setGoogleUserEmail(googleUserEmail);
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

    public List<Event> getEventsBySessionIdAndDateRange(String sessionId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findBySessionIdAndStartTimeBetween(sessionId, start, end);
    }

    public List<Event> getEventsByGoogleUserIdAndDateRange(String googleUserId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByGoogleUserIdAndStartTimeBetween(googleUserId, start, end);
    }

    public List<Event> findConflictingEvents(String sessionId, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findConflictingEvents(sessionId, startTime, endTime);
    }

    public List<Event> findConflictingEventsByGoogleUser(String googleUserId, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findConflictingEventsByGoogleUser(googleUserId, startTime, endTime);
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

