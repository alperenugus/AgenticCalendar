package com.agent.appointmentscheduler.service;

import com.agent.appointmentscheduler.model.AgentResponse;
import com.agent.appointmentscheduler.model.ConversationContext;
import com.agent.appointmentscheduler.model.ConversationMessage;
import com.agent.appointmentscheduler.tools.AppointmentToolService;
import com.agent.appointmentscheduler.util.ReActParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final int MAX_ITERATIONS = 10;
    
    private final ChatLanguageModel chatLanguageModel;
    private final AppointmentToolService toolService;
    private final InputValidationService inputValidationService;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;
    
    private final Map<String, ConversationContext> conversationContexts = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionLastAccess = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = TimeUnit.HOURS.toMillis(24);

    private final ScheduledExecutorService sessionCleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "session-cleanup");
        t.setDaemon(true);
        return t;
    });

    public AgentService(
            ChatLanguageModel chatLanguageModel,
            AppointmentToolService toolService,
            InputValidationService inputValidationService,
            ObjectMapper objectMapper,
            WebSocketService webSocketService
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.toolService = toolService;
        this.inputValidationService = inputValidationService;
        this.objectMapper = objectMapper;
        this.webSocketService = webSocketService;
        
        startSessionCleanupScheduler();
    }

    public AgentResponse processUserMessage(String userMessage, String sessionId) {
        String sanitizedMessage = inputValidationService.validateAndSanitize(userMessage);
        ConversationContext context = conversationContexts.computeIfAbsent(sessionId, ConversationContext::new);
        sessionLastAccess.put(sessionId, System.currentTimeMillis());
        
        context.addUserMessage(sanitizedMessage);
        webSocketService.sendThinking(sessionId, "Analyzing your request...");
        
        try {
            return executeReasoningLoop(context, sanitizedMessage, sessionId);
        } catch (Exception e) {
            log.error("Error processing message", e);
            return new AgentResponse("An error occurred. Please try again.");
        }
    }

    private AgentResponse executeReasoningLoop(ConversationContext context, String userMessage, String sessionId) {
        List<AgentResponse.ThinkingStep> thinkingSteps = new ArrayList<>();
        int iteration = 0;
        
        // Build conversation history for context
        StringBuilder conversationHistory = new StringBuilder();
        List<ConversationMessage> recentMessages = context.getMessages();
        int startIdx = Math.max(0, recentMessages.size() - 5); // Last 5 messages
        for (int i = startIdx; i < recentMessages.size(); i++) {
            ConversationMessage msg = recentMessages.get(i);
            conversationHistory.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        
        // Build the full prompt with system instructions and conversation history
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\nConversation History:\n" + conversationHistory.toString() + 
                          "\nCurrent User Message: " + userMessage + "\n\n" +
                          "Please respond following the ReAct protocol. Start with Thought:";
        
        StringBuilder scratchpad = new StringBuilder();
        
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            
            // Get LLM response
            String prompt = fullPrompt + scratchpad.toString();
            String assistantResponse;
            try {
                assistantResponse = chatLanguageModel.generate(prompt);
            } catch (Exception e) {
                // Handle rate limit and other API errors
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("rate_limit")) {
                    log.error("Rate limit exceeded for LLM provider", e);
                    String userMessage = "I've reached the API rate limit. Please try again in a few minutes. " +
                            "If this persists, the system administrator may need to upgrade the API tier or switch to a different model.";
                    context.addAssistantMessage(userMessage);
                    webSocketService.sendFinalResponse(sessionId, userMessage);
                    return new AgentResponse(userMessage, thinkingSteps);
                } else if (errorMessage != null && errorMessage.contains("tokens per day")) {
                    log.error("Daily token limit exceeded", e);
                    String userMessage = "The daily token limit has been reached. Please try again tomorrow or contact the administrator.";
                    context.addAssistantMessage(userMessage);
                    webSocketService.sendFinalResponse(sessionId, userMessage);
                    return new AgentResponse(userMessage, thinkingSteps);
                } else {
                    // Re-throw other exceptions
                    throw e;
                }
            }
            
            log.debug("LLM Response (iteration {}): {}", iteration, assistantResponse);
            
            // Parse ReAct output
            ReActParser.ReActOutput react = ReActParser.parse(assistantResponse);
            
            // Check for action first - if action is present, ignore final answer (LLM should wait for observation)
            if (react.hasAction()) {
                String toolName = react.getAction();
                String actionInput = react.getActionInput();
                
                // If LLM generated Observation or Final Answer with Action, ignore them
                // The LLM should only provide Action and Action Input, then wait
                if (react.hasFinalAnswer()) {
                    log.warn("LLM generated Final Answer with Action - ignoring Final Answer. LLM should wait for Observation first.");
                }
                
                // Log thinking step
                AgentResponse.ThinkingStep step = new AgentResponse.ThinkingStep();
                step.setThinking(react.getThought());
                step.setToolName(toolName);
                thinkingSteps.add(step);
                
                // Send thinking update
                webSocketService.sendThinking(sessionId, react.getThought());
                
                // Execute tool
                String observation = executeTool(toolName, actionInput);
                step.setToolResult(observation);
                
                // Add to scratchpad for next iteration - explicitly tell LLM to continue
                scratchpad.append("\n\nThought: ").append(react.getThought());
                scratchpad.append("\nAction: ").append(toolName);
                scratchpad.append("\nAction Input: ").append(actionInput);
                scratchpad.append("\nObservation: ").append(observation);
                scratchpad.append("\n\nNow provide your next Thought based on the Observation above, then either another Action or Final Answer.");
                
                log.debug("Tool executed: {} -> {}", toolName, observation);
                // Continue to next iteration - LLM will see the Observation and provide next Thought
            } else if (react.hasFinalAnswer()) {
                // Final answer without action - task is complete
                context.addAssistantMessage(react.getFinalAnswer());
                webSocketService.sendFinalResponse(sessionId, react.getFinalAnswer());
                return new AgentResponse(react.getFinalAnswer(), thinkingSteps);
            } else {
                // No action or final answer - treat as final response
                String finalResponse = assistantResponse.trim();
                context.addAssistantMessage(finalResponse);
                webSocketService.sendFinalResponse(sessionId, finalResponse);
                return new AgentResponse(finalResponse, thinkingSteps);
            }
        }
        
        return new AgentResponse("I've reached my reasoning limit. Could you please be more specific?", thinkingSteps);
    }

    private String executeTool(String toolName, String actionInput) {
        try {
            // Parse action input JSON
            JsonNode inputNode = objectMapper.readTree(actionInput);
            
            // Route to appropriate tool method
            switch (toolName) {
                case "getUser":
                    String firstName = inputNode.has("firstName") ? inputNode.get("firstName").asText() : null;
                    String lastName = inputNode.has("lastName") ? inputNode.get("lastName").asText() : null;
                    String dob = inputNode.has("dob") ? inputNode.get("dob").asText() : null;
                    return toolService.getUser(firstName, lastName, dob);
                    
                case "getAppointmentsByUser":
                    Long userId = inputNode.get("userId").asLong();
                    return toolService.getAppointmentsByUser(userId);
                    
                case "createAppointment":
                    Long createUserId = inputNode.get("userId").asLong();
                    String dateTime = inputNode.get("appointmentDateTime").asText();
                    String description = inputNode.get("description").asText();
                    return toolService.createAppointment(createUserId, dateTime, description);
                    
                case "updateAppointment":
                    Long appointmentId = inputNode.get("appointmentId").asLong();
                    String newDateTime = inputNode.get("newDateTime").asText();
                    return toolService.updateAppointment(appointmentId, newDateTime);
                    
                case "deleteAppointment":
                    Long deleteAppointmentId = inputNode.get("appointmentId").asLong();
                    return toolService.deleteAppointment(deleteAppointmentId);
                    
                case "createUser":
                    String createFirstName = inputNode.get("firstName").asText();
                    String createLastName = inputNode.get("lastName").asText();
                    String createDob = inputNode.get("dob").asText();
                    String createEmail = inputNode.get("email").asText();
                    return toolService.createUser(createFirstName, createLastName, createDob, createEmail);
                    
                case "updateUser":
                    Long updateUserId = inputNode.get("userId").asLong();
                    String updateFirstName = inputNode.has("firstName") ? inputNode.get("firstName").asText() : null;
                    String updateLastName = inputNode.has("lastName") ? inputNode.get("lastName").asText() : null;
                    String updateDob = inputNode.has("dob") ? inputNode.get("dob").asText() : null;
                    String updateEmail = inputNode.has("email") ? inputNode.get("email").asText() : null;
                    return toolService.updateUser(updateUserId, updateFirstName, updateLastName, updateDob, updateEmail);
                    
                case "deleteUser":
                    Long deleteUserId = inputNode.get("userId").asLong();
                    return toolService.deleteUser(deleteUserId);
                    
                default:
                    return "{\"error\": \"Unknown tool: " + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("Error executing tool {} with input {}", toolName, actionInput, e);
            return "{\"error\": \"Error executing tool: " + e.getMessage() + "\"}";
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an AI Appointment Assistant. Your goal is to manage user records and schedules with strict adherence to the ReAct pattern.

                ### OPERATIONAL RULES:
                1. NEVER guess a ID (userId or appointmentId). You MUST use the search tools to retrieve them.
                2. If a tool returns multiple results, you MUST present the options to the user and ask for a selection before proceeding.
                3. Before executing 'create', 'update', or 'delete' actions, summarize the details and ask for user confirmation.

                ### AVAILABLE TOOLS:
                - getUser(firstName, lastName, dob): Returns matching users. All parameters are optional.
                - getAppointmentsByUser(userId): Lists all appointments for a specific ID.
                - createAppointment(userId, appointmentDateTime, description): Books a new slot.
                - updateAppointment(appointmentId, newDateTime): Modifies an existing slot.
                - deleteAppointment(appointmentId): Cancels a specific slot.
                - createUser(firstName, lastName, dob, email): Creates a new user in the system. Collect all required information before calling.
                - updateUser(userId, firstName, lastName, dob, email): Updates an existing user's information. Only provide fields that need updating.
                - deleteUser(userId): Permanently deletes a user from the system. Use with caution.

                ### THE REACT PROTOCOL:
                CRITICAL: You MUST follow this pattern exactly:
                1. Thought: Explicitly state what information you have and what you need to fetch next.
                2. Action: Call one (and only one) of the tools above using proper syntax.
                3. Action Input: Provide the JSON parameters for the tool.
                4. STOP HERE - DO NOT generate Observation or Final Answer after Action.
                5. Wait for the system to provide the Observation (the tool result).
                6. After receiving Observation, provide a new Thought, then either another Action or Final Answer.
                
                IMPORTANT RULES:
                - NEVER generate an Observation yourself - the system will provide it after executing the tool
                - NEVER include both Action and Final Answer in the same response
                - After providing Action and Action Input, STOP and wait for Observation
                - Only provide Final Answer when the task is complete and you have all needed information

                ### EXAMPLE INTERACTION:
                User: "Check my upcoming appointments. My name is [First Name] [Last Name]."
                Thought: I need the userId for this person to fetch their appointments. I will search by name first.
                Action: getUser
                Action Input: {"firstName": "[First Name]", "lastName": "[Last Name]"}
                Observation: [{"userId": "[ID_001]", "firstName": "[First Name]", "lastName": "[Last Name]", "dob": "[DOB_DATA]"}]
                Thought: I have retrieved the userId. Now I can look up the specific appointments.
                Action: getAppointmentsByUser
                Action Input: {"userId": "[ID_001]"}
                Observation: [{"appointmentId": "[APP_99]", "dateTime": "[ISO_DATE_TIME]", "description": "[TEXT]"}]
                Final Answer: I found one appointment for [TEXT] scheduled for [ISO_DATE_TIME].

                ### IMPORTANT FORMATTING:
                - Always use the exact format: "Thought:", "Action:", "Action Input:", "Observation:", "Final Answer:"
                - Action Input must be valid JSON
                - Only call ONE tool per iteration
                - CRITICAL: After providing Action and Action Input, STOP. Do NOT generate Observation or Final Answer.
                - The system will execute the tool and provide the Observation in the next turn
                - Only after receiving the Observation should you provide a new Thought and continue
                """;
    }

    private void startSessionCleanupScheduler() {
        sessionCleanupScheduler.scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();
            sessionLastAccess.entrySet().removeIf(entry -> (now - entry.getValue()) > SESSION_TIMEOUT_MS);
        }, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Retrieves the conversation history for a given session ID
     */
    public List<ConversationMessage> getConversationHistory(String sessionId) {
        ConversationContext context = conversationContexts.get(sessionId);
        if (context == null) {
            return new ArrayList<>();
        }
        return context.getMessages();
    }
}
