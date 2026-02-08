# Railway WebSocket Connection Fix

## Problem

WebSocket connection fails with error:
```
WebSocket connection to 'wss://frontend-url/backend-url/ws/...' failed
```

This happens when `VITE_WS_BASE_URL` environment variable is not set correctly or not available at build time.

## Solution

### Step 1: Verify Environment Variables

Go to your **Frontend service** in Railway → **Variables** tab, make sure you have:

```bash
VITE_API_BASE_URL=https://your-backend.railway.app/api
VITE_WS_BASE_URL=https://your-backend.railway.app/ws
VITE_BASE_PATH=/
```

**Important**:
- Use `https://` (not `wss://`) for WebSocket URL
- Use absolute URLs (full URL with domain)
- Replace `your-backend.railway.app` with your actual backend domain

### Step 2: Rebuild Frontend

**IMPORTANT**: Vite environment variables are embedded at **build time**, not runtime!

1. After setting/updating environment variables in Railway
2. Go to **Frontend service** → **Deployments**
3. Click **Redeploy** to rebuild with new environment variables

### Step 3: Verify in Browser Console

After redeploying, open your frontend URL and check the browser console. You should see:

```
WebSocket URL: https://your-backend.railway.app/ws
API URL: https://your-backend.railway.app/api
```

If you see `http://localhost:8080/ws`, the environment variable is not being read correctly.

## Common Issues

### Issue 1: Environment Variable Not Set

**Symptom**: Console shows `http://localhost:8080/ws`

**Solution**: 
- Check Railway Variables tab
- Make sure `VITE_WS_BASE_URL` is set
- Make sure it's an absolute URL (starts with `https://`)

### Issue 2: Wrong URL Format

**Symptom**: URL shows frontend domain instead of backend domain

**Solution**:
- Use absolute URL: `https://your-backend.railway.app/ws`
- NOT relative: `/ws` or `ws`
- NOT frontend URL: `https://your-frontend.railway.app/ws`

### Issue 3: Not Rebuilt After Setting Variables

**Symptom**: Variables are set but still using old values

**Solution**:
- Vite embeds env vars at build time
- You MUST redeploy after setting/changing variables
- Go to Deployments → Redeploy

## Example Configuration

**Backend Service URL**: `https://agenticappointmentschedulerbackend-production.up.railway.app`

**Frontend Variables**:
```bash
VITE_API_BASE_URL=https://agenticappointmentschedulerbackend-production.up.railway.app/api
VITE_WS_BASE_URL=https://agenticappointmentschedulerbackend-production.up.railway.app/ws
VITE_BASE_PATH=/
```

## Verification Checklist

- [ ] `VITE_WS_BASE_URL` is set in Railway Variables
- [ ] URL is absolute (starts with `https://`)
- [ ] URL points to backend domain (not frontend)
- [ ] URL ends with `/ws`
- [ ] Frontend has been redeployed after setting variables
- [ ] Browser console shows correct WebSocket URL
- [ ] WebSocket connection succeeds

## Debugging

Open browser console (F12) and check:
1. Look for log messages: `WebSocket URL: ...` and `API URL: ...`
2. If they show `localhost:8080`, env vars are not set correctly
3. If they show frontend URL, the variable value is wrong
4. If they show correct backend URL but connection fails, check backend CORS/WebSocket config

