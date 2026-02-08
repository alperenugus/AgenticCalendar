package com.agent.appointmentscheduler.util;

import com.agent.appointmentscheduler.tools.AppointmentTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes an execution plan created by the LLM
 */
public class PlanExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);
    private final Map<String, FunctionCallback> functionCallbacks;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> executionContext; // Stores extracted values from previous steps
    
    public PlanExecutor(Map<String, FunctionCallback> functionCallbacks, ObjectMapper objectMapper) {
        this.functionCallbacks = functionCallbacks;
        this.objectMapper = objectMapper;
        this.executionContext = new HashMap<>();
    }
    
    /**
     * Executes the plan step by step
     */
    public ExecutionResult execute(ExecutionPlan plan) {
        log.info("📋 Executing plan with {} steps", plan.getSteps() != null ? plan.getSteps().size() : 0);
        log.info("Plan: {}", plan.getPlan());
        
        if (plan.getSteps() == null || plan.getSteps().isEmpty()) {
            return new ExecutionResult(false, "No steps in plan", null);
        }
        
        StringBuilder executionLog = new StringBuilder();
        executionLog.append("Execution Log:\n");
        
        for (ExecutionPlan.ExecutionStep step : plan.getSteps()) {
            log.info("🔵 Executing step {}: {}", step.getStepNumber(), step.getToolName());
            
            // Check condition if present
            if (step.getCondition() != null && !evaluateCondition(step.getCondition())) {
                log.info("⏭️ Skipping step {} due to condition: {}", step.getStepNumber(), step.getCondition());
                continue;
            }
            
            // Resolve parameters (replace placeholders with values from execution context)
            Map<String, Object> resolvedParameters = resolveParameters(step.getParameters());
            log.info("  Parameters: {}", resolvedParameters);
            
            // Execute the tool
            FunctionCallback callback = functionCallbacks.get(step.getToolName());
            if (callback == null) {
                String errorMsg = "Tool '" + step.getToolName() + "' not found";
                log.error("❌ {}", errorMsg);
                executionLog.append("Step ").append(step.getStepNumber()).append(" failed: ").append(errorMsg).append("\n");
                
                // Handle error
                if (step.getOnError() != null) {
                    ExecutionResult errorResult = handleError(step, null, errorMsg);
                    if (errorResult != null) {
                        return errorResult;
                    }
                }
                continue;
            }
            
            try {
                String argumentsJson = objectMapper.writeValueAsString(resolvedParameters);
                log.debug("Calling {} with arguments: {}", step.getToolName(), argumentsJson);
                Object result = callback.call(argumentsJson);
                log.debug("Raw result type: {}, value: {}", result != null ? result.getClass().getName() : "null", result);
                
                // Extract values from result FIRST (before any string conversion)
                // This must happen before any string conversion to preserve the object structure
                if (step.getExtractFromResult() != null && !step.getExtractFromResult().isEmpty()) {
                    extractValuesFromResult(result, step.getExtractFromResult());
                }
                
                String resultStr = result != null ? result.toString() : "null";
                log.info("✅ Step {} executed successfully. Result: {}", step.getStepNumber(), resultStr);
                executionLog.append("Step ").append(step.getStepNumber()).append(" (").append(step.getToolName())
                        .append(") executed successfully. Result: ").append(resultStr).append("\n");
                
            } catch (Exception e) {
                log.error("❌ Step {} failed: {}", step.getStepNumber(), e.getMessage(), e);
                executionLog.append("Step ").append(step.getStepNumber()).append(" failed: ").append(e.getMessage()).append("\n");
                
                // Handle error according to plan
                if (step.getOnError() != null) {
                    ExecutionResult errorResult = handleError(step, e, e.getMessage());
                    if (errorResult != null) {
                        return errorResult;
                    }
                }
            }
        }
        
        return new ExecutionResult(true, "Plan executed successfully", executionLog.toString());
    }
    
    /**
     * Resolves parameters by replacing placeholders with values from execution context
     */
    private Map<String, Object> resolveParameters(Map<String, Object> parameters) {
        Map<String, Object> resolved = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            
            // If value is a string that looks like a placeholder (e.g., "${userId}"), resolve it
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("${") && strValue.endsWith("}")) {
                    String key = strValue.substring(2, strValue.length() - 1);
                    Object contextValue = executionContext.get(key);
                    if (contextValue != null) {
                        log.info("🔧 Resolved placeholder ${} to value: {} (type: {})", key, contextValue, contextValue.getClass().getSimpleName());
                        resolved.put(entry.getKey(), contextValue);
                    } else {
                        log.warn("⚠️ Placeholder ${} not found in execution context. Available keys: {}", key, executionContext.keySet());
                        resolved.put(entry.getKey(), value); // Keep original if not found
                    }
                } else {
                    resolved.put(entry.getKey(), value);
                }
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        
        return resolved;
    }
    
    /**
     * Extracts values from the result and stores them in execution context
     */
    private void extractValuesFromResult(Object result, Map<String, String> extractMap) {
        if (result == null) {
            log.warn("Cannot extract values from null result");
            return;
        }
        
        try {
            Map<String, Object> resultMap = null;
            
            // Handle different result types
            if (result instanceof Map) {
                resultMap = (Map<String, Object>) result;
                log.debug("Result is already a Map");
            } else if (result instanceof String) {
                // Result might be a JSON string - try to parse it
                String jsonStr = (String) result;
                try {
                    resultMap = objectMapper.readValue(jsonStr, 
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                    log.debug("Parsed result from JSON string");
                } catch (Exception e) {
                    log.debug("Result is not JSON string: {}", e.getMessage());
                }
            } else {
                // For record types or other objects, convert to Map using ObjectMapper
                // Try JSON round-trip first (most reliable for records)
                try {
                    String json = objectMapper.writeValueAsString(result);
                    log.debug("Serialized result to JSON: {}", json);
                    resultMap = objectMapper.readValue(json, 
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                    log.debug("Successfully converted result via JSON round-trip. Keys: {}", resultMap.keySet());
                } catch (Exception e) {
                    log.debug("JSON round-trip failed, trying convertValue: {}", e.getMessage());
                    // Fallback: use convertValue (works for records and POJOs)
                    try {
                        resultMap = objectMapper.convertValue(result, 
                                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                        log.debug("Successfully converted result using convertValue. Keys: {}", resultMap.keySet());
                    } catch (Exception e2) {
                        log.warn("Both conversion methods failed: {}", e2.getMessage());
                    }
                }
            }
            
            if (resultMap != null) {
                log.debug("Result map keys: {}", resultMap.keySet());
                for (Map.Entry<String, String> entry : extractMap.entrySet()) {
                    String contextKey = entry.getKey();
                    String resultKey = entry.getValue();
                    
                    Object value = extractValueByPath(resultMap, resultKey);
                    
                    // Fallback: If extraction failed and we're looking for appointmentId, try common paths
                    if (value == null && "appointmentId".equals(contextKey)) {
                        log.debug("Primary extraction failed for appointmentId, trying fallback paths...");
                        // Try common paths for appointmentId
                        String[] fallbackPaths = {
                            "appointments[0].appointmentId",
                            "appointments[0].id",
                            "appointment.id",
                            "appointment.appointmentId"
                        };
                        for (String fallbackPath : fallbackPaths) {
                            value = extractValueByPath(resultMap, fallbackPath);
                            if (value != null) {
                                log.info("✅ Found appointmentId using fallback path: {}", fallbackPath);
                                break;
                            }
                        }
                    }
                    
                    if (value != null) {
                        executionContext.put(contextKey, value);
                        log.info("📦 Extracted {} = {} from result using path '{}' (type: {})", 
                                contextKey, value, resultKey, value.getClass().getSimpleName());
                    } else {
                        log.warn("⚠️ Path '{}' not found in result map. Available keys: {}", resultKey, resultMap.keySet());
                        // Log the actual structure for debugging
                        if (resultMap.containsKey("appointments")) {
                            Object appointments = resultMap.get("appointments");
                            if (appointments instanceof List) {
                                List<?> apptList = (List<?>) appointments;
                                if (!apptList.isEmpty()) {
                                    log.debug("Appointments array contains {} items. First item: {}", apptList.size(), apptList.get(0));
                                }
                            }
                        }
                    }
                }
            } else {
                log.warn("Could not convert result to Map for extraction. Result type: {}", result != null ? result.getClass().getName() : "null");
            }
        } catch (Exception e) {
            log.error("Failed to extract values from result: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Extracts a value from a Map using a JSON path expression
     * Supports:
     * - Simple keys: "userId"
     * - Nested keys: "appointments[0].appointmentId"
     * - Array access: "[0].id" (assumes root is array)
     * - Array access with key: "appointments[0].appointmentId"
     */
    private Object extractValueByPath(Map<String, Object> map, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Handle simple key (no path)
        if (!path.contains(".") && !path.contains("[")) {
            return map.get(path);
        }
        
        // Handle JSON path expressions
        try {
            // Split by "." to get segments
            String[] segments = path.split("\\.");
            Object current = map;
            
            for (String segment : segments) {
                if (current == null) {
                    return null;
                }
                
                // Check if segment has array access [index]
                if (segment.contains("[")) {
                    int bracketStart = segment.indexOf("[");
                    int bracketEnd = segment.indexOf("]");
                    
                    if (bracketEnd > bracketStart) {
                        String key = segment.substring(0, bracketStart);
                        String indexStr = segment.substring(bracketStart + 1, bracketEnd);
                        
                        // Get the object/array
                        if (key.isEmpty()) {
                            // Array access at root level: [0].id
                            if (current instanceof List) {
                                try {
                                    int index = Integer.parseInt(indexStr);
                                    List<?> list = (List<?>) current;
                                    if (index >= 0 && index < list.size()) {
                                        current = list.get(index);
                                    } else {
                                        return null;
                                    }
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid array index in path '{}': {}", path, indexStr);
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        } else {
                            // Key with array access: appointments[0]
                            if (current instanceof Map) {
                                Map<String, Object> currentMap = (Map<String, Object>) current;
                                Object obj = currentMap.get(key);
                                if (obj instanceof List) {
                                    try {
                                        int index = Integer.parseInt(indexStr);
                                        List<?> list = (List<?>) obj;
                                        if (index >= 0 && index < list.size()) {
                                            current = list.get(index);
                                        } else {
                                            return null;
                                        }
                                    } catch (NumberFormatException e) {
                                        log.warn("Invalid array index in path '{}': {}", path, indexStr);
                                        return null;
                                    }
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        }
                    }
                } else {
                    // Simple key access
                    if (current instanceof Map) {
                        current = ((Map<String, Object>) current).get(segment);
                    } else {
                        return null;
                    }
                }
            }
            
            return current;
        } catch (Exception e) {
            log.warn("Error extracting value by path '{}': {}", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * Evaluates a condition (simple check for now)
     */
    private boolean evaluateCondition(String condition) {
        // Simple condition evaluation - can be enhanced
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }
        
        // Check if condition references a value in context
        // For now, just check if the referenced value exists and is not null
        String lowerCondition = condition.toLowerCase();
        if (lowerCondition.contains("if") && lowerCondition.contains("is null")) {
            // Extract variable name
            String varName = condition.replaceAll(".*if\\s+(\\w+).*", "$1");
            return executionContext.get(varName) == null;
        }
        
        return true; // Default to true if condition can't be evaluated
    }
    
    /**
     * Handles errors according to the plan's error handling strategy
     */
    private ExecutionResult handleError(ExecutionPlan.ExecutionStep step, Exception exception, String errorMessage) {
        ExecutionPlan.ErrorHandling errorHandling = step.getOnError();
        if (errorHandling == null) {
            return null;
        }
        
        String action = errorHandling.getAction();
        log.info("🔧 Handling error with action: {}", action);
        
        switch (action.toLowerCase()) {
            case "abort":
                return new ExecutionResult(false, errorHandling.getMessage() != null ? 
                        errorHandling.getMessage() : errorMessage, null);
                
            case "skip":
                log.info("⏭️ Skipping step {} as per error handling", step.getStepNumber());
                return null; // Continue execution
                
            case "fallback":
                if (errorHandling.getFallbackTool() != null) {
                    log.info("🔄 Executing fallback tool: {}", errorHandling.getFallbackTool());
                    try {
                        FunctionCallback fallbackCallback = functionCallbacks.get(errorHandling.getFallbackTool());
                        if (fallbackCallback != null) {
                            Map<String, Object> fallbackParams = resolveParameters(
                                    errorHandling.getFallbackParameters() != null ? 
                                    errorHandling.getFallbackParameters() : new HashMap<>());
                            String fallbackArgsJson = objectMapper.writeValueAsString(fallbackParams);
                            Object fallbackResult = fallbackCallback.call(fallbackArgsJson);
                            log.info("✅ Fallback executed successfully: {}", fallbackResult);
                            return null; // Continue execution
                        }
                    } catch (Exception e) {
                        log.error("❌ Fallback execution failed: {}", e.getMessage());
                    }
                }
                break;
        }
        
        return null;
    }
    
    /**
     * Result of plan execution
     */
    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final String executionLog;
        
        public ExecutionResult(boolean success, String message, String executionLog) {
            this.success = success;
            this.message = message;
            this.executionLog = executionLog;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getExecutionLog() {
            return executionLog;
        }
    }
}

