package com.agent.agenticcalendar.model;

import java.time.LocalDateTime;

/**
 * Represents a message in the conversation history
 */
public class ConversationMessage {
    
    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM,
        THINKING  // For transparent reasoning steps
    }
    
    private Role role;
    private String content;
    private LocalDateTime timestamp;
    private String toolCall; // If this message includes a tool call
    private String toolResult; // If this message includes a tool result
    
    public ConversationMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ConversationMessage(Role role, String content) {
        this();
        this.role = role;
        this.content = content;
    }
    
    public static ConversationMessage user(String content) {
        return new ConversationMessage(Role.USER, content);
    }
    
    public static ConversationMessage assistant(String content) {
        return new ConversationMessage(Role.ASSISTANT, content);
    }
    
    public static ConversationMessage thinking(String content) {
        return new ConversationMessage(Role.THINKING, content);
    }
    
    public static ConversationMessage system(String content) {
        return new ConversationMessage(Role.SYSTEM, content);
    }
    
    // Getters and Setters
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getToolCall() {
        return toolCall;
    }
    
    public void setToolCall(String toolCall) {
        this.toolCall = toolCall;
    }
    
    public String getToolResult() {
        return toolResult;
    }
    
    public void setToolResult(String toolResult) {
        this.toolResult = toolResult;
    }
}



