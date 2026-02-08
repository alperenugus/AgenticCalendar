# Agentic Appointment Scheduler

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-18-blue)](https://react.dev/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An intelligent appointment scheduling system powered by Spring AI and Large Language Models (LLMs). This system uses a planning-based agentic architecture where the LLM generates structured execution plans that are then executed by the backend, enabling natural language interaction for appointment management.

## 🎯 Project Overview

This is a Spring Boot 3.4 application that demonstrates an **agentic AI system** for appointment scheduling. Unlike traditional chatbots, this system uses a **planning-based approach** where:

1. The LLM analyzes user requests and generates a structured JSON execution plan
2. The backend executes the plan step-by-step, resolving parameters and handling errors
3. Tools are chained together automatically based on the plan
4. Results are extracted and used in subsequent steps

### Key Features

- 🤖 **AI-Powered Agent**: Natural language understanding for appointment requests
- 📋 **Planning-Based Architecture**: LLM generates execution plans, backend executes them
- 🔗 **Tool Chaining**: Automatic chaining of multiple tool calls
- 🔄 **Parameter Resolution**: Dynamic parameter resolution using `${variableName}` syntax
- 📊 **PostgreSQL Persistence**: Robust data storage with JPA
- 🧪 **Comprehensive Testing**: Unit and integration tests included
- 🔒 **Input Validation**: Security safeguards (currently disabled but preserved)
- 📝 **Smart Date Parsing**: Handles various date formats automatically

## 🏗️ Architecture

### System Components

```
┌─────────────────┐
│   User Request  │
└────────┬────────┘
         │
┌────────▼─────────────────────────────────────┐
│         AgentService                         │
│  ┌──────────────────────────────────────┐   │
│  │  1. Generate Execution Plan (LLM)    │   │
│  └──────────────┬───────────────────────┘   │
│                 │                             │
│  ┌──────────────▼───────────────────────┐   │
│  │  2. Parse Execution Plan (JSON)      │   │
│  └──────────────┬───────────────────────┘   │
│                 │                             │
│  ┌──────────────▼───────────────────────┐   │
│  │  3. Execute Plan (PlanExecutor)      │   │
│  │     - Resolve parameters             │   │
│  │     - Execute tools                  │   │
│  │     - Extract values                 │   │
│  │     - Handle errors                 │   │
│  └──────────────┬───────────────────────┘   │
│                 │                             │
│  ┌──────────────▼───────────────────────┐   │
│  │  4. Generate Response (LLM)          │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│            Tool Functions                   │
│  - getUser                                   │
│  - getAppointmentsByUser                   │
│  - createAppointment                        │
│  - updateAppointment                        │
│  - deleteAppointment                        │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│         PostgreSQL Database                  │
│  - Users                                     │
│  - Appointments                              │
└─────────────────────────────────────────────┘
```

### Execution Plan Structure

The LLM generates plans in this format:

```json
{
  "plan": "Human-readable description",
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
        "message": "User not found"
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

#### macOS
```bash
brew install ollama
```

#### Linux
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

#### Windows
Download from [https://ollama.com/download](https://ollama.com/download)

The system requires a model that supports tool calling. Use one of these:

```bash
# Recommended: llama3.1 (supports tool calling)
ollama pull llama3.1

# Alternative: mistral (also supports tool calling)
ollama pull mistral
```

**Important**: `llama3.2` does NOT support tool calling. Use `llama3.1` or `mistral`.

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
  ai:
    # Ollama Configuration (Default - FREE)
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.1  # Use llama3.1 or mistral
          temperature: 0.7

    # OpenAI Configuration (Optional - Requires API Key)
    # Uncomment and set OPENAI_API_KEY environment variable
    # openai:
    #   api-key: ${OPENAI_API_KEY}
    #   chat:
    #     options:
    #       model: gpt-4o-mini
    #       temperature: 0.7
```

### Switching to OpenAI

1. Get an API key from [OpenAI](https://platform.openai.com/api-keys)
2. Set environment variable:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```
3. Update `application.yml` to uncomment OpenAI config and comment Ollama config
4. Update `AppointmentschedulerApplication.java` to remove OpenAI exclusion if needed

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
  -d '{"message": "Book an appointment for Alperen Ugus born on 1990-01-01 for December 25, 2024 at 2 PM for a dental checkup"}'
```

**Response:**
```json
{
  "response": "I've successfully booked your appointment for December 25, 2024 at 2:00 PM for a dental checkup."
}
```

### Example Requests

#### Create Appointment
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Schedule a meeting for Sarah Smith born on 1985-05-15 on January 15, 2025 at 10 AM"}'
```

#### Update Appointment
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Reschedule Alperen Ugus appointment to January 1, 2025 at 3 PM"}'
```

#### Cancel Appointment
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Cancel appointment for Alperen Ugus born on 1990-01-01"}'
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

- **Plan Generation**: Look for `📋 LLM Plan Response`
- **Plan Parsing**: Look for `🔍 Parsing execution plan from JSON`
- **Plan Execution**: Look for `🔵 Executing step X`
- **Parameter Resolution**: Look for `🔧 Resolved placeholder`
- **Value Extraction**: Look for `📦 Extracted`
- **Tool Execution**: Look for `🔵 getUserFunction CALLED`

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

#### 4. Plan Parsing Fails

**Symptom**: `Failed to parse execution plan`

**Possible Causes**:
- LLM returned text instead of JSON
- JSON structure is invalid
- Model doesn't support tool calling (use `llama3.1` or `mistral`)

**Solution**:
- Check logs for the actual LLM response
- Verify model supports tool calling
- Check system prompt in `AgentService.java`

#### 5. Parameter Resolution Fails

**Symptom**: `Placeholder ${variableName} not found in execution context`

**Possible Causes**:
- Value extraction failed in previous step
- Wrong JSON path in `extractFromResult`
- Variable name mismatch

**Solution**:
- Check extraction logs for the previous step
- Verify JSON path matches actual result structure
- Use correct path format: `appointments[0].appointmentId` not `appointmentId`

### Logging Levels

Adjust logging in `application.yml`:

```yaml
logging:
  level:
    com.agent.appointmentscheduler: DEBUG  # Application logs
    org.springframework.ai: DEBUG          # Spring AI logs
    org.hibernate.SQL: DEBUG                # SQL queries
```

## 📊 Performance Analysis

### Response Times

Typical response times:
- **Plan Generation**: 2-5 seconds (depends on LLM)
- **Plan Execution**: 100-500ms (depends on database queries)
- **Total Request**: 3-6 seconds

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
│   │   │   │   │   └── WebConfig.java               # CORS configuration
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AgentController.java
│   │   │   │   │   ├── AppointmentController.java
│   │   │   │   │   └── UserController.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.java
│   │   │   │   │   └── Appointment.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── AppointmentRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AgentService.java
│   │   │   │   │   ├── AppointmentService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── InputValidationService.java
│   │   │   │   ├── tools/
│   │   │   │   │   └── AppointmentTools.java
│   │   │   │   └── util/
│   │   │   │       ├── ExecutionPlan.java
│   │   │   │       ├── PlanExecutor.java
│   │   │   │       ├── DateParser.java
│   │   │   │       ├── MessageExtractor.java
│   │   │   │       └── ToolCallParser.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/                                     # Tests
│   ├── docker-compose.yml                           # PostgreSQL setup
│   └── pom.xml                                      # Maven dependencies
├── frontend/                                        # React frontend
│   ├── src/
│   │   ├── components/
│   │   │   ├── ChatComponent.jsx
│   │   │   └── AppointmentTable.jsx
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

1. Add function in `AppointmentTools.java`:
```java
@Bean
@Qualifier("myNewFunction")
public FunctionCallback myNewFunction() {
    return FunctionCallbackWrapper.builder(
        (Function<Map<String, Object>, MyResponse>) arguments -> {
            // Implementation
        }
    )
    .withName("myNewFunction")
    .withDescription("Description of what it does")
    .build();
}
```

2. Register in `AgentService` constructor
3. Update system prompt in `AgentService` to mention the new tool

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is open source and available under the MIT License.

## 🙏 Acknowledgments

- **Spring AI** for LLM integration
- **Ollama** for local LLM support
- **Spring Boot** for the framework
- **PostgreSQL** for data persistence

## 📞 Support

For issues, questions, or contributions, please open an issue on the repository.

---

**Happy Scheduling! 🎉**
