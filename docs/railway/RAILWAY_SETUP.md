# Railway Setup Instructions

## Quick Fix: Manual Configuration in Railway Dashboard

If Railway shows "No start command was found", configure it manually:

### Step 1: Open Railway Dashboard

1. Go to [railway.app](https://railway.app)
2. Select your project
3. Click on your service (the backend service)

### Step 2: Configure Build & Deploy Settings

1. Click on the **Settings** tab
2. Scroll down to **Build & Deploy** section

### Step 3: Set Build Command

In the **Build Command** field, enter:
```bash
cd backend && ./mvnw clean package -DskipTests
```

### Step 4: Set Start Command

In the **Start Command** field, enter:
```bash
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

### Step 5: Set Root Directory (Optional)

If Railway is confused about the project structure:
- Set **Root Directory** to: `.` (root of repository)
- Or leave it empty (Railway should auto-detect)

### Step 6: Save and Redeploy

1. Click **Save** or **Deploy**
2. Railway will rebuild with the new commands

---

## Alternative: Using railway.json

The `railway.json` file at the root should work, but if it doesn't:

1. **Verify file location**: Should be at root: `/railway.json`
2. **Verify file format**: Should be valid JSON
3. **Redeploy**: Railway should pick it up on next deploy

---

## Troubleshooting

### Issue: "mvnw: command not found"

**Solution**: Use full path or make sure mvnw is executable:
```bash
cd backend && chmod +x ./mvnw && ./mvnw clean package -DskipTests
```

### Issue: "JAR file not found"

**Solution**: Check the build completed. The JAR should be at:
```
backend/target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

### Issue: "Java version mismatch"

**Solution**: Set Java version in Railway environment variables:
```
JAVA_VERSION=21
```

### Issue: Railway still can't find start command

**Solution**: 
1. Go to Settings → Build & Deploy
2. Manually enter the commands (don't rely on railway.json)
3. Make sure there are no extra spaces or quotes
4. Save and redeploy

---

## Complete Railway Configuration

### Environment Variables

Set these in Railway → Variables:

```bash
# Database (auto-set if using Railway PostgreSQL)
DATABASE_URL=postgresql://...

# Or manually set:
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...

# LangChain4j - Groq is the default (FREE tier: 100,000 tokens/day)
# Get API key: https://console.groq.com/keys

# Groq (FREE - Default! 100,000 tokens/day)
LANGCHAIN4J_PROVIDER=groq
LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key-here
LANGCHAIN4J_GROQ_MODEL=llama-3.1-8b-instant  # Smaller model to avoid rate limits
LANGCHAIN4J_GROQ_TEMPERATURE=0.7

# Alternative: OpenAI (Paid - only if you need it)
# LANGCHAIN4J_PROVIDER=openai
# LANGCHAIN4J_OPENAI_API_KEY=sk-your-openai-api-key-here
# LANGCHAIN4J_OPENAI_MODEL=gpt-4o-mini
# LANGCHAIN4J_OPENAI_TEMPERATURE=0.7

# Note: Ollama is for local development only and won't work on Railway

# Java
JAVA_VERSION=21
```

### Build & Deploy Settings

**Build Command:**
```bash
cd backend && ./mvnw clean package -DskipTests
```

**Start Command:**
```bash
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

**Root Directory:**
```
. (or leave empty)
```

---

## Verification

After setting up, verify:

1. **Build succeeds**: Check Railway logs for successful build
2. **JAR created**: Should see "BUILD SUCCESS" in logs
3. **App starts**: Should see Spring Boot banner in logs
4. **Health check**: Visit `https://your-app.railway.app/api/users/count`

---

## Quick Copy-Paste Commands

**Build Command:**
```
cd backend && ./mvnw clean package -DskipTests
```

**Start Command:**
```
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar
```

Copy these directly into Railway dashboard!

