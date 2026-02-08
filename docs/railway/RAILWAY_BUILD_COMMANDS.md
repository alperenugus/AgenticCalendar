# Railway Build Commands

## Manual Build Commands for Railway

### Option 1: From Root Directory (Recommended)

If Railway sets the working directory to the repository root:

**Build Command:**
```bash
cd backend && ./mvnw clean package -DskipTests
```

**Start Command:**
```bash
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

### Option 2: Using Maven (if Maven is installed globally)

**Build Command:**
```bash
cd backend && mvn clean package -DskipTests
```

**Start Command:**
```bash
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

### Option 3: If Railway Auto-Detects Backend Directory

If Railway automatically sets working directory to `backend/`:

**Build Command:**
```bash
./mvnw clean package -DskipTests
```

**Start Command:**
```bash
java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

---

## Setting Build Commands in Railway

### Via Railway Dashboard:

1. Go to your project in Railway
2. Click on your service
3. Go to **Settings** tab
4. Scroll to **Build & Deploy** section
5. Set:
   - **Build Command**: `cd backend && ./mvnw clean package -DskipTests`
   - **Start Command**: `cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar`

### Via railway.json (Already Configured):

The `backend/railway.json` file is already configured with:
- Build Command: `cd backend && mvn clean package -DskipTests`
- Start Command: `cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar`

**Note**: If Railway doesn't have Maven installed globally, use `./mvnw` instead of `mvn`.

---

## Recommended Configuration

**Use Maven Wrapper (mvnw) - Most Reliable:**

**Build Command:**
```bash
cd backend && ./mvnw clean package -DskipTests
```

**Start Command:**
```bash
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

**Why Maven Wrapper?**
- ✅ Works even if Maven isn't installed globally
- ✅ Uses the correct Maven version
- ✅ More reliable across different environments
- ✅ Included in the project (mvnw file)

---

## Environment Variables Needed

Make sure these are set in Railway:

```bash
# Database (auto-set if using Railway PostgreSQL)
DATABASE_URL=postgresql://...

# Or manually:
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...

# LangChain4j
LANGCHAIN4J_OLLAMA_BASE_URL=http://localhost:11434
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Java Version (Railway should auto-detect Java 21)
JAVA_VERSION=21
```

---

## Troubleshooting

### Build Fails: "mvn: command not found"

**Solution**: Use Maven wrapper instead:
```bash
cd backend && ./mvnw clean package -DskipTests
```

### Build Fails: "mvnw: Permission denied"

**Solution**: Make sure mvnw is executable:
```bash
chmod +x backend/mvnw
```

### Build Fails: "Java version mismatch"

**Solution**: Set Java version explicitly:
```bash
JAVA_VERSION=21
```

### Start Fails: "JAR file not found"

**Solution**: Check build completed successfully and JAR path is correct:
```bash
ls -la backend/target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

