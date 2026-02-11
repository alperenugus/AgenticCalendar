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
- **Groq API Key** (FREE - recommended) OR **OpenAI API Key** (for cloud LLM)

### System Requirements

- **RAM**: Minimum 4GB (8GB recommended)
- **Disk Space**: ~2GB for dependencies
- **Network**: Internet connection for Maven dependencies and LLM API calls

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
- ✅ Check all prerequisites (Java 21+, Maven 3.6+, Node.js 18+, Docker)
- ✅ Download and compile backend dependencies
- ✅ Set up PostgreSQL in Docker (if Docker is available)
- ✅ Install frontend dependencies
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

#### Step 4: Configure LLM Provider

The application defaults to **Groq** (FREE tier: 100,000 tokens/day). Get your API key:

1. Sign up at [https://console.groq.com](https://console.groq.com)
2. Get your API key from [https://console.groq.com/keys](https://console.groq.com/keys)
3. Set the environment variable:
   ```bash
   export LANGCHAIN4J_GROQ_API_KEY=your-api-key-here
   ```

**Alternative: Ollama (Local Development Only)**
If you prefer to run LLMs locally, you can use Ollama:
- Install: `brew install ollama` (macOS) or visit [https://ollama.com](https://ollama.com)
- Start: `ollama serve`
- Pull model: `ollama pull llama3.1`
- Set: `export LANGCHAIN4J_PROVIDER=ollama`

**Note**: Ollama is for local development only. For production/deployment, use Groq or OpenAI.

#### Step 5: Start Services

**Start PostgreSQL** (if not using Docker):
```bash
cd backend
docker-compose up -d
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
langchain4j:
  # Provider selection: "groq" is default (free tier: 100,000 tokens/day)
  # Set LANGCHAIN4J_PROVIDER=ollama to use local Ollama instead
  provider: groq  # Default to Groq (works both locally and on Railway)
  
  # Groq Configuration (FREE tier: 100,000 tokens/day)
  # Get API key: https://console.groq.com/keys
  # Set LANGCHAIN4J_GROQ_API_KEY environment variable
  groq:
    api-key: ${LANGCHAIN4J_GROQ_API_KEY:}
    # llama-3.1-8b-instant: Fast, efficient, uses ~10x fewer tokens than 70b models
    # llama-3.3-70b-versatile: More capable but uses many more tokens (may hit rate limits)
    model: llama-3.1-8b-instant  # Switched to smaller model to avoid rate limits
    # Alternative models: llama-3.3-70b-versatile (more capable but token-heavy), mixtral-8x7b-32768
    temperature: 0.7
  
  # Ollama Configuration (for local development - optional)
  # To use Ollama, set LANGCHAIN4J_PROVIDER=ollama and run: docker run -d -p 11434:11434 ollama/ollama
  ollama:
    base-url: ${LANGCHAIN4J_OLLAMA_BASE_URL:http://localhost:11434}
    model: ${LANGCHAIN4J_OLLAMA_MODEL:llama3.1}
    temperature: ${LANGCHAIN4J_OLLAMA_TEMPERATURE:0.7}
    
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

#### Create User
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Create a new user named Jane Doe, born on 1995-06-15, email jane.doe@example.com", "sessionId": "session-123"}'
```

#### Update User
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Update Alperen Ugus email to newemail@example.com", "sessionId": "session-123"}'
```

#### Delete User
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Delete user with ID 5", "sessionId": "session-123"}'
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

#### 1. Groq Rate Limit Error

**Symptom**: `Rate limit reached` or `tokens per day limit`

**Solution**:
- The free tier has a 100,000 tokens/day limit
- Wait until the next day or switch to a different model
- Consider using `llama-3.1-8b-instant` (more token-efficient) instead of `llama-3.3-70b-versatile`
- For production, consider upgrading to a paid Groq tier or using OpenAI

#### 2. Missing Groq API Key

**Symptom**: `API key is required` or authentication errors

**Solution**:
```bash
# Set the API key as environment variable
export LANGCHAIN4J_GROQ_API_KEY=your-api-key-here

# Or add to your shell profile (~/.bashrc, ~/.zshrc, etc.)
echo 'export LANGCHAIN4J_GROQ_API_KEY=your-api-key-here' >> ~/.zshrc
```

Get your API key from: [https://console.groq.com/keys](https://console.groq.com/keys)

#### 3. Database Connection Error

**Symptom**: `Connection to localhost:5432 refused`

**Solution**:
```bash
# Start PostgreSQL
docker-compose up -d

# Check if it's running
docker ps
```

#### 4. Ollama Connection Error (If Using Local LLM)

**Symptom**: `Connection refused` or `Failed to connect to Ollama`

**Solution**:
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running, start it
ollama serve

# Pull the required model
ollama pull llama3.1
```

**Note**: Ollama is optional and only for local development. The default is Groq.

#### 5. LLM Not Following ReAct Pattern

**Symptom**: LLM generates Observations instead of waiting for tool results

**Possible Causes**:
- Model doesn't support structured output well
- System prompt not clear enough

**Solution**:
- Use `llama3.1` or `mistral` (not `llama3.2`)
- Check system prompt in `AgentService.java`
- Review logs to see what LLM is generating

#### 5. Rate Limit Exceeded

**Symptom**: Error message about rate limits or tokens per day

**Solution**:
- The system automatically handles rate limit errors with user-friendly messages
- Switch to `llama-3.1-8b-instant` model (uses ~10x fewer tokens)
- Wait for the daily token limit to reset (100,000 tokens/day for Groq free tier)
- Consider upgrading to Groq Dev Tier if you need more capacity

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

1. **Use Groq's Efficient Models**: `llama-3.1-8b-instant` uses ~10x fewer tokens than 70b models
2. **Database Indexing**: Already configured for `firstName`, `lastName`, `dob`
3. **Connection Pooling**: HikariCP is configured by default
4. **Caching**: Consider adding Redis for frequently accessed data
5. **Local Development**: Use Ollama for faster local testing (no network latency)

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
