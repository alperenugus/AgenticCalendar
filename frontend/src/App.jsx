import { useState } from 'react'
import ChatComponent from './components/ChatComponent'
import AppointmentTable from './components/AppointmentTable'
import UserTable from './components/UserTable'
import { Calendar, MessageSquare, Users } from 'lucide-react'

function App() {
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  const handleMessageSent = () => {
    // Trigger refresh of both tables after message is sent
    setRefreshTrigger(prev => prev + 1)
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100">
      {/* Header */}
      <header className="border-b border-slate-700 bg-slate-800/50 backdrop-blur-sm">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center gap-3">
            <Calendar className="w-8 h-8 text-blue-400" />
            <div>
              <h1 className="text-2xl font-bold text-white">Agentic Appointment Scheduler</h1>
              <p className="text-sm text-slate-400">AI-Powered Appointment Management System</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-6 py-6 max-w-7xl">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 max-h-[calc(100vh-140px)]">
          {/* Left Panel - Chat Interface */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl min-h-0 max-h-[calc(100vh-140px)] overflow-hidden">
            <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-700 flex-shrink-0">
              <MessageSquare className="w-4 h-4 text-blue-400" />
              <h2 className="text-base font-semibold text-white">Chat with Agent</h2>
            </div>
            <div className="flex-1 min-h-0">
              <ChatComponent onMessageSent={handleMessageSent} />
            </div>
          </div>

          {/* Middle Panel - Appointment Monitor */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl min-h-0 max-h-[calc(100vh-140px)]">
            <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-700 flex-shrink-0">
              <Calendar className="w-4 h-4 text-green-400" />
              <h2 className="text-base font-semibold text-white">Live Appointment Monitor</h2>
            </div>
            <AppointmentTable refreshTrigger={refreshTrigger} />
          </div>

          {/* Right Panel - Registered Users */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl min-h-0 max-h-[calc(100vh-140px)]">
            <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-700 flex-shrink-0">
              <Users className="w-4 h-4 text-purple-400" />
              <h2 className="text-base font-semibold text-white">Registered Users</h2>
            </div>
            <UserTable refreshTrigger={refreshTrigger} />
          </div>
        </div>
      </main>
    </div>
  )
}

export default App

