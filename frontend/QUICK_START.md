# Quick Start Guide

## Prerequisites

1. **Backend must be running** on `http://localhost:8080`
2. **Node.js 18+** installed
3. **npm** or **yarn** package manager

## Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:5173`

## First Time Setup

1. **Start the backend**:
   ```bash
   # In the root directory
   mvn spring-boot:run
   ```

2. **Start the frontend**:
   ```bash
   # In the frontend directory
   npm run dev
   ```

3. **Open your browser** to `http://localhost:5173`

## Testing the Application

1. **Send a message** in the chat interface:
   ```
   Book an appointment for Alperen Ugus born on 1990-01-01 for December 25, 2024 at 2 PM for a dental checkup
   ```

2. **Watch the "Thinking..." indicator** while the agent processes your request

3. **See the appointment appear** in the Live Appointment Monitor on the right

4. **Try other commands**:
   - "Cancel appointment for Alperen Ugus born on 1990-01-01"
   - "Reschedule Alperen Ugus appointment to January 1, 2025 at 3 PM"

## Troubleshooting

### Frontend can't connect to backend

- Make sure the backend is running on port 8080
- Check browser console for CORS errors
- Verify `WebConfig.java` has CORS enabled

### "Thinking..." indicator never stops

- Check backend logs for errors
- Verify Ollama is running and the model is loaded
- Check network tab in browser DevTools

### Appointments not showing

- Verify backend is running
- Check `/api/appointments` endpoint directly
- Look for errors in browser console

## Development Tips

- **Hot Reload**: Changes to React components will auto-reload
- **Browser DevTools**: Use React DevTools extension for debugging
- **Network Tab**: Monitor API calls in browser DevTools
- **Console**: Check for JavaScript errors

