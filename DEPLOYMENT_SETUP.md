# Deployment Setup Guide - Agentic Calendar

## 🚀 Quick Setup for Railway

### Backend Environment Variables

Set these in Railway backend service:

```bash
# Google OAuth (Required)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret

# LLM Provider (Required)
LANGCHAIN4J_GROQ_API_KEY=your-groq-api-key
LANGCHAIN4J_PROVIDER=groq

# Database (Auto-set by Railway PostgreSQL)
PGHOST=...
PGPORT=5432
PGDATABASE=railway
PGUSER=postgres
PGPASSWORD=...

# Rate Limiting (Optional - defaults provided)
RATE_LIMIT_CAPACITY=20
RATE_LIMIT_REFILL=10
RATE_LIMIT_PERIOD=60
```

### Frontend Environment Variables

Set these in Railway frontend service:

```bash
# Backend URLs (Required)
VITE_API_BASE_URL=https://your-backend.railway.app/api
VITE_WS_BASE_URL=https://your-backend.railway.app/ws
VITE_BASE_PATH=/
```

### Google OAuth Setup

1. **Create OAuth Credentials:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - APIs & Services → Credentials
   - Create OAuth 2.0 Client ID
   - Application type: Web application

2. **Authorized Redirect URIs:**
   ```
   http://localhost:8080/login/oauth2/code/google
   https://your-backend.railway.app/login/oauth2/code/google
   ```

3. **Copy Credentials:**
   - Client ID → `GOOGLE_CLIENT_ID`
   - Client Secret → `GOOGLE_CLIENT_SECRET`

## ✅ Verification

After deployment:

1. **Test OAuth:**
   - Click "Sign in with Google"
   - Should redirect to Google login
   - After login, should see your name in header

2. **Test Calendar Agent:**
   - "Schedule a meeting tomorrow at 2pm"
   - "What's on my calendar next week?"
   - "Reschedule my meeting to Friday"

3. **Check Events:**
   - Events should appear in right panel
   - Should show title, time, location, status

## 🔧 Troubleshooting

### OAuth Not Working
- Check redirect URI matches exactly
- Verify `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are set
- Check browser console for CORS errors
- Ensure `withCredentials: true` in axios calls

### Events Not Showing
- Check if user is authenticated
- Verify `googleUserId` is being passed
- Check backend logs for errors
- Verify database connection

### Agent Not Responding
- Check `LANGCHAIN4J_GROQ_API_KEY` is set
- Verify rate limits not exceeded
- Check WebSocket connection status
- Review backend logs

