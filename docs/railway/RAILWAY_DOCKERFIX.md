# Railway Docker Start Command Fix

## Problem

Railway is trying to execute `cd` as a command, which fails because `cd` is a shell builtin, not an executable.

## Solution

**IMPORTANT**: When using a Dockerfile, you must **remove any start command** from Railway dashboard settings!

### Steps to Fix:

1. **Go to Railway Dashboard** → Your Backend Service → **Settings** tab
2. **Find "Start Command"** field
3. **DELETE/EMPTY the start command** - leave it completely empty
4. **Save** the settings
5. **Redeploy** the service

### Why This Happens

When Railway detects a Dockerfile, it should use the `CMD` or `ENTRYPOINT` from the Dockerfile. However, if you have a start command set in the dashboard settings, Railway will try to use that instead, which can cause conflicts.

### Correct Configuration

- ✅ **Dockerfile CMD**: `java -jar app.jar` (already set correctly)
- ✅ **Railway Start Command**: **EMPTY** (let Dockerfile handle it)
- ✅ **application.yml**: `server.port: ${PORT:8080}` (reads Railway's PORT)

### Verify

After removing the start command:
1. Railway will use the Dockerfile's CMD
2. Container should start successfully
3. Check logs to see Spring Boot starting

## Alternative: If You Must Use Start Command

If Railway still requires a start command, use this (but Dockerfile should handle it):

```bash
java -jar app.jar
```

**NOT**:
```bash
cd backend && java -jar target/appointmentscheduler-0.0.1-SNAPSHOT.jar  # ❌ WRONG
```

The JAR is already in `/app/app.jar` in the container, so no `cd` is needed.
