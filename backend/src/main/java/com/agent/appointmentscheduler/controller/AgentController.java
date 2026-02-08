package com.agent.appointmentscheduler.controller;

import com.agent.appointmentscheduler.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@RequestBody AgentRequest request) {
        try {
            String response = agentService.processUserMessage(request.message());
            return ResponseEntity.ok(new AgentResponse(response));
        } catch (IllegalArgumentException e) {
            // Return validation errors as user-friendly messages
            return ResponseEntity.badRequest()
                    .body(new AgentResponse("I'm sorry, but I couldn't process your request: " + e.getMessage()));
        } catch (Exception e) {
            // Log unexpected errors but don't expose details to user
            return ResponseEntity.status(500)
                    .body(new AgentResponse("I'm sorry, an error occurred while processing your request. Please try again."));
        }
    }

    public record AgentRequest(String message) {}
    public record AgentResponse(String response) {}
}

