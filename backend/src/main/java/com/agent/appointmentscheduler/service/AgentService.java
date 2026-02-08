package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.tools.AppointmentTools;
import com.agent.appointmentscheduler.util.ExecutionPlan;
import com.agent.appointmentscheduler.util.MessageExtractor;
import com.agent.appointmentscheduler.util.PlanExecutor;
import com.agent.appointmentscheduler.util.ToolCallParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ChatClient chatClient;
    private final ChatModel chatModel; // Store ChatModel separately for plan generation
    private final InputValidationService inputValidationService;
    private final Map<String, FunctionCallback> functionCallbacks;
    private final ObjectMapper objectMapper;

    public AgentService(
            @Qualifier("ollamaChatModel") ChatModel chatModel,
            @Qualifier("getUserFunction") FunctionCallback getUserFunction,
            @Qualifier("getAppointmentsByUserFunction") FunctionCallback getAppointmentsByUserFunction,
            @Qualifier("createAppointmentFunction") FunctionCallback createAppointmentFunction,
            @Qualifier("updateAppointmentFunction") FunctionCallback updateAppointmentFunction,
            @Qualifier("deleteAppointmentFunction") FunctionCallback deleteAppointmentFunction,
            InputValidationService inputValidationService,
            ObjectMapper objectMapper
    ) {
        this.chatModel = chatModel;
        this.inputValidationService = inputValidationService;
        this.objectMapper = objectMapper;
        
        // Store function callbacks for manual execution
        this.functionCallbacks = new HashMap<>();
        this.functionCallbacks.put("getUser", getUserFunction);
        this.functionCallbacks.put("getAppointmentsByUser", getAppointmentsByUserFunction);
        this.functionCallbacks.put("createAppointment", createAppointmentFunction);
        this.functionCallbacks.put("updateAppointment", updateAppointmentFunction);
        this.functionCallbacks.put("deleteAppointment", deleteAppointmentFunction);
        
        // Build ChatClient with the specified ChatModel
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
        // Register all tool functions with the ChatClient (for final response generation)
        this.chatClient = chatClientBuilder
                .defaultFunctions(
                        getUserFunction,
                        getAppointmentsByUserFunction,
                        createAppointmentFunction,
                        updateAppointmentFunction,
                        deleteAppointmentFunction
                )
                .build();
        
        log.info("✅ ChatClient initialized with {} functions registered", 5);
        log.info("Registered functions: getUser, getAppointmentsByUser, createAppointment, updateAppointment, deleteAppointment");
    }

    public String processUserMessage(String userMessage) {
        // Validate and sanitize input first
        String sanitizedMessage = inputValidationService.validateAndSanitize(userMessage);
        
        // Extract structured information from the message to help the LLM
        String enhancedMessage = MessageExtractor.enhanceMessageWithExtractedInfo(sanitizedMessage);
        
        // System prompt for planning-based approach
        // IMPORTANT: We use a separate ChatClient WITHOUT functions for plan generation
        // to prevent the LLM from calling functions directly instead of returning a plan
        String systemPrompt = """
                You are a planning assistant. Your ONLY job is to create a JSON execution plan.
                
                **CRITICAL INSTRUCTIONS:**
                - DO NOT call any functions or tools
                - DO NOT execute any actions
                - ONLY return a JSON object with the execution plan
                - NO explanatory text before or after the JSON
                - NO markdown code blocks (just raw JSON)
                
                **Your Process:**
                1. Analyze the user's request
                2. Create a step-by-step plan
                3. Return ONLY a JSON object with this exact structure:
                
                {
                  "plan": "Human-readable description of the plan",
                  "steps": [
                    {
                      "stepNumber": 1,
                      "toolName": "getUser",
                      "parameters": {
                        "firstName": "Alperen",
                        "lastName": "Ugus",
                        "dob": "1990-01-01"
                      },
                      "expectedResult": "User object with userId",
                      "extractFromResult": {
                        "userId": "userId"
                      },
                      "onError": {
                        "action": "abort",
                        "message": "User not found. Please provide correct information."
                      }
                    },
                    {
                      "stepNumber": 2,
                      "toolName": "createAppointment",
                      "parameters": {
                        "userId": "${userId}",
                        "appointmentDateTime": "2024-12-25T14:00:00",
                        "description": "dental checkup"
                      },
                      "expectedResult": "Appointment created successfully",
                      "onError": {
                        "action": "abort",
                        "message": "Failed to create appointment"
                      }
                    }
                  ]
                }
                
                **Available Tools (for reference only - DO NOT call them):**
                - getUser: Look up user by firstName, lastName, dob. Returns userId.
                - getAppointmentsByUser: Get all appointments for a userId. Returns list of appointments with IDs.
                - createAppointment: Create appointment. Requires userId (numeric), appointmentDateTime (ISO format), description.
                - updateAppointment: Update appointment. Requires appointmentId (numeric), newDateTime (ISO format).
                - deleteAppointment: Delete appointment. Requires appointmentId (numeric).
                
                **Parameter Resolution:**
                - Use "${variableName}" to reference values extracted from previous steps
                - Example: If step 1 extracts userId, step 2 can use "${userId}" in parameters
                
                **Extracting Values from Results:**
                - Use JSON path expressions in "extractFromResult"
                - Simple key: "userId" extracts the userId field directly
                - Nested path: "appointments[0].appointmentId" extracts appointmentId from first appointment in array
                - Array access: "appointments[0]" gets the first element, then ".appointmentId" gets the field
                - CRITICAL: getAppointmentsByUser returns: {"userId": 1, "appointments": [{"appointmentId": 5, ...}], "message": "..."}
                  To extract appointmentId, you MUST use: "appointments[0].appointmentId"
                  DO NOT use just "appointmentId" - it doesn't exist at the root level!
                - Example for getAppointmentsByUser:
                  "extractFromResult": {
                    "appointmentId": "appointments[0].appointmentId"
                  }
                
                **Error Handling:**
                - "abort": Stop execution and return error message
                - "skip": Skip this step and continue
                - "fallback": Try an alternative tool (specify fallbackTool and fallbackParameters)
                
                **Important:**
                - Always include getUser as step 1 if you need a userId
                - Extract userId from getUser result before calling createAppointment
                - Extract appointmentId from getAppointmentsByUser result before update/delete
                - Use correct JSON paths: "appointments[0].appointmentId" NOT "[0].id"
                - Convert dates to ISO format: yyyy-MM-ddTHH:mm:ss
                - Be specific about what to extract from each step's result
                
                **REMEMBER: Return ONLY the JSON object, nothing else.**
                """;

        log.info("Processing user message: {}", enhancedMessage);
        
        try {
            // Step 1: Ask LLM to create an execution plan
            // Use a ChatClient WITHOUT functions to prevent LLM from calling functions directly
            ChatClient planChatClient = ChatClient.builder(chatModel)
                    .defaultSystem(systemPrompt)
                    .build();
            
            ChatResponse planResponse = planChatClient.prompt()
                    .user(enhancedMessage)
                    .call()
                    .chatResponse();

            String planContent = planResponse.getResult().getOutput().getContent();
            log.info("📋 LLM Plan Response: {}", planContent);
            
            // Step 2: Parse the execution plan from JSON
            ExecutionPlan plan = parseExecutionPlan(planContent);
            
            if (plan == null) {
                log.warn("⚠️ Failed to parse execution plan, falling back to legacy approach");
                return handleLegacyResponse(planContent, enhancedMessage);
            }
            
            // Step 3: Execute the plan
            PlanExecutor executor = new PlanExecutor(functionCallbacks, objectMapper);
            PlanExecutor.ExecutionResult result = executor.execute(plan);
            
            // Step 4: Generate final response based on execution result
            if (result.isSuccess()) {
                String finalResponse = chatClient.prompt()
                        .system("You are a helpful assistant. Summarize what was accomplished based on the execution log.")
                        .user("User request: " + enhancedMessage)
                        .user("Execution log:\n" + result.getExecutionLog())
                        .call()
                        .content();
                return finalResponse;
            } else {
                return result.getMessage();
            }
            
        } catch (Exception e) {
            log.error("Error processing message", e);
            throw e;
        }
    }
    
    /**
     * Parses execution plan from LLM response
     */
    private ExecutionPlan parseExecutionPlan(String content) {
        try {
            // Try to extract JSON from code blocks first
            String jsonContent = content.trim();
            
            // Remove markdown code blocks if present
            if (jsonContent.contains("```json")) {
                int start = jsonContent.indexOf("```json") + 7;
                int end = jsonContent.indexOf("```", start);
                if (end > start) {
                    jsonContent = jsonContent.substring(start, end).trim();
                }
            } else if (jsonContent.contains("```")) {
                int start = jsonContent.indexOf("```") + 3;
                int end = jsonContent.indexOf("```", start);
                if (end > start) {
                    jsonContent = jsonContent.substring(start, end).trim();
                }
            }
            
            // Try to find JSON object in the content (handle text before/after JSON)
            int jsonStart = jsonContent.indexOf("{");
            if (jsonStart == -1) {
                log.warn("⚠️ No JSON object found in response");
                return null;
            }
            
            // Find matching closing brace (handle nested objects)
            int braceCount = 0;
            int jsonEnd = -1;
            for (int i = jsonStart; i < jsonContent.length(); i++) {
                char c = jsonContent.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        jsonEnd = i;
                        break;
                    }
                }
            }
            
            if (jsonEnd > jsonStart) {
                jsonContent = jsonContent.substring(jsonStart, jsonEnd + 1);
            } else {
                log.warn("⚠️ Could not find matching closing brace for JSON object");
                return null;
            }
            
            log.info("🔍 Parsing execution plan from JSON: {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));
            ExecutionPlan plan = objectMapper.readValue(jsonContent, ExecutionPlan.class);
            log.info("✅ Successfully parsed execution plan with {} steps", 
                    plan.getSteps() != null ? plan.getSteps().size() : 0);
            return plan;
        } catch (Exception e) {
            log.error("❌ Failed to parse execution plan: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Fallback to legacy approach if plan parsing fails
     */
    private String handleLegacyResponse(String content, String enhancedMessage) {
        // Keep the old logic as fallback
        log.info("Using legacy tool call parsing approach");
        
        // Try to parse tool calls from the content
        List<ToolCallParser.ToolCall> parsedToolCalls = ToolCallParser.parseToolCallsFromText(content);
        
        if (!parsedToolCalls.isEmpty()) {
            StringBuilder toolResults = new StringBuilder();
            for (ToolCallParser.ToolCall toolCall : parsedToolCalls) {
                FunctionCallback callback = functionCallbacks.get(toolCall.getName());
                if (callback != null) {
                    try {
                        String argumentsJson = objectMapper.writeValueAsString(toolCall.getParameters());
                        Object result = callback.call(argumentsJson);
                        toolResults.append("Tool '").append(toolCall.getName())
                                .append("' executed. Result: ").append(result).append("\n");
                    } catch (Exception e) {
                        log.error("Error executing tool: {}", e.getMessage());
                    }
                }
            }
            return "Executed " + parsedToolCalls.size() + " tool(s). " + toolResults.toString();
        }
        
        return "I apologize, but I couldn't process your request. Please try rephrasing it.";
    }
}
