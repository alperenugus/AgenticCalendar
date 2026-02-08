# Agent System Learnings & Best Practices

## Document Purpose

This document captures all learnings, best practices, challenges, and solutions from building a single-agent appointment scheduling system. This will serve as a reference for future multi-agent system development.

---

## Table of Contents

1. [Architecture Decisions](#architecture-decisions)
2. [Technology Stack](#technology-stack)
3. [Pattern Implementation](#pattern-implementation)
4. [Best Practices](#best-practices)
5. [Challenges & Solutions](#challenges--solutions)
6. [What Worked Well](#what-worked-well)
7. [What Caused Issues](#what-caused-issues)
8. [Key Learnings](#key-learnings)
9. [Future Multi-Agent Considerations](#future-multi-agent-considerations)

---

## Architecture Decisions

### Why LangChain4j Over Spring AI?

**Decision**: Migrated from Spring AI to LangChain4j

**Rationale**:
- **Industry Standard**: LangChain4j is the de-facto standard for Java LLM applications
- **Better Tool Support**: More mature tool calling and function definition
- **Active Development**: More active community and faster updates
- **Better Documentation**: Comprehensive documentation and examples
- **Flexibility**: Easier to integrate with different LLM providers

**Lesson**: Always choose industry-standard frameworks over newer alternatives when building production systems.

### Why ReAct Pattern Over Planning-Based?

**Decision**: Implemented ReAct (Reasoning and Acting) pattern instead of plan-based execution

**Rationale**:
- **Simplicity**: ReAct is simpler and more direct
- **LLM-Friendly**: LLMs naturally follow the Thought -> Action -> Observation flow
- **Error Recovery**: Easier to handle errors and retry within the loop
- **Transparency**: Users can see the agent's reasoning process
- **Flexibility**: Can adapt to unexpected situations without rigid plans

**Lesson**: ReAct pattern is more suitable for conversational agents where flexibility and transparency matter.

### Why Manual ReAct Parsing Over Native Tool Calling?

**Decision**: Implemented manual ReAct pattern parsing instead of relying on LLM's native tool calling

**Rationale**:
- **Control**: Full control over the execution flow
- **Debugging**: Easier to debug and log each step
- **Flexibility**: Can handle edge cases and multiple matches
- **Transparency**: Can show thinking steps to users via WebSocket
- **Compatibility**: Works with models that don't support native tool calling well

**Lesson**: Manual parsing gives more control but requires careful prompt engineering.

---

## Technology Stack

### Core Technologies

1. **LangChain4j 0.34.0**
   - Industry standard for Java LLM applications
   - Excellent Ollama integration
   - Clean API for tool definitions

2. **Spring Boot 3.4**
   - Robust framework for enterprise applications
   - Excellent dependency injection
   - Built-in WebSocket support

3. **Ollama (llama3.1)**
   - Free, local LLM execution
   - No API costs
   - Good structured output support

4. **PostgreSQL**
   - Reliable relational database
   - Good for structured data
   - Excellent Spring Data JPA integration

### Why These Choices?

- **LangChain4j**: Industry standard, active development
- **Ollama**: Free, local, no vendor lock-in
- **PostgreSQL**: Reliable, well-supported
- **Spring Boot**: Enterprise-grade, excellent ecosystem

---

## Pattern Implementation

### ReAct Pattern Structure

```
User Request
    ↓
Thought: "I need to find the user"
    ↓
Action: getUser
    ↓
Action Input: {"firstName": "John"}
    ↓
Observation: {"userId": 123, ...}
    ↓
Thought: "Found user, now get appointments"
    ↓
Action: getAppointmentsByUser
    ↓
Observation: [{"appointmentId": 456, ...}]
    ↓
Final Answer: "I found your appointment..."
```

### Implementation Details

**Key Components**:
1. **ReActParser**: Parses LLM output into structured format
2. **AgentService**: Manages the reasoning loop
3. **AppointmentToolService**: Defines tools with @Tool annotations
4. **ConversationContext**: Maintains conversation history

**Critical Implementation Points**:
- System prompt must be very explicit about NOT generating Observations
- LLM must STOP after Action and wait for Observation
- Each iteration adds to scratchpad for context
- Max iterations prevent infinite loops

---

## Best Practices

### 1. System Prompt Engineering

**✅ DO**:
- Be extremely explicit about the format
- Use examples showing the exact pattern
- Emphasize what NOT to do (e.g., "DO NOT generate Observation")
- Include operational rules (e.g., "NEVER guess IDs")
- Use clear section headers

**Example**:
```
### THE REACT PROTOCOL:
CRITICAL: You MUST follow this pattern exactly:
1. Thought: ...
2. Action: ...
3. Action Input: ...
4. STOP HERE - DO NOT generate Observation
5. Wait for system to provide Observation
```

**❌ DON'T**:
- Assume LLM will follow format without explicit instructions
- Use vague language
- Skip examples
- Forget to emphasize stopping points

### 2. Tool Design

**✅ DO**:
- Make parameters optional when possible (flexible search)
- Return structured JSON for easy parsing
- Handle multiple matches gracefully
- Provide clear error messages
- Use descriptive tool names

**Example**:
```java
@Tool("Look up users by first name, last name, and/or date of birth. All parameters optional.")
public String getUser(String firstName, String lastName, String dob) {
    // Handle null/empty parameters
    // Return JSON with clear structure
    // Handle multiple matches
}
```

**❌ DON'T**:
- Require all parameters when partial info is sufficient
- Return unstructured text
- Fail silently on errors
- Use ambiguous tool names

### 3. Error Handling

**✅ DO**:
- Return errors as structured JSON
- Let LLM handle errors in next Thought
- Provide helpful error messages
- Log errors for debugging

**Example**:
```java
try {
    // Tool execution
} catch (Exception e) {
    return String.format("{\"error\": \"Error: %s\"}", e.getMessage());
}
```

**❌ DON'T**:
- Throw exceptions that crash the loop
- Return unclear error messages
- Hide errors from LLM

### 4. Session Management

**✅ DO**:
- Use ConcurrentHashMap for thread safety
- Implement session cleanup (24-hour timeout)
- Store conversation history per session
- Use defensive copying for history

**Example**:
```java
private final Map<String, ConversationContext> conversationContexts = 
    new ConcurrentHashMap<>();
    
// Cleanup stale sessions
sessionCleanupScheduler.scheduleWithFixedDelay(() -> {
    // Remove sessions older than 24 hours
}, 1, 1, TimeUnit.HOURS);
```

**❌ DON'T**:
- Use regular HashMap (not thread-safe)
- Keep sessions forever (memory leak)
- Share state between sessions

### 5. Flexible Search Implementation

**✅ DO**:
- Support partial information
- Return multiple matches when found
- Prompt user for selection when needed
- Use repository methods for different combinations

**Example**:
```java
// Support all combinations
List<User> searchUsersFlexible(String firstName, String lastName, LocalDate dob) {
    if (hasFirstName && hasLastName && hasDob) {
        return findByFirstNameAndLastNameAndDob(...);
    } else if (hasFirstName && hasLastName) {
        return findByFirstNameAndLastName(...);
    }
    // ... handle all combinations
}
```

**❌ DON'T**:
- Require all parameters
- Fail when partial info provided
- Return first match when multiple exist

---

## Challenges & Solutions

### Challenge 1: LLM Generating Fake Observations

**Problem**: LLM was generating its own Observations instead of waiting for tool results.

**Symptoms**:
- LLM would output: `Action: getUser` followed by `Observation: [fake data]`
- Tool results were ignored
- Incorrect information propagated

**Root Cause**: 
- System prompt wasn't explicit enough
- LLM tried to complete the pattern on its own
- No clear instruction to STOP after Action

**Solution**:
1. Made system prompt extremely explicit:
   ```
   CRITICAL: After providing Action and Action Input, STOP HERE.
   DO NOT generate Observation or Final Answer.
   Wait for the system to provide the Observation.
   ```

2. Added explicit instruction in scratchpad after tool execution:
   ```java
   scratchpad.append("\n\nNow provide your next Thought based on the Observation above...");
   ```

3. Changed logic to check Action first, ignore Final Answer if Action present

**Lesson**: LLMs need very explicit stopping points. Don't assume they'll wait.

### Challenge 2: Flexible User Search

**Problem**: getUser required all three parameters (firstName, lastName, dob), but users often provide partial information.

**Symptoms**:
- "Book appointment for John" → fails (missing lastName, dob)
- "Find user born on 1990-01-01" → fails (missing firstName, lastName)
- Poor user experience

**Root Cause**:
- Original design required exact match
- No support for partial searches
- Repository only had exact match method

**Solution**:
1. Added repository methods for all combinations:
   - `findByFirstName`
   - `findByLastName`
   - `findByDob`
   - `findByFirstNameAndLastName`
   - `findByFirstNameAndDob`
   - `findByLastNameAndDob`

2. Created flexible search method:
   ```java
   List<User> searchUsersFlexible(String firstName, String lastName, LocalDate dob)
   ```

3. Updated tool to handle multiple matches:
   - Return list when multiple found
   - Prompt user for selection
   - Return single user when one match

**Lesson**: Design tools to be flexible and handle partial information. Users rarely provide complete data.

### Challenge 3: Session Management

**Problem**: Sessions were never cleaned up, causing memory leaks.

**Symptoms**:
- Memory usage growing over time
- Old conversations consuming resources
- No way to expire sessions

**Root Cause**:
- No cleanup mechanism
- Sessions stored indefinitely
- No timeout logic

**Solution**:
1. Added session last access tracking:
   ```java
   private final Map<String, Long> sessionLastAccess = new ConcurrentHashMap<>();
   ```

2. Implemented cleanup scheduler:
   ```java
   sessionCleanupScheduler.scheduleWithFixedDelay(() -> {
       long now = System.currentTimeMillis();
       sessionLastAccess.entrySet().removeIf(
           entry -> (now - entry.getValue()) > SESSION_TIMEOUT_MS
       );
   }, 1, 1, TimeUnit.HOURS);
   ```

3. Update last access on each request

**Lesson**: Always implement cleanup for long-running services with state.

### Challenge 4: ReAct Pattern Parsing

**Problem**: LLM output format varied, making parsing difficult.

**Symptoms**:
- Sometimes multi-line format
- Sometimes single-line format
- Inconsistent spacing
- Missing fields

**Root Cause**:
- Regex patterns too strict
- No handling of variations
- Assumed consistent format

**Solution**:
1. Used flexible regex patterns with DOTALL flag:
   ```java
   Pattern.compile("Thought:\\s*(.+?)(?=\\n(?:Action:|Final Answer:)|$)", Pattern.DOTALL)
   ```

2. Made all fields optional in parsing
3. Added fallback handling for missing fields
4. Logged parsing failures for debugging

**Lesson**: Always handle format variations. LLM output is never perfectly consistent.

### Challenge 5: Tool Parameter Parsing

**Problem**: Action Input JSON parsing failed with various formats.

**Symptoms**:
- JSON parse errors
- Missing parameters
- Type mismatches

**Root Cause**:
- LLM sometimes generated invalid JSON
- No validation before parsing
- Assumed correct types

**Solution**:
1. Added try-catch around JSON parsing
2. Validated JSON structure before use
3. Handled missing fields gracefully:
   ```java
   String firstName = inputNode.has("firstName") ? 
       inputNode.get("firstName").asText() : null;
   ```

4. Provided clear error messages

**Lesson**: Always validate and handle parsing errors gracefully. LLM output needs validation.

---

## What Worked Well

### 1. LangChain4j Integration

**Why It Worked**:
- Clean API for tool definitions
- Excellent Ollama integration
- Good documentation
- Active community support

**Key Success Factors**:
- Industry standard = better support
- Mature library = fewer bugs
- Good examples = faster development

### 2. ReAct Pattern

**Why It Worked**:
- Natural flow for LLMs
- Easy to debug (see each step)
- Flexible error handling
- Transparent to users

**Key Success Factors**:
- Simple pattern = easier to implement
- Clear structure = easier to parse
- Iterative = handles complex tasks

### 3. Flexible User Search

**Why It Worked**:
- Better user experience
- Handles real-world scenarios
- Reduces user friction
- Shows system intelligence

**Key Success Factors**:
- Repository layer flexibility
- Service layer abstraction
- Tool layer handles multiple matches

### 4. WebSocket for Real-Time Updates

**Why It Worked**:
- Users see thinking process
- Better UX (no waiting in silence)
- Transparent agent behavior
- Builds trust

**Key Success Factors**:
- Spring Boot WebSocket support
- Simple message format
- Clear separation of concerns

### 5. Comprehensive Testing

**Why It Worked**:
- Caught issues early
- Documented expected behavior
- Easy to verify fixes
- Confidence in changes

**Key Success Factors**:
- Test all combinations
- Test edge cases
- Test error scenarios
- Keep tests updated

---

## What Caused Issues

### 1. Spring AI Migration Pain

**Issue**: Initial implementation used Spring AI, which had limitations.

**Problems**:
- Less mature than LangChain4j
- Fewer examples
- Tool calling not as robust
- Less community support

**Impact**: Required complete rewrite to LangChain4j

**Lesson**: Research framework maturity before committing. Check community activity and examples.

### 2. Plan-Based Approach Complexity

**Issue**: Initial plan-based approach was too complex.

**Problems**:
- Complex JSON structure
- Hard to parse
- Difficult to debug
- LLM struggled with format

**Impact**: Switched to simpler ReAct pattern

**Lesson**: Simpler is better. Don't over-engineer the pattern.

### 3. Assuming LLM Follows Format

**Issue**: Assumed LLM would follow ReAct format without explicit instructions.

**Problems**:
- LLM generated fake Observations
- Inconsistent output format
- Missing fields

**Impact**: Required extensive prompt engineering

**Lesson**: Never assume LLM behavior. Be extremely explicit in prompts.

### 4. Not Handling Multiple Matches

**Issue**: Original getUser tool failed when multiple users matched.

**Problems**:
- Poor user experience
- Lost information
- Required user to provide more details

**Impact**: Had to redesign to handle multiple matches

**Lesson**: Design for real-world scenarios. Users provide partial information.

### 5. No Session Cleanup

**Issue**: Sessions never expired, causing memory leaks.

**Problems**:
- Memory growth over time
- Old data consuming resources
- No way to clean up

**Impact**: Had to add cleanup mechanism later

**Lesson**: Always plan for resource cleanup in long-running services.

---

## Key Learnings

### 1. Prompt Engineering is Critical

**Learning**: The system prompt is the most important part of the system.

**Key Points**:
- Be extremely explicit
- Use examples
- Emphasize what NOT to do
- Test prompt variations
- Iterate based on LLM behavior

**Best Practice**: 
- Write prompt first
- Test with simple cases
- Refine based on failures
- Document prompt decisions

### 2. Tool Design Matters

**Learning**: Well-designed tools make the agent more capable.

**Key Points**:
- Make parameters optional when possible
- Return structured data
- Handle edge cases
- Provide clear errors
- Support partial information

**Best Practice**:
- Design tools for flexibility
- Think about real-world usage
- Handle multiple matches
- Return helpful errors

### 3. Error Handling Strategy

**Learning**: Let the LLM handle errors, don't crash the loop.

**Key Points**:
- Return errors as structured data
- Let LLM decide how to handle
- Provide helpful error messages
- Log for debugging

**Best Practice**:
- Never throw exceptions from tools
- Return error JSON
- Let LLM reason about errors
- Log everything

### 4. Session Management

**Learning**: Proper session management is essential for production.

**Key Points**:
- Use thread-safe collections
- Implement cleanup
- Track last access
- Limit session lifetime

**Best Practice**:
- Use ConcurrentHashMap
- Schedule cleanup tasks
- Set reasonable timeouts
- Monitor memory usage

### 5. Testing Strategy

**Learning**: Comprehensive tests catch issues early.

**Key Points**:
- Test all combinations
- Test edge cases
- Test error scenarios
- Keep tests updated

**Best Practice**:
- Write tests as you develop
- Test flexible search thoroughly
- Test error handling
- Update tests with changes

### 6. Framework Choice

**Learning**: Choose industry-standard frameworks.

**Key Points**:
- Better documentation
- More examples
- Active community
- Faster development

**Best Practice**:
- Research before committing
- Check community activity
- Look for examples
- Consider migration path

---

## Future Multi-Agent Considerations

### 1. Agent Communication

**Current State**: Single agent, no inter-agent communication

**Future Needs**:
- Message passing between agents
- Shared state management
- Coordination protocols
- Conflict resolution

**Considerations**:
- How will agents communicate?
- What information should be shared?
- How to handle conflicts?
- What coordination is needed?

### 2. Agent Specialization

**Current State**: One general-purpose agent

**Future Needs**:
- Specialized agents (user lookup, appointment management, etc.)
- Agent roles and responsibilities
- Task routing to appropriate agent
- Agent capabilities registry

**Considerations**:
- What agents are needed?
- What are their responsibilities?
- How to route tasks?
- How to discover capabilities?

### 3. Shared State Management

**Current State**: Per-session state

**Future Needs**:
- Shared knowledge base
- Agent memory
- Context sharing
- State synchronization

**Considerations**:
- What state should be shared?
- How to synchronize?
- What consistency is needed?
- How to handle conflicts?

### 4. Coordination Patterns

**Current State**: No coordination needed

**Future Needs**:
- Orchestration patterns
- Choreography patterns
- Leader election
- Task delegation

**Considerations**:
- What coordination is needed?
- Centralized or decentralized?
- How to handle failures?
- What patterns to use?

### 5. Monitoring & Observability

**Current State**: Basic logging

**Future Needs**:
- Agent performance metrics
- Communication tracking
- State monitoring
- Failure detection

**Considerations**:
- What metrics to track?
- How to visualize?
- What alerts are needed?
- How to debug?

### 6. Scalability

**Current State**: Single instance

**Future Needs**:
- Horizontal scaling
- Load distribution
- Agent instance management
- Resource allocation

**Considerations**:
- How to scale agents?
- How to distribute load?
- How to manage instances?
- What resources are needed?

---

## Technical Debt & Future Improvements

### 1. Prompt Engineering

**Current**: Manual prompt, hardcoded in code

**Improvement**: 
- Externalize prompts to configuration
- Version control for prompts
- A/B testing framework
- Prompt optimization tools

### 2. Tool Discovery

**Current**: Tools hardcoded in service

**Improvement**:
- Dynamic tool registration
- Tool metadata
- Tool versioning
- Tool discovery API

### 3. Error Recovery

**Current**: Basic error handling

**Improvement**:
- Retry strategies
- Fallback mechanisms
- Error classification
- Recovery protocols

### 4. Performance

**Current**: Sequential tool execution

**Improvement**:
- Parallel tool execution where possible
- Caching strategies
- Request batching
- Connection pooling optimization

### 5. Observability

**Current**: Basic logging

**Improvement**:
- Structured logging
- Metrics collection
- Distributed tracing
- Performance monitoring

---

## Conclusion

This single-agent system provides a solid foundation for multi-agent development. Key takeaways:

1. **Start Simple**: ReAct pattern is simple and effective
2. **Be Explicit**: LLMs need very clear instructions
3. **Design for Flexibility**: Tools should handle partial information
4. **Plan for Production**: Session management, cleanup, error handling
5. **Test Thoroughly**: Comprehensive tests catch issues early
6. **Choose Wisely**: Industry-standard frameworks save time

For multi-agent systems, we'll need to add:
- Agent communication protocols
- Shared state management
- Coordination mechanisms
- Specialized agent roles
- Enhanced monitoring

The foundation is solid. The next step is adding multi-agent capabilities while maintaining the simplicity and effectiveness of the current system.

---

**Last Updated**: 2026-02-08  
**Project**: Agentic Appointment Scheduler  
**Version**: 1.0 (Single Agent)

