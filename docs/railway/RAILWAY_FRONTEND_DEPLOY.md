# Deploy Frontend to Railway - Quick Guide

## Step-by-Step Instructions

### Step 1: Create Frontend Service

1. Go to your **Railway Dashboard**
2. In the **same project** where your backend is deployed, click **New** → **GitHub Repo**
3. Select your repository (same one as backend)
4. Railway will start deploying

### Step 2: Configure Root Directory

1. Go to your **Frontend service** → **Settings** tab
2. Find **"Root Directory"** field
3. Set it to: `frontend`
4. **Save**

### Step 3: Verify Build Settings

Railway should auto-detect Node.js, but verify in **Settings**:

**Build Command:**
```bash
npm ci && npm run build
```

**Start Command:**
```bash
npm start
```

(These should be auto-detected, but you can set them manually if needed)

### Step 4: Get Backend URL

1. Go to your **Backend service** in Railway
2. Click **Settings** → **Generate Domain** (if not already generated)
3. Copy the URL (e.g., `https://appointmentscheduler-backend-production.up.railway.app`)

### Step 5: Set Environment Variables

Go to **Frontend service** → **Variables** tab, add:

```bash
# Backend API URL (replace with your actual backend URL)
VITE_API_BASE_URL=https://your-backend-service.railway.app/api

# WebSocket URL (replace with your actual backend URL)
# Note: Use https:// (not wss://) - SockJS will handle WebSocket upgrade automatically
VITE_WS_BASE_URL=https://your-backend-service.railway.app/ws

# Base path (leave empty for Railway root)
VITE_BASE_PATH=/
```

**Important**: 
- Use `https://` (not `http://`) for API URL
- Use `https://` (not `wss://`) for WebSocket URL - SockJS handles the WebSocket upgrade
- The code will automatically convert `wss://` to `https://` if you set it incorrectly
- Replace `your-backend-service.railway.app` with your actual backend URL

### Step 6: Deploy

1. Railway will automatically build and deploy after you save the variables
2. Or manually trigger: **Deployments** → **Redeploy**

### Step 7: Get Frontend URL

1. Go to **Frontend service** → **Settings** → **Generate Domain**
2. Copy the URL (e.g., `https://appointmentscheduler-frontend-production.up.railway.app`)
3. Visit the URL in your browser!

## Verification Checklist

- [ ] Frontend service created in Railway
- [ ] Root directory set to `frontend`
- [ ] Build command: `npm ci && npm run build`
- [ ] Start command: `npm start`
- [ ] `VITE_API_BASE_URL` set to backend URL (with `https://`)
- [ ] `VITE_WS_BASE_URL` set to backend URL (with `wss://`)
- [ ] `VITE_BASE_PATH` set to `/`
- [ ] Frontend deployed successfully
- [ ] Frontend URL accessible in browser
- [ ] Chat interface loads
- [ ] Can send messages and get responses

## Troubleshooting

### Build Fails

**Error**: `Cannot find module`
- **Solution**: Make sure Root Directory is set to `frontend`
- Check that `package.json` exists in the `frontend` directory

**Error**: `Environment variable not found`
- **Solution**: Set all `VITE_*` variables in Railway dashboard
- Rebuild after adding variables

### Frontend Can't Connect to Backend

**Error**: CORS error in browser console
- **Solution**: Backend CORS is already configured to allow all origins
- Verify backend URL in `VITE_API_BASE_URL` is correct

**Error**: WebSocket connection failed
- **Solution**: 
  - Make sure `VITE_WS_BASE_URL` uses `https://` (not `wss://` - SockJS handles upgrade)
  - The code will auto-convert `wss://` to `https://`, but it's better to use `https://` directly
  - Check backend WebSocket config allows your frontend domain
  - Verify backend URL is correct

**Error**: 404 on API calls
- **Solution**: 
  - Check `VITE_API_BASE_URL` ends with `/api`
  - Verify backend is running and accessible
  - Test backend URL directly: `https://your-backend.railway.app/api/users/count`

### Port Issues

**Error**: Port already in use
- **Solution**: Railway sets `$PORT` automatically
- The `npm start` script uses `$PORT` correctly

## Example Environment Variables

Replace these with your actual backend URL:

```bash
VITE_API_BASE_URL=https://appointmentscheduler-backend-production.up.railway.app/api
VITE_WS_BASE_URL=https://appointmentscheduler-backend-production.up.railway.app/ws
VITE_BASE_PATH=/
```

## After Deployment

Once deployed:
1. Visit your frontend URL
2. You should see the chat interface
3. Try sending a message - it should connect to your backend
4. The backend will use Groq LLM to respond

## Cost

- **Railway Frontend**: Uses your Railway credits (usually free tier is enough)
- **Total**: FREE for development/testing!

Your full stack is now deployed! 🎉

