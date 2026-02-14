package com.agent.agenticcalendar.service;

import com.agent.agenticcalendar.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending real-time updates via WebSocket
 */
@Service
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Sends a thinking step to the frontend
     */
    public void sendThinkingStep(String sessionId, String thinking, String toolName, String toolCall, String toolResult) {
        try {
            AgentResponse.ThinkingStep step = new AgentResponse.ThinkingStep();
            step.setThinking(thinking);
            step.setToolName(toolName);
            step.setToolCall(toolCall);
            step.setToolResult(toolResult);
            
            messagingTemplate.convertAndSend("/topic/thinking/" + sessionId, step);
            log.debug("Sent thinking step to session {}: {}", sessionId, thinking);
        } catch (Exception e) {
            log.error("Error sending thinking step via WebSocket", e);
        }
    }

    /**
     * Sends a thinking message (without tool call)
     */
    public void sendThinking(String sessionId, String thinking) {
        try {
            messagingTemplate.convertAndSend("/topic/thinking/" + sessionId, 
                new ThinkingMessage("thinking", thinking));
            log.debug("Sent thinking to session {}: {}", sessionId, thinking);
        } catch (Exception e) {
            log.error("Error sending thinking via WebSocket", e);
        }
    }

    /**
     * Sends a final response
     */
    public void sendFinalResponse(String sessionId, String response) {
        try {
            messagingTemplate.convertAndSend("/topic/response/" + sessionId, 
                new ThinkingMessage("response", response));
            log.debug("Sent final response to session {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending final response via WebSocket", e);
        }
    }

    /**
     * Sends an error message
     */
    public void sendError(String sessionId, String error) {
        try {
            messagingTemplate.convertAndSend("/topic/error/" + sessionId, 
                new ThinkingMessage("error", error));
            log.debug("Sent error to session {}: {}", sessionId, error);
        } catch (Exception e) {
            log.error("Error sending error via WebSocket", e);
        }
    }

    /**
     * Simple message wrapper for WebSocket
     */
    public static class ThinkingMessage {
        private String type;
        private String content;

        public ThinkingMessage() {}

        public ThinkingMessage(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

