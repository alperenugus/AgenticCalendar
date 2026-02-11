# AgentService Algorithm Flow Diagram

This document provides a detailed flow diagram and pseudocode for the `AgentService` class, which implements the ReAct (Reasoning and Acting) pattern for AI agent interactions.

## Overview

The `AgentService` processes user messages through an iterative reasoning loop where the LLM:
1. **Thinks** about what needs to be done
2. **Acts** by selecting a tool and providing parameters
3. **Observes** the tool execution results
4. **Repeats** until the task is complete or maximum iterations reached

## Flow Diagram

```mermaid
flowchart TD
    Start([User sends message]) --> Validate[Validate & Sanitize Input]
    Validate --> AddToHistory[Add user message to conversation history]
    AddToHistory --> InitLoop[Initialize ReAct Loop<br/>iteration = 0<br/>thinkingSteps = []<br/>scratchpad = ""]
    
    InitLoop --> BuildPrompt[Build Full Prompt<br/>System Prompt +<br/>Conversation History +<br/>Current Message +<br/>Scratchpad]
    
    BuildPrompt --> CheckIterations{iteration < MAX_ITERATIONS?}
    CheckIterations -->|No| MaxIterations[Return: Max iterations reached]
    CheckIterations -->|Yes| IncrementIter[iteration++]
    
    IncrementIter --> CallLLM[Call LLM: chatLanguageModel.generate]
    
    CallLLM --> RateLimitCheck{Rate Limit Error?}
    RateLimitCheck -->|Yes| HandleRateLimit[Return: Rate limit error message]
    RateLimitCheck -->|No| ParseResponse[Parse LLM Response<br/>ReActParser.parse]
    
    ParseResponse --> CheckAction{Has Action?}
    
    CheckAction -->|Yes| ExtractTool[Extract tool name & parameters]
    ExtractTool --> ExecuteTool[Execute Tool<br/>executeTool]
    ExecuteTool --> AddToScratchpad[Add to Scratchpad:<br/>Thought + Action +<br/>Action Input + Observation]
    AddToScratchpad --> SendThinking[Send thinking update via WebSocket]
    SendThinking --> CheckIterations
    
    CheckAction -->|No| CheckFinalAnswer{Has Final Answer?}
    CheckFinalAnswer -->|Yes| AddFinalAnswer[Add final answer to history]
    AddFinalAnswer --> SendFinal[Send final response via WebSocket]
    SendFinal --> ReturnSuccess[Return AgentResponse with<br/>final answer & thinking steps]
    
    CheckFinalAnswer -->|No| TreatAsFinal[Treat entire response as final]
    TreatAsFinal --> AddFinalAnswer
    
    MaxIterations --> ReturnError[Return: Max iterations error]
    HandleRateLimit --> End([End])
    ReturnSuccess --> End
    ReturnError --> End
    
    style Start fill:#e1f5ff
    style End fill:#ffe1f5
    style CallLLM fill:#fff4e1
    style ExecuteTool fill:#e1ffe1
    style RateLimitCheck fill:#ffe1e1
    style CheckAction fill:#e1e1ff
    style CheckFinalAnswer fill:#e1e1ff
```

## Detailed Algorithm Pseudocode

