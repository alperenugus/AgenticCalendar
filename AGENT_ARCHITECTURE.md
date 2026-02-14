# ReAct Pattern Agent Architecture

## Overview

The agent uses the **ReAct (Reasoning and Acting) pattern**, an industry-standard approach for building AI agents. The LLM reasons about user requests, selects appropriate tools, observes results, and iteratively works through tasks until completion.

## Key Features

### 1. **ReAct Pattern Implementation**
- **Thought**: LLM describes what it needs to do
- **Action**: LLM selects a tool and provides arguments
- **Observation**: Java code executes the tool and returns the result
- **Thought**: LLM evaluates the result and decides next steps
- **Final Answer**: LLM provides the response when task is complete

### 2. **Flexible User Search**
- Search users with partial information (firstName, lastName, or dob - all optional)
- Handles multiple matches by prompting user for selection
- Works with any combination of parameters

### 3. **Iterative Execution Loop**
- LLM reasons about the user's request
- LLM uses tools as needed (LangChain4j executes automatically)
- LLM sees tool results and continues reasoning
- Continues until task complete or impossible (max 10 iterations)

### 4. **Conversation Memory**
- Full conversation history stored per session
- Last 5 messages used as context
- Session state maintained
- Automatic cleanup of stale sessions (24 hours)

### 5. **Best Practices**
- Thread-safe conversation storage (ConcurrentHashMap)
- Proper memory management with cleanup scheduler
- No manual intervention in LLM reasoning
- LLM handles error cases autonomously

## Architecture Components

### AgentService
- Main ReAct reasoning loop
- Manages conversation contexts
- Handles tool execution
- Parses ReAct pattern output

### CalendarToolService
- LangChain4j tool definitions with `@Tool` annotations
- Tools: createEvent, getEvents, getEventsByDateRange, checkConflicts, updateEvent, deleteEvent, getUpcomingEvents
- Supports recurring events with RRULE format
- Returns JSON strings for LLM parsing

### ReActParser
- Parses LLM output following ReAct format
- Extracts: Thought, Action, Action Input, Observation, Final Answer
- Handles multi-line and structured output

### ConversationContext
- Stores full conversation history
- Manages session state
- Formats history for LLM context

## How It Works

1. **User sends message** → Added to conversation history
2. **ReAct loop starts**:
   - LLM sees conversation history + system prompt
   - LLM provides Thought about what to do
   - LLM provides Action (tool name) and Action Input (JSON parameters)
   - System executes tool and provides Observation
   - LLM sees Observation and provides new Thought
   - Loop continues until Final Answer
3. **Final response** returned with all thinking visible

## Example Flow

**User**: "Update my appointment. My name is Alperen Ugus."

**Iteration 1**:
- **Thought**: "I need to find the user first. I will search by name."
- **Action**: getUser
- **Action Input**: {"firstName": "Alperen", "lastName": "Ugus"}
- **Observation**: {"userId": 1, "firstName": "Alperen", "lastName": "Ugus", "dob": "1990-01-01", "email": "alperen.ugus@example.com", "message": "User found"}

**Iteration 2**:
- **Thought**: "I found the user. Now I need to get their appointments."
- **Action**: getAppointmentsByUser
- **Action Input**: {"userId": 1}
- **Observation**: {"userId": 1, "appointments": [{"appointmentId": 5, "appointmentDateTime": "2024-12-25T10:00:00", "description": "Dental checkup"}], "message": "Found 1 appointment(s)"}

**Iteration 3**:
- **Thought**: "I found one appointment. I should ask the user which appointment they want to update or if they want to update this one."
- **Final Answer**: "I found one appointment for you: Dental checkup on December 25, 2024 at 10:00 AM. Would you like to update this appointment? Please provide the new date and time."

## Flexible User Search Example

**User**: "Book an appointment for John"

**Iteration 1**:
- **Thought**: "I only have the first name. I'll search for users with firstName='John'."
- **Action**: getUser
- **Action Input**: {"firstName": "John"}
- **Observation**: {"hasMultipleMatches": true, "users": [{"userId": 3, "firstName": "John", "lastName": "Doe", ...}, {"userId": 10, "firstName": "John", "lastName": "Smith", ...}], "message": "Found 2 matching users..."}

**Iteration 2**:
- **Thought**: "Multiple users found. I need to ask which one."
- **Final Answer**: "I found multiple users named John. Please provide your last name or date of birth so I can identify the correct account."

## Benefits

1. **Industry Standard**: Uses LangChain4j, the industry standard for Java LLM applications
2. **ReAct Pattern**: Proven pattern for building reliable AI agents
3. **Flexible**: Works with partial information
4. **Transparent**: Users see the agent's thinking process
5. **Iterative**: Works through complex tasks step by step
6. **Memory**: Maintains context across conversation
7. **Robust**: Handles errors and edge cases autonomously

## System Prompt

The system prompt enforces the ReAct protocol:

```
You are an AI Appointment Assistant. Your goal is to manage user records and schedules 
with strict adherence to the ReAct pattern.

### OPERATIONAL RULES:
1. NEVER guess a ID (userId or appointmentId). You MUST use the search tools to retrieve them.
2. If a tool returns multiple results, you MUST present the options to the user and ask for a selection before proceeding.
3. Before executing 'create', 'update', or 'delete' actions, summarize the details and ask for user confirmation.

### THE REACT PROTOCOL:
CRITICAL: You MUST follow this pattern exactly:
1. Thought: Explicitly state what information you have and what you need to fetch next.
2. Action: Call one (and only one) of the tools above using proper syntax.
3. Action Input: Provide the JSON parameters for the tool.
4. STOP HERE - DO NOT generate Observation or Final Answer after Action.
5. Wait for the system to provide the Observation (the tool result).
6. After receiving Observation, provide a new Thought, then either another Action or Final Answer.
```

## Tool Definitions

All tools are defined in `AppointmentToolService.java` using LangChain4j's `@Tool` annotation:

**User Management:**
- `getUser(firstName?, lastName?, dob?)` - Flexible user search (all parameters optional)
- `createUser(firstName, lastName, dob, email)` - Create new user in the system
- `updateUser(userId, firstName?, lastName?, dob?, email?)` - Update existing user (all fields optional)
- `deleteUser(userId)` - Permanently delete a user

**Appointment Management:**
- `getAppointmentsByUser(userId)` - Get all appointments for a user
- `createAppointment(userId, appointmentDateTime, description)` - Create new appointment
- `updateAppointment(appointmentId, newDateTime)` - Update existing appointment
- `deleteAppointment(appointmentId)` - Delete appointment

## Error Handling

The system handles errors gracefully:

- **Tool execution errors**: Returned as JSON with error message
- **Multiple user matches**: LLM prompts user for selection
- **No results found**: LLM asks for more information
- **Invalid parameters**: Tool returns error, LLM handles it in next Thought
- **Rate limit errors**: Graceful handling with user-friendly messages when API rate limits are exceeded
- **Token limit errors**: Clear messaging when daily token limits are reached

## Performance

- **Tool Execution**: 50-200ms (database queries)
- **LLM Reasoning**: 2-5 seconds (depends on model)
- **Total Request**: 3-6 seconds (for multi-step tasks)
- **Max Iterations**: 10 (prevents infinite loops)
