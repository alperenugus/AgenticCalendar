package com.agent.appointmentscheduler.controller;

import com.agent.appointmentscheduler.exception.RateLimitExceededException;
import com.agent.appointmentscheduler.model.AgentResponse;
import com.agent.appointmentscheduler.model.ConversationMessage;
import com.agent.appointmentscheduler.service.AgentService;
import com.agent.appointmentscheduler.service.RateLimitService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final RateLimitService rateLimitService;

    public AgentController(AgentService agentService, RateLimitService rateLimitService) {
        this.agentService = agentService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(
            @RequestBody AgentRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "X-Google-User-Id", required = false) String googleUserId,
            @RequestHeader(value = "X-Google-User-Email", required = false) String googleUserEmail) {
        try {
            // Use provided session ID or default
            String effectiveSessionId = sessionId != null && !sessionId.trim().isEmpty() 
                    ? sessionId 
                    : "default";
            
            // Check rate limit before processing
            rateLimitService.checkChatRateLimit(effectiveSessionId);
            
            AgentResponse response = agentService.processUserMessage(
                    request.message(), 
                    effectiveSessionId, 
                    googleUserId, 
                    googleUserEmail
            );
            
            // Add rate limit headers to successful response
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingChatTokens(effectiveSessionId)));
            
            return ResponseEntity.ok().headers(headers).body(response);
        } catch (RateLimitExceededException e) {
            // Return 429 Too Many Requests with proper headers
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-RateLimit-Remaining", String.valueOf(e.getRemainingTokens()));
            headers.add("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            headers.add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + e.getRetryAfterSeconds()));
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(new AgentResponse(e.getMessage()));
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
            
            // Check rate limit before processing
            rateLimitService.checkHistoryRateLimit(effectiveSessionId);
            
            List<ConversationMessage> history = agentService.getConversationHistory(effectiveSessionId);
            return ResponseEntity.ok(history);
        } catch (RateLimitExceededException e) {
            // Return 429 Too Many Requests
            HttpHeaders headers = new HttpHeaders();
            headers.add("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(List.of());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    public record AgentRequest(String message) {}
}