```pseudocode
FUNCTION processUserMessage(userMessage: String, sessionId: String) RETURNS AgentResponse
    // Step 1: Input Validation
    sanitizedMessage = inputValidationService.validateAndSanitize(userMessage)
    
    // Step 2: Get or Create Conversation Context
    context = conversationContexts.getOrCreate(sessionId)
    sessionLastAccess.update(sessionId, currentTime)
    
    // Step 3: Add User Message to History
    context.addUserMessage(sanitizedMessage)
    webSocketService.sendThinking(sessionId, "Analyzing your request...")
    
    // Step 4: Execute ReAct Reasoning Loop
    TRY
        RETURN executeReasoningLoop(context, sanitizedMessage, sessionId)
    CATCH Exception e
        IF errorMessage contains "rate_limit" OR "tokens per day"
            RETURN AgentResponse("Rate limit error message")
        ELSE
            RETURN AgentResponse("Generic error message")
        END IF
    END TRY
END FUNCTION

FUNCTION executeReasoningLoop(
    context: ConversationContext,
    userMessage: String,
    sessionId: String
) RETURNS AgentResponse
    
    // Initialize loop variables
    thinkingSteps = []
    iteration = 0
    scratchpad = ""
    
    // Build conversation history (last 5 messages)
    conversationHistory = ""
    recentMessages = context.getMessages().getLast(5)
    FOR EACH message IN recentMessages
        conversationHistory += message.role + ": " + message.content + "\n"
    END FOR
    
    // Build system prompt
    systemPrompt = buildSystemPrompt()
    
    // Build full prompt with context
    fullPrompt = systemPrompt + 
                 "\n\nConversation History:\n" + conversationHistory +
                 "\nCurrent User Message: " + userMessage +
                 "\n\nPlease respond following the ReAct protocol. Start with Thought:"
    
    // Main ReAct Loop
    WHILE iteration < MAX_ITERATIONS DO
        iteration++
        
        // Build prompt with scratchpad (previous iterations)
        prompt = fullPrompt + scratchpad
        
        // Call LLM
        TRY
            assistantResponse = chatLanguageModel.generate(prompt)
        CATCH Exception e
            errorMessage = e.getMessage()
            
            IF errorMessage contains "rate_limit" THEN
                errorResponse = "I've reached the API rate limit. Please try again in a few minutes."
                context.addAssistantMessage(errorResponse)
                webSocketService.sendFinalResponse(sessionId, errorResponse)
                RETURN AgentResponse(errorResponse, thinkingSteps)
            ELSE IF errorMessage contains "tokens per day" THEN
                errorResponse = "The daily token limit has been reached. Please try again tomorrow."
                context.addAssistantMessage(errorResponse)
                webSocketService.sendFinalResponse(sessionId, errorResponse)
                RETURN AgentResponse(errorResponse, thinkingSteps)
            ELSE
                THROW e  // Re-throw other exceptions
            END IF
        END TRY
        
        // Parse ReAct output
        react = ReActParser.parse(assistantResponse)
        
        // Check if LLM provided an Action
        IF react.hasAction() THEN
            toolName = react.getAction()
            actionInput = react.getActionInput()
            
            // Log warning if LLM generated Final Answer with Action
            IF react.hasFinalAnswer() THEN
                LOG WARNING: "LLM generated Final Answer with Action - ignoring Final Answer"
            END IF
            
            // Create thinking step
            step = new ThinkingStep()
            step.setThinking(react.getThought())
            step.setToolName(toolName)
            thinkingSteps.add(step)
            
            // Send thinking update via WebSocket
            webSocketService.sendThinking(sessionId, react.getThought())
            
            // Execute the tool
            observation = executeTool(toolName, actionInput)
            step.setToolResult(observation)
            
            // Add to scratchpad for next iteration
            scratchpad += "\n\nThought: " + react.getThought()
            scratchpad += "\nAction: " + toolName
            scratchpad += "\nAction Input: " + actionInput
            scratchpad += "\nObservation: " + observation
            scratchpad += "\n\nNow provide your next Thought based on the Observation above, "
            scratchpad += "then either another Action or Final Answer."
            
            // Continue to next iteration
            CONTINUE
            
        // Check if LLM provided Final Answer
        ELSE IF react.hasFinalAnswer() THEN
            finalAnswer = react.getFinalAnswer()
            context.addAssistantMessage(finalAnswer)
            webSocketService.sendFinalResponse(sessionId, finalAnswer)
            RETURN AgentResponse(finalAnswer, thinkingSteps)
            
        // No Action or Final Answer - treat entire response as final
        ELSE
            finalResponse = assistantResponse.trim()
            context.addAssistantMessage(finalResponse)
            webSocketService.sendFinalResponse(sessionId, finalResponse)
            RETURN AgentResponse(finalResponse, thinkingSteps)
        END IF
    END WHILE
    
    // Max iterations reached
    RETURN AgentResponse(
        "I've reached my reasoning limit. Could you please be more specific?",
        thinkingSteps
    )
END FUNCTION

FUNCTION executeTool(toolName: String, actionInput: String) RETURNS String
    TRY
        // Parse JSON action input
        inputNode = objectMapper.readTree(actionInput)
        
        // Route to appropriate tool based on toolName
        SWITCH toolName
            CASE "getUser":
                firstName = inputNode.get("firstName") IF EXISTS ELSE null
                lastName = inputNode.get("lastName") IF EXISTS ELSE null
                dob = inputNode.get("dob") IF EXISTS ELSE null
                RETURN toolService.getUser(firstName, lastName, dob)
                
            CASE "getAppointmentsByUser":
                userId = inputNode.get("userId").asLong()
                RETURN toolService.getAppointmentsByUser(userId)
                
            CASE "createAppointment":
                userId = inputNode.get("userId").asLong()
                dateTime = inputNode.get("appointmentDateTime").asText()
                description = inputNode.get("description").asText()
                RETURN toolService.createAppointment(userId, dateTime, description)
                
            CASE "updateAppointment":
                appointmentId = inputNode.get("appointmentId").asLong()
                newDateTime = inputNode.get("newDateTime").asText()
                RETURN toolService.updateAppointment(appointmentId, newDateTime)
                
            CASE "deleteAppointment":
                appointmentId = inputNode.get("appointmentId").asLong()
                RETURN toolService.deleteAppointment(appointmentId)
                
            CASE "createUser":
                firstName = inputNode.get("firstName").asText()
                lastName = inputNode.get("lastName").asText()
                dob = inputNode.get("dob").asText()
                email = inputNode.get("email").asText()
                RETURN toolService.createUser(firstName, lastName, dob, email)
                
            CASE "updateUser":
                userId = inputNode.get("userId").asLong()
                firstName = inputNode.get("firstName") IF EXISTS ELSE null
                lastName = inputNode.get("lastName") IF EXISTS ELSE null
                dob = inputNode.get("dob") IF EXISTS ELSE null
                email = inputNode.get("email") IF EXISTS ELSE null
                RETURN toolService.updateUser(userId, firstName, lastName, dob, email)
                
            CASE "deleteUser":
                userId = inputNode.get("userId").asLong()
                RETURN toolService.deleteUser(userId)
                
            DEFAULT:
                RETURN "{\"error\": \"Unknown tool: " + toolName + "\"}"
        END SWITCH
        
    CATCH Exception e
        LOG ERROR: "Error executing tool " + toolName + " with input " + actionInput
        RETURN "{\"error\": \"Error executing tool: " + e.getMessage() + "\"}"
    END TRY
END FUNCTION

FUNCTION buildSystemPrompt() RETURNS String
    RETURN """
        You are an AI Appointment Assistant. Your goal is to manage user records 
        and schedules with strict adherence to the ReAct pattern.
        
        ### OPERATIONAL RULES:
        1. NEVER guess a ID (userId or appointmentId). You MUST use the search tools to retrieve them.
        2. If a tool returns multiple results, you MUST present the options to the user 
           and ask for a selection before proceeding.
        3. Before executing 'create', 'update', or 'delete' actions, summarize the details 
           and ask for user confirmation.
        
        ### AVAILABLE TOOLS:
        - getUser(firstName?, lastName?, dob?): Returns matching users. All parameters optional.
        - getAppointmentsByUser(userId): Lists all appointments for a specific ID.
        - createAppointment(userId, appointmentDateTime, description): Books a new slot.
        - updateAppointment(appointmentId, newDateTime): Modifies an existing slot.
        - deleteAppointment(appointmentId): Cancels a specific slot.
        - createUser(firstName, lastName, dob, email): Creates a new user in the system.
        - updateUser(userId, firstName?, lastName?, dob?, email?): Updates user info.
        - deleteUser(userId): Permanently deletes a user from the system.
        
        ### THE REACT PROTOCOL:
        CRITICAL: You MUST follow this pattern exactly:
        1. Thought: Explicitly state what information you have and what you need to fetch next.
        2. Action: Call one (and only one) of the tools above using proper syntax.
        3. Action Input: Provide the JSON parameters for the tool.
        4. STOP HERE - DO NOT generate Observation or Final Answer after Action.
        5. Wait for the system to provide the Observation (the tool result).
        6. After receiving Observation, provide a new Thought, then either another Action or Final Answer.
        
        ### IMPORTANT FORMATTING:
        - Always use the exact format: "Thought:", "Action:", "Action Input:", "Observation:", "Final Answer:"
        - Action Input must be valid JSON
        - Only call ONE tool per iteration
        - CRITICAL: After providing Action and Action Input, STOP. Do NOT generate Observation or Final Answer.
        """
END FUNCTION
```

