package com.agent.appointmentscheduler.model;

import java.util.List;

/**
 * Response model for agent that includes thinking steps and tool calls
 */
public class AgentResponse {
    
    private String finalResponse;
    private List<ThinkingStep> thinkingSteps;
    
    public AgentResponse() {}
    
    public AgentResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }
    
    public AgentResponse(String finalResponse, List<ThinkingStep> thinkingSteps) {
        this.finalResponse = finalResponse;
        this.thinkingSteps = thinkingSteps;
    }
    
    public String getFinalResponse() {
        return finalResponse;
    }
    
    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }
    
    public List<ThinkingStep> getThinkingSteps() {
        return thinkingSteps;
    }
    
    public void setThinkingSteps(List<ThinkingStep> thinkingSteps) {
        this.thinkingSteps = thinkingSteps;
    }
    
    public static class ThinkingStep {
        private String thinking;
        private String toolCall;
        private String toolResult;
        private String toolName;
        
        public ThinkingStep() {}
        
        public ThinkingStep(String thinking) {
            this.thinking = thinking;
        }
        
        public ThinkingStep(String thinking, String toolName, String toolCall, String toolResult) {
            this.thinking = thinking;
            this.toolName = toolName;
            this.toolCall = toolCall;
            this.toolResult = toolResult;
        }
        
        public String getThinking() {
            return thinking;
        }
        
        public void setThinking(String thinking) {
            this.thinking = thinking;
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
        
        public String getToolName() {
            return toolName;
        }
        
        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
    }
}



