# WebSocket Real-Time Thinking Setup

## Overview

The agent now supports real-time WebSocket communication to send thinking steps and tool calls to the frontend as they happen, providing a live view of the agent's reasoning process.

## Backend Setup

### Dependencies
WebSocket support is already added to `pom.xml`:
- `spring-boot-starter-websocket`

### Configuration
- **WebSocketConfig**: Configures STOMP over WebSocket
- **WebSocketService**: Service for sending real-time updates
- **AgentService**: Integrated to send thinking steps via WebSocket

### Endpoints
- WebSocket endpoint: `/ws`
- Topics:
  - `/topic/thinking/{sessionId}` - Thinking steps and tool calls
  - `/topic/response/{sessionId}` - Final responses
  - `/topic/error/{sessionId}` - Error messages

## Frontend Setup

### Install Dependencies
```bash
cd frontend
npm install sockjs-client @stomp/stompjs
```

### How It Works
1. Frontend connects to WebSocket on component mount
2. Subscribes to topics for the session ID
3. Receives real-time updates as agent processes:
   - Thinking messages
   - Tool calls with arguments
   - Tool results
   - Final responses
4. Displays updates immediately in the UI

### Features
- **Connection Status**: Shows if WebSocket is connected
- **Real-time Thinking**: See agent's reasoning as it happens
- **Tool Call Display**: Shows which tools are being used with arguments
- **Fallback**: If WebSocket fails, falls back to HTTP response

## Usage

1. Start the backend server
2. Install frontend dependencies: `npm install` in frontend directory
3. Start the frontend: `npm run dev`
4. Open the chat interface
5. Send a message - you'll see thinking steps appear in real-time!

## Session Management

Each chat session gets a unique session ID. The frontend generates one on mount and passes it in the `X-Session-Id` header. This ensures WebSocket messages are routed to the correct client.



