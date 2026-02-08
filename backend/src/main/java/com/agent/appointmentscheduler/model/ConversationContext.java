package com.agent.appointmentscheduler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages conversation history and context for the agent
 */
public class ConversationContext {
    
    private final List<ConversationMessage> messages;
    private final SessionState sessionState;
    private final String sessionId;
    
    public ConversationContext(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new ArrayList<>();
        this.sessionState = new SessionState();
    }
    
    public void addMessage(ConversationMessage message) {
        messages.add(message);
    }
    
    public void addUserMessage(String content) {
        addMessage(ConversationMessage.user(content));
    }
    
    public void addAssistantMessage(String content) {
        addMessage(ConversationMessage.assistant(content));
    }
    
    public void addThinking(String content) {
        addMessage(ConversationMessage.thinking(content));
    }
    
    public List<ConversationMessage> getMessages() {
        return new ArrayList<>(messages); // Return defensive copy
    }
    
    public List<ConversationMessage> getRecentMessages(int count) {
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }
    
    public SessionState getSessionState() {
        return sessionState;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets conversation history formatted for LLM context
     */
    public String getFormattedHistory(int maxMessages) {
        List<ConversationMessage> recent = getRecentMessages(maxMessages);
        StringBuilder sb = new StringBuilder();
        
        for (ConversationMessage msg : recent) {
            switch (msg.getRole()) {
                case USER:
                    sb.append("User: ").append(msg.getContent()).append("\n");
                    break;
                case ASSISTANT:
                    sb.append("Assistant: ").append(msg.getContent()).append("\n");
                    break;
                case THINKING:
                    sb.append("[Thinking] ").append(msg.getContent()).append("\n");
                    break;
                case SYSTEM:
                    sb.append("[System] ").append(msg.getContent()).append("\n");
                    break;
            }
        }
        
        return sb.toString();
    }
}

