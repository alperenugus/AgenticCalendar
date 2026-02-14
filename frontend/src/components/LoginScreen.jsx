import { Calendar, LogIn } from 'lucide-react'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function LoginScreen({ onLogin }) {
  const handleLogin = () => {
    window.location.href = `${API_BASE_URL.replace('/api', '')}/oauth2/authorization/google`
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full">
        {/* Logo and Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-blue-600 rounded-2xl mb-4 shadow-lg">
            <Calendar className="w-12 h-12 text-white" />
          </div>
          <h1 className="text-4xl font-bold text-white mb-2">Agentic Calendar</h1>
          <p className="text-slate-400 text-lg">AI-Powered Calendar Management</p>
        </div>

        {/* Login Card */}
        <div className="bg-slate-800/50 backdrop-blur-sm border border-slate-700 rounded-2xl p-8 shadow-2xl">
          <div className="space-y-6">
            <div>
              <h2 className="text-2xl font-semibold text-white mb-2">Welcome</h2>
              <p className="text-slate-400">
                Sign in with your Google account to access your AI-powered calendar assistant.
              </p>
            </div>

            <div className="space-y-4">
              <div className="flex items-start gap-3 p-4 bg-slate-700/50 rounded-lg border border-slate-600">
                <div className="flex-shrink-0 w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center mt-0.5">
                  <span className="text-white text-xs font-bold">1</span>
                </div>
                <div>
                  <p className="text-white font-medium text-sm">Schedule Events</p>
                  <p className="text-slate-400 text-xs mt-1">Create meetings and appointments with natural language</p>
                </div>
              </div>

              <div className="flex items-start gap-3 p-4 bg-slate-700/50 rounded-lg border border-slate-600">
                <div className="flex-shrink-0 w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center mt-0.5">
                  <span className="text-white text-xs font-bold">2</span>
                </div>
                <div>
                  <p className="text-white font-medium text-sm">Smart Calendar</p>
                  <p className="text-slate-400 text-xs mt-1">View your events in a beautiful calendar interface</p>
                </div>
              </div>

              <div className="flex items-start gap-3 p-4 bg-slate-700/50 rounded-lg border border-slate-600">
                <div className="flex-shrink-0 w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center mt-0.5">
                  <span className="text-white text-xs font-bold">3</span>
                </div>
                <div>
                  <p className="text-white font-medium text-sm">AI Assistant</p>
                  <p className="text-slate-400 text-xs mt-1">Get intelligent help with scheduling and time management</p>
                </div>
              </div>
            </div>

            <button
              onClick={handleLogin}
              className="w-full flex items-center justify-center gap-3 px-6 py-4 bg-white hover:bg-gray-50 text-gray-900 rounded-lg font-semibold transition-all shadow-lg hover:shadow-xl transform hover:scale-[1.02]"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              <LogIn className="w-5 h-5" />
              <span>Sign in with Google</span>
            </button>

            <p className="text-xs text-center text-slate-500">
              By signing in, you agree to our Terms of Service and Privacy Policy
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="mt-8 text-center">
          <p className="text-slate-500 text-sm">
            Powered by AI • Secure OAuth Authentication
          </p>
        </div>
      </div>
    </div>
  )
}

export default LoginScreen

