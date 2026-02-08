package com.agent.appointmentscheduler.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to parse tool calls from Ollama responses that return them as JSON text
 */
public class ToolCallParser {
    
    private static final Logger log = LoggerFactory.getLogger(ToolCallParser.class);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Extracts tool calls from response text that contains JSON function calls
     * Handles JSON in code blocks, multi-line JSON, and inline JSON
     * Example: {"name": "createAppointment", "parameters": {...}}
     */
    public static List<ToolCall> parseToolCallsFromText(String responseText) {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        if (responseText == null || responseText.isEmpty()) {
            return toolCalls;
        }
        
        // First, try to extract JSON from code blocks (```json ... ``` or ``` ... ```)
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(responseText);
        while (codeBlockMatcher.find()) {
            String codeBlockContent = codeBlockMatcher.group(1).trim();
            log.debug("Found code block content: {}", codeBlockContent.substring(0, Math.min(200, codeBlockContent.length())));
            ToolCall toolCall = parseJsonToolCall(codeBlockContent);
            if (toolCall != null) {
                toolCalls.add(toolCall);
            }
        }
        
        // Also try to find JSON objects that span multiple lines (common in LLM responses)
        if (toolCalls.isEmpty()) {
            // Try multiple patterns to find the start of a JSON object
            String[] startPatterns = {"{\"name\"", "{\n\"name\"", "{ \"name\"", "{\n \"name\"", "{\"name\":"};
            
            // Use a more flexible approach: find all occurrences of {"name" (with variations)
            int startIdx = -1;
            for (String pattern : startPatterns) {
                startIdx = responseText.indexOf(pattern);
                if (startIdx != -1) {
                    log.debug("Found potential JSON start with pattern '{}' at index {}", pattern, startIdx);
                    break;
                }
            }
            
            while (startIdx != -1) {
                // Find the matching closing brace
                int braceCount = 0;
                int endIdx = startIdx;
                boolean inString = false;
                char stringChar = 0;
                
                for (int i = startIdx; i < responseText.length(); i++) {
                    char c = responseText.charAt(i);
                    
                    if (!inString && (c == '"' || c == '\'')) {
                        inString = true;
                        stringChar = c;
                    } else if (inString && c == stringChar && responseText.charAt(i - 1) != '\\') {
                        inString = false;
                    } else if (!inString) {
                        if (c == '{') {
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                endIdx = i + 1;
                                break;
                            }
                        }
                    }
                }
                
                if (endIdx > startIdx && braceCount == 0) {
                    String jsonStr = responseText.substring(startIdx, endIdx);
                    log.debug("Extracted JSON string ({} chars): {}", jsonStr.length(), jsonStr.substring(0, Math.min(200, jsonStr.length())));
                    ToolCall toolCall = parseJsonToolCall(jsonStr);
                    if (toolCall != null) {
                        toolCalls.add(toolCall);
                    }
                }
                
                // Find next occurrence
                startIdx = -1;
                for (String pattern : startPatterns) {
                    int nextIdx = responseText.indexOf(pattern, endIdx);
                    if (nextIdx != -1 && (startIdx == -1 || nextIdx < startIdx)) {
                        startIdx = nextIdx;
                    }
                }
            }
        }
        
        // Remove duplicates (same name and parameters)
        return toolCalls.stream()
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Attempts to parse a JSON string as a tool call
     */
    private static ToolCall parseJsonToolCall(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Clean up the JSON string (remove extra whitespace, handle multi-line)
            jsonStr = jsonStr.trim();
            // Normalize whitespace: replace newlines and multiple spaces with single space
            jsonStr = jsonStr.replaceAll("\\s+", " ");
            // Handle common formatting issues
            jsonStr = jsonStr.replaceAll(",\\s*}", "}");
            jsonStr = jsonStr.replaceAll(",\\s*]", "]");
            
            log.debug("Attempting to parse JSON: {}", jsonStr.substring(0, Math.min(300, jsonStr.length())));
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            if (jsonNode.has("name") && jsonNode.has("parameters")) {
                String name = jsonNode.get("name").asText();
                JsonNode parametersNode = jsonNode.get("parameters");
                Map<String, Object> parameters = objectMapper.convertValue(parametersNode, Map.class);
                
                log.info("🔍 Parsed tool call: name={}, parameters={}", name, parameters);
                return new ToolCall(name, parameters);
            } else {
                log.debug("JSON object found but missing 'name' or 'parameters' field");
            }
        } catch (Exception e) {
            log.debug("Failed to parse JSON tool call (first 200 chars: '{}'): {}", 
                    jsonStr.substring(0, Math.min(200, jsonStr.length())), e.getMessage());
        }
        
        return null;
    }
    
    public static class ToolCall {
        private final String name;
        private final Map<String, Object> parameters;
        
        public ToolCall(String name, Map<String, Object> parameters) {
            this.name = name;
            this.parameters = parameters;
        }
        
        public String getName() {
            return name;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ToolCall toolCall = (ToolCall) o;
            return java.util.Objects.equals(name, toolCall.name) &&
                   java.util.Objects.equals(parameters, toolCall.parameters);
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, parameters);
        }
        
        @Override
        public String toString() {
            return "ToolCall{name='" + name + "', parameters=" + parameters + "}";
        }
    }
}

