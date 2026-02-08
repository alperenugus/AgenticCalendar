# Appointment Scheduler Frontend

React frontend for the Agentic Appointment Scheduler application.

## Features

- 🎨 Modern dark-mode UI with Tailwind CSS
- 💬 Real-time chat interface with AI agent
- 🤔 "Thinking..." indicator during agent processing
- 📊 Live appointment monitor with auto-refresh
- 🔔 Toast notifications for errors and success
- 📱 Responsive design

## Setup

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will run on `http://localhost:5173`

### Build for Production

```bash
npm run build
```

## Configuration

The frontend is configured to connect to the backend at `http://localhost:8080`. 

To change the API URL, update `API_BASE_URL` in:
- `src/components/ChatComponent.jsx`
- `src/components/AppointmentTable.jsx`

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── ChatComponent.jsx      # Chat interface with agent
│   │   └── AppointmentTable.jsx   # Live appointment monitor
│   ├── App.jsx                     # Main app component
│   ├── main.jsx                    # Entry point
│   └── index.css                   # Global styles
├── index.html
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## Features in Detail

### Chat Component

- Sends messages to `/api/agent/chat`
- Shows "Thinking..." indicator while processing
- Displays user and assistant messages
- Auto-scrolls to latest message
- Error handling with toast notifications

### Appointment Table

- Fetches appointments from `/api/appointments`
- Auto-refreshes every 5 seconds
- Manual refresh button
- Shows appointment ID, user ID, date/time, and description
- Empty state when no appointments exist

## Development

The frontend uses:
- **Vite** for fast development and building
- **React 18** for UI
- **Tailwind CSS** for styling
- **Axios** for API calls
- **Lucide React** for icons
- **React Hot Toast** for notifications

