import { useState } from 'react'
import ChatComponent from './components/ChatComponent'
import AppointmentTable from './components/AppointmentTable'
import { Calendar, MessageSquare } from 'lucide-react'

function App() {
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  const handleMessageSent = () => {
    // Trigger refresh of appointment table after message is sent
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
      <main className="container mx-auto px-6 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 h-[calc(100vh-120px)]">
          {/* Left Panel - Chat Interface */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl">
            <div className="flex items-center gap-2 px-6 py-4 border-b border-slate-700">
              <MessageSquare className="w-5 h-5 text-blue-400" />
              <h2 className="text-lg font-semibold text-white">Chat with Agent</h2>
            </div>
            <ChatComponent onMessageSent={handleMessageSent} />
          </div>

          {/* Right Panel - Appointment Monitor */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl">
            <div className="flex items-center gap-2 px-6 py-4 border-b border-slate-700">
              <Calendar className="w-5 h-5 text-green-400" />
              <h2 className="text-lg font-semibold text-white">Live Appointment Monitor</h2>
            </div>
            <AppointmentTable refreshTrigger={refreshTrigger} />
          </div>
        </div>
      </main>
    </div>
  )
}

export default App

