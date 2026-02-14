import { useState, useEffect } from 'react'
import ChatComponent from './components/ChatComponent'
import CalendarView from './components/CalendarView'
import LoginScreen from './components/LoginScreen'
import { Calendar, MessageSquare, LogOut, User } from 'lucide-react'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function App() {
  const [refreshTrigger, setRefreshTrigger] = useState(0)
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    checkAuth()
  }, [])

  const checkAuth = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/auth/user`, { withCredentials: true })
      if (response.data.authenticated) {
        setUser(response.data)
      }
    } catch (error) {
      console.error('Auth check failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleLogout = async () => {
    try {
      await axios.post(`${API_BASE_URL.replace('/api', '')}/logout`, {}, { withCredentials: true })
      setUser(null)
      window.location.reload()
    } catch (error) {
      console.error('Logout failed:', error)
      setUser(null)
      window.location.reload()
    }
  }

  const handleMessageSent = () => {
    // Trigger refresh of calendar after message is sent
    setRefreshTrigger(prev => prev + 1)
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-900 text-slate-100 flex items-center justify-center">
        <div className="text-slate-400">Loading...</div>
      </div>
    )
  }

  // Show login screen if user is not authenticated
  if (!user) {
    return <LoginScreen />
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100">
      {/* Header */}
      <header className="border-b border-slate-700 bg-slate-800/50 backdrop-blur-sm">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Calendar className="w-8 h-8 text-blue-400" />
              <div>
                <h1 className="text-2xl font-bold text-white">Agentic Calendar</h1>
                <p className="text-sm text-slate-400">AI-Powered Calendar Management</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 px-3 py-1 bg-blue-900/30 border border-blue-700/50 rounded">
                {user.picture && (
                  <img src={user.picture} alt={user.name} className="w-6 h-6 rounded-full" />
                )}
                <span className="text-sm text-blue-300 font-medium">{user.name || user.email}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors"
              >
                <LogOut className="w-4 h-4" />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-6 py-6 max-w-7xl">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 max-h-[calc(100vh-140px)]">
          {/* Left Panel - Chat Interface */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl min-h-0 max-h-[calc(100vh-140px)] overflow-hidden">
            <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-700 flex-shrink-0">
              <MessageSquare className="w-4 h-4 text-blue-400" />
              <h2 className="text-base font-semibold text-white">Chat with Calendar Agent</h2>
            </div>
            <div className="flex-1 min-h-0">
              <ChatComponent onMessageSent={handleMessageSent} user={user} />
            </div>
          </div>

          {/* Right Panel - Calendar View */}
          <div className="flex flex-col bg-slate-800/50 rounded-lg border border-slate-700 shadow-xl min-h-0 max-h-[calc(100vh-140px)] overflow-hidden">
            <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-700 flex-shrink-0">
              <Calendar className="w-4 h-4 text-green-400" />
              <h2 className="text-base font-semibold text-white">Your Calendar</h2>
            </div>
            <CalendarView refreshTrigger={refreshTrigger} user={user} />
          </div>
        </div>
      </main>
    </div>
  )
}

export default App

