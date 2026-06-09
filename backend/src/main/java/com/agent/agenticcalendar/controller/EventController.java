package com.agent.agenticcalendar.controller;

import com.agent.agenticcalendar.model.Event;
import com.agent.agenticcalendar.repository.EventRepository;
import com.agent.agenticcalendar.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Returns events for the authenticated user only. Ownership is derived from the
     * session principal, never from a client-supplied query parameter.
     */
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(Authentication authentication) {
        String ownerKey = CurrentUser.ownerKey(authentication);
        if (ownerKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(eventRepository.findByGoogleUserId(ownerKey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id, Authentication authentication) {
        String ownerKey = CurrentUser.ownerKey(authentication);
        if (ownerKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return eventRepository.findById(id)
                .filter(event -> ownerKey.equals(event.getGoogleUserId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public ResponseEntity<EventCountResponse> getEventCount(Authentication authentication) {
        String ownerKey = CurrentUser.ownerKey(authentication);
        if (ownerKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        long count = eventRepository.findByGoogleUserId(ownerKey).size();
        return ResponseEntity.ok(new EventCountResponse(count));
    }

    public record EventCountResponse(long count) {}
}