## Key Components

### 1. Input Processing
- **Validation**: All user input is sanitized and validated
- **Context Management**: Conversation history is maintained per session
- **Session Tracking**: Last access time tracked for cleanup

### 2. ReAct Loop
- **Iteration Limit**: Maximum 10 iterations to prevent infinite loops
- **Prompt Building**: System prompt + conversation history + current message + scratchpad
- **LLM Interaction**: Single call per iteration to get Thought/Action/Final Answer

### 3. Tool Execution
- **JSON Parsing**: Action input parsed as JSON
- **Tool Routing**: Switch statement routes to appropriate tool method
- **Error Handling**: Tool errors returned as JSON for LLM to handle

### 4. State Management
- **Scratchpad**: Accumulates Thought-Action-Observation cycles
- **Thinking Steps**: Tracked for transparency and debugging
- **WebSocket Updates**: Real-time updates sent to frontend

### 5. Error Handling
- **Rate Limits**: Specific handling for API rate limit errors
- **Token Limits**: Handling for daily token limit errors
- **Tool Errors**: Returned as JSON for LLM to reason about

## Example Execution Flow

### Scenario: "Update my appointment. My name is Alperen Ugus."

```
Iteration 1:
├─ Thought: "I need to find the user first. I will search by name."
├─ Action: getUser
├─ Action Input: {"firstName": "Alperen", "lastName": "Ugus"}
├─ Observation: {"userId": 1, "firstName": "Alperen", ...}
└─ Scratchpad: [Thought + Action + Action Input + Observation]

Iteration 2:
├─ Thought: "I found the user. Now I need to get their appointments."
├─ Action: getAppointmentsByUser
├─ Action Input: {"userId": 1}
├─ Observation: {"appointments": [{"appointmentId": 5, ...}]}
└─ Scratchpad: [Previous + New Thought + Action + Action Input + Observation]

Iteration 3:
├─ Thought: "I found one appointment. I should ask the user for the new date/time."
└─ Final Answer: "I found your appointment. Please provide the new date and time."
```

## Constants and Configuration

- **MAX_ITERATIONS**: 10 (prevents infinite loops)
- **SESSION_TIMEOUT_MS**: 24 hours (automatic cleanup)
- **CONVERSATION_HISTORY_SIZE**: Last 5 messages used as context

## Performance Considerations

1. **Token Usage**: Each iteration adds to the prompt, increasing token consumption
2. **LLM Latency**: Each iteration requires a full LLM call (2-5 seconds)
3. **Database Queries**: Tool execution involves database operations (50-200ms)
4. **WebSocket Updates**: Real-time updates add minimal overhead

## Error Recovery

- **Rate Limits**: User-friendly message, graceful degradation
- **Tool Failures**: Error returned as JSON, LLM can reason about it
- **Max Iterations**: Clear message asking user to be more specific
- **Invalid Input**: Validation catches issues before processing


