package com.agent.appointmentscheduler.controller;

import com.agent.appointmentscheduler.model.AgentResponse;
import com.agent.appointmentscheduler.model.ConversationMessage;
import com.agent.appointmentscheduler.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(
            @RequestBody AgentRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        try {
            // Use provided session ID or default
            String effectiveSessionId = sessionId != null && !sessionId.trim().isEmpty() 
                    ? sessionId 
                    : "default";
            
            AgentResponse response = agentService.processUserMessage(request.message(), effectiveSessionId);
            return ResponseEntity.ok(response);
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

    @GetMapping("/history")
    public ResponseEntity<List<ConversationMessage>> getHistory(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        try {
            // Use provided session ID or default
            String effectiveSessionId = sessionId != null && !sessionId.trim().isEmpty() 
                    ? sessionId 
                    : "default";
            
            List<ConversationMessage> history = agentService.getConversationHistory(effectiveSessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public record AgentRequest(String message) {}
}

