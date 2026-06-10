# Agentic Calendar

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.34-blue)](https://github.com/langchain4j/langchain4j)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An intelligent **AI-powered calendar assistant** that works like Google Calendar but with natural language interaction. Powered by **LangChain4j** and Large Language Models (LLMs), using the **ReAct (Reasoning and Acting) pattern** for intelligent calendar management.

## 🎯 Project Overview

**Agentic Calendar** is a full-stack calendar application that combines:
- 🤖 **AI Assistant**: Natural language calendar management (schedule, reschedule, query)
- 📅 **Google Calendar-like UI**: Beautiful month/week/day views with event visualization
- 🔄 **Recurring Events**: Full support for daily, weekly, monthly, and custom recurrence patterns
- 🔐 **Two sign-in methods**: Google OAuth **and** username/password — with proper per-user data isolation
- ⚡ **Real-time Updates**: WebSocket-powered live thinking and tool execution
- 🛡️ **Rate Limiting**: Industry-standard token bucket algorithm to prevent abuse

### Key Features

- 📅 **Smart Calendar Management**: Create, update, delete, and query events with natural language
- 🔁 **Recurring Events**: Support for daily, weekly, monthly, yearly, and custom patterns
- 🎨 **Beautiful UI**: Google Calendar-inspired interface with month/week/day views
- 🤖 **AI-Powered**: Natural language understanding for all calendar operations
- 🔍 **Conflict Detection**: Automatically checks for scheduling conflicts
- ⏰ **Real-time Thinking**: See the AI's reasoning process in real-time via WebSocket
- 🔐 **Google OAuth**: Secure authentication with Google Sign-In
- 📊 **PostgreSQL**: Robust data persistence with JPA/Hibernate
- 🛡️ **Rate Limiting**: Per-session rate limiting to prevent token abuse
- 💬 **WebSocket Support**: Real-time communication with thinking updates

## 🏗️ Architecture

### System Components

```
┌─────────────────┐
│   User Request  │
└────────┬────────┘
         │
┌────────▼─────────────────────────────────────┐
│         AgentService (LangChain4j)          │
│  ┌──────────────────────────────────────┐   │
│  │  1. Thought: LLM reasons            │   │
│  └──────────────┬───────────────────────┘   │
│                 │                             │
│  ┌──────────────▼───────────────────────┐   │
│  │  2. Action: LLM selects tool         │   │
│  └──────────────┬───────────────────────┘   │
│                 │                             │
│  ┌──────────────▼───────────────────────┐   │
│  │  3. Observation: Tool executed      │   │
│  └──────────────┬───────────────────────┘   │
│                 │                             │
│  ┌──────────────▼───────────────────────┐   │
│  │  4. Thought: LLM evaluates result   │   │
│  │     - Continue with another Action? │   │
│  │     - Or provide Final Answer?      │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│         CalendarToolService                 │
│  - createEvent(title, startTime, endTime,   │
│    description?, location?, recurrenceRule?)│
│  - getEvents(sessionId, googleUserId?)      │
│  - getEventsByDateRange(...)                │
│  - checkConflicts(...)                      │
│  - updateEvent(...)                         │
│  - deleteEvent(...)                         │
│  - getUpcomingEvents(...)                   │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│         PostgreSQL Database                  │
│  - Events (with recurrence support)          │
│  - Google OAuth sessions                     │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│         React Frontend                      │
│  - Google Calendar-like UI                   │
│  - Month/Week/Day views                      │
│  - Recurring event expansion                 │
│  - Real-time WebSocket updates               │
└─────────────────────────────────────────────┘
```

### ReAct Pattern Flow

```
User: "Schedule a weekly team meeting every Monday at 2pm"
  │
  ├─> Thought: "User wants a recurring weekly event. I need to parse the date, 
  │             calculate endTime, check conflicts, and include recurrenceRule."
  ├─> Action: checkConflicts
  ├─> Observation: {"hasConflicts": false}
  │
  ├─> Thought: "No conflicts. Now create the recurring event with recurrenceRule."
  ├─> Action: createEvent
  ├─> Action Input: {"title": "Team Meeting", "startTime": "2024-12-30T14:00:00", 
  │                  "endTime": "2024-12-30T15:00:00", "recurrenceRule": "every Monday"}
  ├─> Observation: {"eventId": 2, "recurrenceRule": "FREQ=WEEKLY;BYDAY=MO", ...}
  │
  └─> Final Answer: "I've created a recurring weekly team meeting every Monday at 2:00 PM..."
```

## 🚀 Quick Start

### Prerequisites

- **Java 21+**
- **Maven 3.6+**
- **Node.js 18+**
- **Docker** (for PostgreSQL)
- **Groq API Key** (FREE - 100,000 tokens/day) OR **OpenAI API Key**

### Installation

1. **Clone the repository:**
```bash
git clone https://github.com/alperenugus/AgenticCalendar.git
cd AgenticCalendar
```

2. **Set up backend:**
```bash
cd backend
mvn clean install
docker-compose up -d  # Start PostgreSQL
```

3. **Set up frontend:**
```bash
cd frontend
npm install
```

4. **Configure environment variables:**

**Backend** (`backend/src/main/resources/application.yml` or environment variables):
```bash
# LLM Provider (Required)
LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key
LANGCHAIN4J_PROVIDER=groq

# Google OAuth (Required for production)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google
FRONTEND_URL=https://your-frontend.railway.app
```

**Frontend** (environment variables):
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=http://localhost:8080/ws
```

5. **Start the application:**

**Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Frontend** (in a new terminal):
```bash
cd frontend
npm run dev
```

The backend will be available at `http://localhost:8080`  
The frontend will be available at `http://localhost:5173`

## 📋 Features

### Calendar Operations

The AI assistant can handle:

- ✅ **Create Events**: "Schedule a meeting tomorrow at 2pm"
- ✅ **Recurring Events**: "Create a weekly team meeting every Monday at 2pm"
- ✅ **Query Calendar**: "What's on my calendar next week?"
- ✅ **Reschedule**: "Move my 3pm meeting to Friday"
- ✅ **Cancel Events**: "Cancel my meeting on Monday"
- ✅ **Conflict Detection**: Automatically checks for scheduling conflicts
- ✅ **Find Free Time**: "When am I free for a 1-hour call?"

### Recurring Events

Full support for recurring patterns:
- **Daily**: "Schedule a daily standup at 9am"
- **Weekly**: "Create a weekly team meeting every Monday"
- **Monthly**: "Add a monthly review on the first Friday"
- **Yearly**: "Schedule an annual review on January 1st"
- **Weekdays**: "Create a meeting every weekday at 10am"
- **Custom**: "Every 2 weeks", "Every 3 months", etc.

Recurring events are automatically expanded and displayed on all applicable days in the calendar view.

### Google Calendar-like UI

- **Month View**: Grid layout showing all events on their dates
- **Week View**: Hourly timeline with event blocks positioned by time
- **Day View**: Detailed single-day view with full event information
- **Event Colors**: Customizable event colors
- **Real-time Updates**: Auto-refreshes every 10 seconds

## 🔐 Authentication & data isolation

Two sign-in methods, one session model:

1. **Google OAuth2** — sign in with Google.
2. **Username/password** — register/sign in with email + password (BCrypt-hashed).

**Identity is always derived server-side from the authenticated session** (never from a client-supplied id). Each user's calendar is scoped to their own owner key (Google `sub` or `local-<id>`), so users only ever see their own data. All `/api/**` routes require authentication except the public ones (`/api/auth/**`, `/actuator/health`, `/ws`, `/oauth2`, `/login`).

### Setting up Google OAuth

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create OAuth 2.0 Client ID
3. Add authorized redirect URI: `https://your-backend.railway.app/login/oauth2/code/google`
4. Copy Client ID and Secret to environment variables

## 📡 API Endpoints

### Agent Endpoints
- **POST** `/api/agent/chat` - Send natural language requests _(authenticated; identity from session)_
- **GET** `/api/agent/history` - Get conversation history _(authenticated)_

### Event Endpoints _(all authenticated; scoped to the current user)_
- **GET** `/api/events` - Get the current user's events
- **GET** `/api/events/{id}` - Get a specific event (404 if not yours)
- **GET** `/api/events/count` - Count of the current user's events

### Auth Endpoints
- **GET** `/api/auth/user` - Get current authenticated user (Google or local)
- **POST** `/api/auth/register` - Create a username/password account `{email, password, name}`
- **POST** `/api/auth/login` - Sign in with username/password `{email, password}`
- **GET** `/oauth2/authorization/google` - Initiate Google login
- **POST** `/logout` - Logout

### Ops
- **GET** `/actuator/health` - Health check (used by Railway)

## 🏛️ Project Structure

```
AgenticCalendar/
├── backend/                                          # Spring Boot backend
│   ├── src/main/java/com/agent/agenticcalendar/
│   │   ├── AgenticCalendarApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java                  # OAuth2 + form login, /api/** lockdown, CORS
│   │   │   ├── OAuth2Config.java                    # OAuth redirect URI fix (Railway proxy)
│   │   │   ├── LangChain4jConfig.java               # LLM (Groq/Ollama) configuration
│   │   │   └── WebSocketConfig.java                 # WebSocket setup
│   │   ├── controller/
│   │   │   ├── AgentController.java                 # Agent chat (identity from session)
│   │   │   ├── EventController.java                 # Per-user event reads
│   │   │   └── AuthController.java                  # Google + username/password auth
│   │   ├── model/
│   │   │   ├── Event.java                           # Calendar event entity
│   │   │   ├── LocalAccount.java                    # Username/password account (BCrypt)
│   │   │   ├── EventType.java / EventStatus.java    # Enums
│   │   ├── repository/                              # Event + LocalAccount repositories
│   │   ├── security/
│   │   │   ├── CurrentUser.java                     # Server-derived owner key (sub / local-<id>)
│   │   │   ├── LocalUserPrincipal.java              # UserDetails for local accounts
│   │   │   └── LocalUserDetailsService.java
│   │   ├── service/
│   │   │   ├── AgentService.java                    # Custom text-based ReAct loop
│   │   │   ├── EventService.java                    # Event business logic + recurrence
│   │   │   └── RateLimitService.java                # Rate limiting
│   │   ├── tools/
│   │   │   └── CalendarToolService.java             # Calendar tools
│   │   └── util/                                    # RecurrenceParser/Expander, DateParser, ReActParser
│   └── pom.xml
├── frontend/                                        # React frontend
│   ├── src/
│   │   ├── components/
│   │   │   ├── ChatComponent.jsx                    # AI chat interface
│   │   │   ├── CalendarView.jsx                     # Google Calendar-like UI
│   │   │   └── LoginScreen.jsx                      # Google + email/password login
│   │   ├── utils/recurrenceExpander.js              # Expand recurring events (client side)
│   │   └── App.jsx
│   └── package.json
├── CLAUDE.md                                         # Build/deploy/test playbook (Railway CLI)
├── EVALUATION_REPORT.md                              # Code-review findings + backlog
└── README.md
```

> **Note:** the files under `backend/src/main/resources/db/migration/` are inert — Flyway is not a dependency, so the schema is managed by Hibernate `ddl-auto=update`. See `EVALUATION_REPORT.md`.

## 🧪 Example Interactions

### Create One-time Event
```
User: "Schedule a meeting tomorrow at 2pm"
Agent: "I've scheduled a meeting for tomorrow, February 14, 2026 at 2:00 PM. The event has been added to your calendar."
```

### Create Recurring Event
```
User: "Create a weekly team meeting every Monday at 2pm"
Agent: "I've created a recurring weekly team meeting every Monday at 2:00 PM, starting February 17, 2026. The event has been added to your calendar and will repeat weekly."
```

### Query Calendar
```
User: "What's on my calendar next week?"
Agent: "Next week, you have 3 events:
- Team Meeting on Monday, February 17 at 2:00 PM
- Daily Standup on Tuesday, February 18 at 9:00 AM
- Monthly Review on Friday, February 21 at 3:00 PM"
```

### Reschedule Event
```
User: "Reschedule my 2pm meeting to Friday"
Agent: "I've rescheduled your meeting to Friday, February 21, 2026 at 2:00 PM."
```

## 🚀 Deployment

### Railway Deployment

Two services (`AgenticCalendarBackend` via Dockerfile, `AgenticCalendarFrontend` via Nixpacks) + Postgres, auto-deployed from GitHub `main`. The full build/deploy/verify workflow — including the Railway-CLI commands and the frontend `railway.json` gotcha — is documented in **`CLAUDE.md`**. Production testing is done against the live URL with Playwright (see `CLAUDE.md`).

**Quick Setup:**
1. Connect the GitHub repository to Railway (or deploy with `railway up`).
2. Create backend, frontend, and Postgres services.
3. Set environment variables (`LANGCHAIN4J_GROQ_API_KEY`, `GOOGLE_CLIENT_ID/SECRET`, `GOOGLE_REDIRECT_URI`, `FRONTEND_URL`).
4. Configure the Google OAuth redirect URI.
5. Deploy and verify `/actuator/health`.

## 📝 License

This project is open source and available under the MIT License.

## 🙏 Acknowledgments

- **LangChain4j** for LLM integration
- **Spring Boot** for the backend framework
- **React** for the frontend
- **PostgreSQL** for data persistence
- **Groq** for free LLM API access

---

**Happy Scheduling! 🎉**
