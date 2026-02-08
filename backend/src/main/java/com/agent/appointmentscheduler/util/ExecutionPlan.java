package com.agent.appointmentscheduler.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents an execution plan created by the LLM
 */
public class ExecutionPlan {
    
    @JsonProperty("plan")
    private String plan; // Human-readable step-by-step plan
    
    @JsonProperty("steps")
    private List<ExecutionStep> steps;
    
    public String getPlan() {
        return plan;
    }
    
    public void setPlan(String plan) {
        this.plan = plan;
    }
    
    public List<ExecutionStep> getSteps() {
        return steps;
    }
    
    public void setSteps(List<ExecutionStep> steps) {
        this.steps = steps;
    }
    
    public static class ExecutionStep {
        @JsonProperty("stepNumber")
        private Integer stepNumber;
        
        @JsonProperty("toolName")
        private String toolName;
        
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        
        @JsonProperty("expectedResult")
        private String expectedResult; // Description of what we expect
        
        @JsonProperty("extractFromResult")
        private Map<String, String> extractFromResult; // What to extract (e.g., {"userId": "userId"})
        
        @JsonProperty("onError")
        private ErrorHandling onError;
        
        @JsonProperty("condition")
        private String condition; // Optional condition to execute this step (e.g., "if userId is null")
        
        public Integer getStepNumber() {
            return stepNumber;
        }
        
        public void setStepNumber(Integer stepNumber) {
            this.stepNumber = stepNumber;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public void setToolName(String toolName) {
            this.toolName = toolName;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
        
        public String getExpectedResult() {
            return expectedResult;
        }
        
        public void setExpectedResult(String expectedResult) {
            this.expectedResult = expectedResult;
        }
        
        public Map<String, String> getExtractFromResult() {
            return extractFromResult;
        }
        
        public void setExtractFromResult(Map<String, String> extractFromResult) {
            this.extractFromResult = extractFromResult;
        }
        
        public ErrorHandling getOnError() {
            return onError;
        }
        
        public void setOnError(ErrorHandling onError) {
            this.onError = onError;
        }
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
    }
    
    public static class ErrorHandling {
        @JsonProperty("action")
        private String action; // "retry", "skip", "abort", "fallback"
        
        @JsonProperty("fallbackTool")
        private String fallbackTool; // If action is "fallback"
        
        @JsonProperty("fallbackParameters")
        private Map<String, Object> fallbackParameters;
        
        @JsonProperty("message")
        private String message; // Message to return to user
        
        public String getAction() {
            return action;
        }
        
        public void setAction(String action) {
            this.action = action;
        }
        
        public String getFallbackTool() {
            return fallbackTool;
        }
        
        public void setFallbackTool(String fallbackTool) {
            this.fallbackTool = fallbackTool;
        }
        
        public Map<String, Object> getFallbackParameters() {
            return fallbackParameters;
        }
        
        public void setFallbackParameters(Map<String, Object> fallbackParameters) {
            this.fallbackParameters = fallbackParameters;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}

