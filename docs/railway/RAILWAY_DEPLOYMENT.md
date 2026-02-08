# Railway Deployment Guide

## Overview

This guide covers deploying both frontend and backend to Railway using Groq (free LLM).

## Prerequisites

1. Railway account: https://railway.app
2. Groq API key: https://console.groq.com/keys (free, no credit card needed)

## Step 1: Deploy Backend

### 1.1 Create Backend Service

1. Go to Railway dashboard
2. **New Project** → **Deploy from GitHub repo**
3. Select your repository
4. Railway will auto-detect the Dockerfile

### 1.2 Add PostgreSQL Database

1. In your Railway project, click **New** → **Database** → **PostgreSQL**
2. Railway will automatically set `DATABASE_URL` environment variable

### 1.3 Connect PostgreSQL to Backend Service

**IMPORTANT**: After creating the PostgreSQL database, you need to connect it to your backend service:

1. Go to your **PostgreSQL service** in Railway
2. Click **Variables** tab
3. You'll see these variables (Railway sets them automatically):
   - `PGHOST`
   - `PGPORT`
   - `PGDATABASE`
   - `PGUSER`
   - `PGPASSWORD`
   - `DATABASE_URL`

4. Go to your **Backend service** → **Variables** tab
5. Click **"Add Variable from Service"** or **"Reference Variable"**
6. Select your **PostgreSQL service**
7. Add these references:
   - `PGHOST` → Reference from PostgreSQL service
   - `PGPORT` → Reference from PostgreSQL service
   - `PGDATABASE` → Reference from PostgreSQL service
   - `PGUSER` → Reference from PostgreSQL service
   - `PGPASSWORD` → Reference from PostgreSQL service

**OR** manually add them (copy values from PostgreSQL service):

```bash
# Database (copy these values from PostgreSQL service Variables tab)
PGHOST=your-postgres-host.railway.app
PGPORT=5432
PGDATABASE=railway
PGUSER=postgres
PGPASSWORD=your-password-here

# Groq LLM (FREE tier: 14,400 requests/day)
LANGCHAIN4J_PROVIDER=groq
LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key-here
LANGCHAIN4J_GROQ_MODEL=llama-3.3-70b-versatile
LANGCHAIN4J_GROQ_TEMPERATURE=0.7
```

### 1.4 Get Backend URL

1. Go to backend service → **Settings** → **Generate Domain**
2. Copy the URL (e.g., `https://appointmentscheduler-backend-production.up.railway.app`)

## Step 2: Deploy Frontend

### 2.1 Create Frontend Service

1. In the same Railway project, click **New** → **GitHub Repo**
2. Select the same repository
3. **Settings** → **Root Directory**: Set to `frontend`

### 2.2 Configure Build Settings

Railway should auto-detect Node.js, but verify:

**Build Command:**
```bash
npm ci && npm run build
```

**Start Command:**
```bash
npm start
```

### 2.3 Set Environment Variables

Go to frontend service → **Variables** tab, add:

```bash
# Backend API URL (use the backend URL from Step 1.4)
VITE_API_BASE_URL=https://your-backend-service.railway.app/api
VITE_WS_BASE_URL=https://your-backend-service.railway.app/ws

# Base path (empty for Railway)
VITE_BASE_PATH=/
```

### 2.4 Get Frontend URL

1. Go to frontend service → **Settings** → **Generate Domain**
2. Copy the URL (e.g., `https://appointmentscheduler-frontend-production.up.railway.app`)

## Step 3: Update Backend CORS (if needed)

The backend is already configured to allow all origins (`allowedOriginPatterns("*")`), so it should work with any Railway frontend domain.

If you want to restrict it, update `backend/src/main/java/com/agent/appointmentscheduler/config/WebConfig.java`:

```java
.allowedOrigins("https://your-frontend-service.railway.app")
```

## Verification

1. **Backend**: Visit `https://your-backend.railway.app/api/users/count` - should return user count
2. **Frontend**: Visit your frontend URL - should load the chat interface
3. **Test**: Send a message in the chat - should get a response from Groq LLM

## Groq Free Tier Limits

- **14,400 requests/day** (600 requests/hour)
- **30 requests/minute** rate limit
- **No credit card required**
- **No expiration** (as long as within limits)

## Troubleshooting

### Backend Build Fails

- Check Railway logs for Maven errors
- Verify Dockerfile is at repository root
- Ensure Java 21 is available

### Frontend Can't Connect to Backend

- Verify `VITE_API_BASE_URL` is set correctly (use `https://`, not `http://`)
- Verify `VITE_WS_BASE_URL` uses `wss://` (secure WebSocket)
- Check backend CORS configuration

### LLM Not Working

- Verify `LANGCHAIN4J_PROVIDER=groq` is set
- Check `LANGCHAIN4J_GROQ_API_KEY` is correct (starts with `gsk_`)
- Check Railway logs for LLM errors

## Cost

- **Railway**: $5/month free credit (usually enough for small apps)
- **Groq**: FREE (14,400 requests/day)
- **PostgreSQL**: Included with Railway

**Total: FREE for development/testing!**

