package com.agent.agenticcalendar.controller;

import com.agent.agenticcalendar.model.Event;
import com.agent.agenticcalendar.repository.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String googleUserId) {
        try {
            List<Event> events;
            if (googleUserId != null && !googleUserId.isEmpty()) {
                events = eventRepository.findByGoogleUserId(googleUserId);
            } else if (sessionId != null && !sessionId.isEmpty()) {
                events = eventRepository.findBySessionId(sessionId);
            } else {
                events = eventRepository.findAll();
            }
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public ResponseEntity<EventCountResponse> getEventCount() {
        long count = eventRepository.count();
        return ResponseEntity.ok(new EventCountResponse(count));
    }

    public record EventCountResponse(long count) {}
}

