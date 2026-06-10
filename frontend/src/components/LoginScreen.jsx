import { useState } from 'react'
import { Calendar, LogIn, Mail, Lock, User as UserIcon, Loader2 } from 'lucide-react'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function LoginScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login') // 'login' | 'register'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleGoogleLogin = () => {
    window.location.href = `${API_BASE_URL.replace('/api', '')}/oauth2/authorization/google`
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!email.trim() || !password) {
      setError('Please enter your email and password.')
      return
    }
    if (mode === 'register' && password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    setIsSubmitting(true)
    try {
      const endpoint = mode === 'register' ? 'register' : 'login'
      const payload = mode === 'register'
        ? { email: email.trim(), password, name: name.trim() }
        : { email: email.trim(), password }

      await axios.post(`${API_BASE_URL}/auth/${endpoint}`, payload, { withCredentials: true })

      // Session cookie is now set; let the app re-check auth.
      if (onAuthenticated) {
        await onAuthenticated()
      } else {
        window.location.reload()
      }
    } catch (err) {
      const message = err.response?.data?.error
        || (mode === 'register'
          ? 'Could not create your account. Please try again.'
          : 'Invalid email or password.')
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const switchMode = (next) => {
    setMode(next)
    setError('')
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
          {/* Mode Tabs */}
          <div className="flex bg-slate-900/60 rounded-lg p-1 mb-6">
            <button
              type="button"
              onClick={() => switchMode('login')}
              className={`flex-1 py-2 rounded-md text-sm font-medium transition-colors ${
                mode === 'login' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => switchMode('register')}
              className={`flex-1 py-2 rounded-md text-sm font-medium transition-colors ${
                mode === 'register' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              Create Account
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'register' && (
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1">Name</label>
                <div className="relative">
                  <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Your name"
                    autoComplete="name"
                    className="w-full pl-10 pr-3 py-2.5 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1">Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  autoComplete="email"
                  className="w-full pl-10 pr-3 py-2.5 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder={mode === 'register' ? 'At least 8 characters' : 'Your password'}
                  autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                  className="w-full pl-10 pr-3 py-2.5 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {error && (
              <div className="text-sm text-red-300 bg-red-900/30 border border-red-700/50 rounded-lg px-3 py-2">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 disabled:cursor-not-allowed text-white rounded-lg font-semibold transition-colors"
            >
              {isSubmitting ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <LogIn className="w-5 h-5" />
              )}
              <span>{mode === 'register' ? 'Create Account' : 'Sign In'}</span>
            </button>
          </form>

          {/* Divider */}
          <div className="flex items-center gap-3 my-6">
            <div className="flex-1 h-px bg-slate-700" />
            <span className="text-xs text-slate-500">OR</span>
            <div className="flex-1 h-px bg-slate-700" />
          </div>

          {/* Google Login */}
          <button
            onClick={handleGoogleLogin}
            className="w-full flex items-center justify-center gap-3 px-6 py-3 bg-white hover:bg-gray-50 text-gray-900 rounded-lg font-semibold transition-all shadow-lg hover:shadow-xl"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            <span>Sign in with Google</span>
          </button>

          <p className="mt-6 text-xs text-center text-slate-500">
            By signing in, you agree to our Terms of Service and Privacy Policy
          </p>
        </div>

        <div className="mt-8 text-center">
          <p className="text-slate-500 text-sm">Powered by AI • Secure Authentication</p>
        </div>
      </div>
    </div>
  )
}

export default LoginScreen
