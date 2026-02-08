# Agentic Appointment Scheduler

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.34-blue)](https://github.com/langchain4j/langchain4j)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An intelligent appointment scheduling system powered by **LangChain4j** and Large Language Models (LLMs). This system uses the **ReAct (Reasoning and Acting) pattern** where the LLM reasons about user requests, selects appropriate tools, and iteratively works through tasks until completion.

## 🎯 Project Overview

This is a Spring Boot 3.4 application that demonstrates an **agentic AI system** for appointment scheduling. The system uses the **ReAct pattern** where:

1. **Thought**: LLM describes what it needs to do
2. **Action**: LLM selects a tool and provides arguments
3. **Observation**: Java code executes the tool and returns the result to the LLM
4. **Thought**: LLM evaluates the result and decides if it's done
5. **Final Answer**: LLM provides the response to the user

### Key Features

- 🤖 **AI-Powered Agent**: Natural language understanding for appointment requests using LangChain4j
- 🔄 **ReAct Pattern**: Industry-standard reasoning and acting loop
- 🔍 **Flexible User Search**: Search users with partial information (firstName, lastName, or dob - all optional)
- 👥 **User Management**: Create, update, and delete users via natural language
- 🔗 **Tool Chaining**: Automatic chaining of multiple tool calls
- 📊 **PostgreSQL Persistence**: Robust data storage with JPA
- 🧪 **Comprehensive Testing**: Unit and integration tests included
- 🔒 **Input Validation**: Security safeguards
- 📝 **Smart Date Parsing**: Handles various date formats automatically
- 💬 **WebSocket Support**: Real-time communication with thinking updates
- 🛡️ **Rate Limit Handling**: Graceful error handling for API rate limits

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
│         AppointmentToolService             │
│  - getUser(firstName?, lastName?, dob?)    │
│  - getAppointmentsByUser(userId)            │
│  - createAppointment(...)                  │
│  - updateAppointment(...)                  │
│  - deleteAppointment(...)                  │
│  - createUser(firstName, lastName, dob, email) │
│  - updateUser(userId, ...)                 │
│  - deleteUser(userId)                      │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│         PostgreSQL Database                  │
│  - Users                                     │
│  - Appointments                              │
└─────────────────────────────────────────────┘
```

### ReAct Pattern Flow

```
User: "Update my appointment"
  │
  ├─> Thought: "I need to find the user first"
  ├─> Action: getUser(firstName="John")
  ├─> Observation: {"userId": 123, ...}
  │
  ├─> Thought: "Found user. Now get their appointments"
  ├─> Action: getAppointmentsByUser(userId=123)
  ├─> Observation: [{"appointmentId": 456, ...}]
  │
  ├─> Thought: "Found appointment. Now update it"
  ├─> Action: updateAppointment(appointmentId=456, ...)
  ├─> Observation: {"message": "Updated successfully"}
  │
  └─> Final Answer: "I've updated your appointment..."
```

## 📋 Requirements

### Software Requirements

- **Java 21** or higher
- **Maven 3.6+**
- **Docker** and **Docker Compose** (for PostgreSQL)
- **Ollama** (for local LLM) OR **OpenAI API Key** (for cloud LLM)

### System Requirements

- **RAM**: Minimum 8GB (16GB recommended for Ollama)
- **Disk Space**: ~5GB for dependencies and models
- **Network**: Internet connection for Maven dependencies

## 🚀 Installation & Setup

> **⚠️ Important**: All backend commands must be run from the `backend/` directory. All frontend commands must be run from the `frontend/` directory.

### Quick Setup (Recommended)

We provide an automated setup script that handles everything:

```bash
git clone <repository-url>
cd appointmentscheduler
chmod +x setup.sh
./setup.sh
```

The script will:
- ✅ Check all prerequisites (Java 21+, Maven 3.6+, Node.js 18+, Docker, Ollama)
- ✅ Download and compile backend dependencies
- ✅ Set up PostgreSQL in Docker (if Docker is available)
- ✅ Install frontend dependencies
- ✅ Pull required Ollama model (if Ollama is installed)
- ✅ Provide clear error messages if anything fails

### Manual Setup

If you prefer to set up manually or the script fails:

#### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd appointmentscheduler
```

#### Step 2: Backend Setup

```bash
cd backend
mvn clean install
docker-compose up -d  # Start PostgreSQL
```

#### Step 3: Frontend Setup

```bash
cd frontend
npm install
```

#### Step 4: Install Ollama (Recommended for Local Development)

Ollama is a free, open-source tool for running LLMs locally.

**macOS**
```bash
brew install ollama
```

**Linux**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**Windows**
Download from [https://ollama.com/download](https://ollama.com/download)

The system requires a model that supports structured output. Use one of these:

```bash
# Recommended: llama3.1 (supports structured output)
ollama pull llama3.1

# Alternative: mistral (also supports structured output)
ollama pull mistral
```

**Important**: `llama3.2` does NOT support structured output well. Use `llama3.1` or `mistral`.

#### Step 5: Start Services

**Start PostgreSQL** (if not using Docker):
```bash
cd backend
docker-compose up -d
```

**Start Ollama** (if using local LLM):
```bash
ollama serve
```

**Start Backend**:
```bash
# Option 1: Use convenience script (from root)
./start-backend.sh

# Option 2: Manual (from root)
cd backend && mvn spring-boot:run
```

**Start Frontend** (in a new terminal):
```bash
# Option 1: Use convenience script (from root)
./start-frontend.sh

# Option 2: Manual (from root)
cd frontend && npm run dev
```

The backend will be available at `http://localhost:8080`  
The frontend will be available at `http://localhost:5173`

## ⚙️ Configuration

### Application Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  langchain4j:
    # Ollama Configuration (Default - FREE)
    ollama:
      base-url: http://localhost:11434
      model: llama3.1  # Use llama3.1 or mistral
      temperature: 0.7

    # Groq Configuration (Default - FREE tier: 100,000 tokens/day)
    # Get API key: https://console.groq.com/keys
    groq:
      api-key: ${LANGCHAIN4J_GROQ_API_KEY:}
      model: llama-3.1-8b-instant  # Smaller model to avoid rate limits
      # Alternative: llama-3.3-70b-versatile (more capable but token-heavy)
      temperature: 0.7
    
    # OpenAI Configuration (Optional - Requires API Key)
    # Set LANGCHAIN4J_PROVIDER=openai to use OpenAI
    # openai:
    #   api-key: ${LANGCHAIN4J_OPENAI_API_KEY:}
    #   model: gpt-4o-mini
    #   temperature: 0.7
```

### Switching to OpenAI

1. Get an API key from [OpenAI](https://platform.openai.com/api-keys) or use Groq (free) from [Groq Console](https://console.groq.com/keys)
2. Set environment variable:
   ```bash
   export LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key-here
   ```
3. The application defaults to Groq. To use OpenAI, set `LANGCHAIN4J_PROVIDER=openai` and configure OpenAI API key.

### Database Configuration

Default PostgreSQL settings (in `backend/src/main/resources/application.yml`):
- URL: `jdbc:postgresql://localhost:5432/appointmentscheduler`
- Username: `postgres`
- Password: `postgres`

To change, update `application.yml` or use environment variables.

## 🧪 Testing

### Run All Tests

```bash
cd backend
mvn test
```

### Run Specific Test Classes

```bash
cd backend
# Unit tests
mvn test -Dtest=AgentServiceTest
mvn test -Dtest=AppointmentServiceTest
mvn test -Dtest=UserServiceTest

# Integration tests
mvn test -Dtest=AgentServiceIntegrationTest
mvn test -Dtest=DataInitializationTest
```

### Test Coverage

```bash
# Generate test coverage report (requires jacoco plugin)
mvn clean test jacoco:report
```

### Test Users

The system automatically creates 5 test users on first startup:

1. **Alperen Ugus** - DOB: 1990-01-01 - Email: alperen.ugus@example.com
2. **Sarah Smith** - DOB: 1985-05-15 - Email: sarah.smith@example.com
3. **John Doe** - DOB: 1992-08-20 - Email: john.doe@example.com
4. **Emily Johnson** - DOB: 1988-03-10 - Email: emily.johnson@example.com
5. **Michael Brown** - DOB: 1995-11-25 - Email: michael.brown@example.com

## 📡 API Usage

### Main Agent Endpoint

**POST** `/api/agent/chat`

Send natural language requests to the agent.

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Book an appointment for Alperen Ugus born on 1990-01-01 for December 25, 2024 at 2 PM for a dental checkup", "sessionId": "session-123"}'
```

**Response:**
```json
{
  "response": "I've successfully booked your appointment for December 25, 2024 at 2:00 PM for a dental checkup.",
  "thinkingSteps": [...]
}
```

### Example Requests

#### Create Appointment
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Schedule a meeting for Sarah Smith born on 1985-05-15 on January 15, 2025 at 10 AM", "sessionId": "session-123"}'
```

#### Update Appointment (Flexible Search)
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Reschedule Alperen appointment to January 1, 2025 at 3 PM", "sessionId": "session-123"}'
```

Note: The system can find users with just a first name, last name, or date of birth!

#### Cancel Appointment
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Cancel appointment for Alperen Ugus born on 1990-01-01", "sessionId": "session-123"}'
```

### Debug Endpoints

#### Get All Users
```bash
curl http://localhost:8080/api/users
```

#### Get User Count
```bash
curl http://localhost:8080/api/users/count
```

#### Get All Appointments
```bash
curl http://localhost:8080/api/appointments
```

#### Get Appointments by User
```bash
curl http://localhost:8080/api/appointments/user/1
```

#### Get Appointment by ID
```bash
curl http://localhost:8080/api/appointments/1
```

## 🐛 Debugging

### Enable Debug Logging

The application already has debug logging enabled. Check logs for:

- **ReAct Loop**: Look for `LLM Response (iteration X)`
- **Tool Execution**: Look for `🔵 getUser CALLED`
- **Observation**: Look for `Tool executed:`
- **Final Answer**: Look for `Sent final response`

### Common Issues

#### 1. Ollama Connection Error

**Symptom**: `Connection refused` or `Failed to connect to Ollama`

**Solution**:
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running, start it
ollama serve
```

#### 2. Model Not Found

**Symptom**: `model 'llama3.1' not found`

**Solution**:
```bash
# Pull the required model
ollama pull llama3.1
```

#### 3. Database Connection Error

**Symptom**: `Connection to localhost:5432 refused`

**Solution**:
```bash
# Start PostgreSQL
docker-compose up -d

# Check if it's running
docker ps
```

#### 4. LLM Not Following ReAct Pattern

**Symptom**: LLM generates Observations instead of waiting for tool results

**Possible Causes**:
- Model doesn't support structured output well
- System prompt not clear enough

**Solution**:
- Use `llama3.1` or `mistral` (not `llama3.2`)
- Check system prompt in `AgentService.java`
- Review logs to see what LLM is generating

### Logging Levels

Adjust logging in `application.yml`:

```yaml
logging:
  level:
    com.agent.appointmentscheduler: DEBUG  # Application logs
    dev.langchain4j: DEBUG                  # LangChain4j logs
    org.hibernate.SQL: DEBUG                # SQL queries
```

## 📊 Performance Analysis

### Response Times

Typical response times:
- **Tool Execution**: 50-200ms (depends on database queries)
- **LLM Reasoning**: 2-5 seconds (depends on LLM)
- **Total Request**: 3-6 seconds (for multi-step tasks)

### Optimization Tips

1. **Use Local Ollama**: Faster than cloud APIs (no network latency)
2. **Database Indexing**: Already configured for `firstName`, `lastName`, `dob`
3. **Connection Pooling**: HikariCP is configured by default
4. **Caching**: Consider adding Redis for frequently accessed data

### Monitoring

Check application metrics:
```bash
# View application logs
tail -f logs/application.log

# Monitor database connections
docker exec -it appointmentscheduler-postgres psql -U postgres -c "SELECT * FROM pg_stat_activity;"
```

## 🏛️ Project Structure

```
appointmentscheduler/
├── backend/                                          # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agent/appointmentscheduler/
│   │   │   │   ├── AppointmentschedulerApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── DataInitializer.java
│   │   │   │   │   ├── LangChain4jConfig.java      # LangChain4j configuration
│   │   │   │   │   ├── WebConfig.java                # CORS configuration
│   │   │   │   │   └── WebSocketConfig.java         # WebSocket configuration
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AgentController.java
│   │   │   │   │   ├── AppointmentController.java
│   │   │   │   │   └── UserController.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Appointment.java
│   │   │   │   │   ├── AgentResponse.java
│   │   │   │   │   ├── ConversationContext.java
│   │   │   │   │   └── ConversationMessage.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── AppointmentRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AgentService.java            # ReAct pattern implementation
│   │   │   │   │   ├── AppointmentService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── InputValidationService.java
│   │   │   │   │   └── WebSocketService.java
│   │   │   │   ├── tools/
│   │   │   │   │   └── AppointmentToolService.java  # LangChain4j tools
│   │   │   │   └── util/
│   │   │   │       ├── ReActParser.java             # ReAct pattern parser
│   │   │   │       └── DateParser.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/                                     # Tests
│   ├── docker-compose.yml                           # PostgreSQL setup
│   └── pom.xml                                      # Maven dependencies
├── frontend/                                        # React frontend
│   ├── src/
│   │   ├── components/
│   │   │   ├── ChatComponent.jsx
│   │   │   ├── AppointmentTable.jsx
│   │   │   └── UserTable.jsx
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   ├── package.json
│   ├── vite.config.js
│   └── tailwind.config.js
├── setup.sh                                         # Automated setup script
├── start-backend.sh                                 # Convenience script to start backend
├── start-frontend.sh                                # Convenience script to start frontend
└── README.md                                        # This file
```

## 🔧 Development

### Building from Source

```bash
cd backend
# Clean and build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Create executable JAR
mvn clean package
java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

### Code Style

The project follows standard Java conventions. Use your IDE's auto-formatting.

### Adding New Tools

1. Add method in `AppointmentToolService.java`:
```java
@Tool("Description of what the tool does")
public String myNewTool(String param1, Long param2) {
    // Implementation
    return "{\"result\": \"...\"}";
}
```

2. Update system prompt in `AgentService.java` to mention the new tool
3. The tool will be automatically available to the LLM

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is open source and available under the MIT License.

## 🙏 Acknowledgments

- **LangChain4j** for LLM integration (industry standard)
- **Ollama** for local LLM support
- **Spring Boot** for the framework
- **PostgreSQL** for data persistence

## 📞 Support

For issues, questions, or contributions, please open an issue on the repository.

---

**Happy Scheduling! 🎉**
