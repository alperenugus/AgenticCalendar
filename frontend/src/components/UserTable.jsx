import { useState, useEffect } from 'react'
import { User, RefreshCw, AlertCircle, Mail, Calendar } from 'lucide-react'
import axios from 'axios'
import toast from 'react-hot-toast'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function UserTable({ refreshTrigger }) {
  const [users, setUsers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isManualRefresh, setIsManualRefresh] = useState(false)
  const [error, setError] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)

  const fetchUsers = async (silent = false) => {
    try {
      if (!silent) {
        setIsLoading(true)
      }
      setError(null)
      const response = await axios.get(`${API_BASE_URL}/users`)
      setUsers(response.data || [])
      setLastUpdated(new Date())
    } catch (error) {
      console.error('Error fetching users:', error)
      setError('Failed to load users')
      if (error.response) {
        toast.error('Failed to fetch users from server')
      } else if (error.request) {
        toast.error('Backend server is not responding')
      }
    } finally {
      setIsLoading(false)
      setIsManualRefresh(false)
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [refreshTrigger])

  // Auto-refresh every 5 seconds (silent - no loading indicator)
  useEffect(() => {
    const interval = setInterval(() => {
      fetchUsers(true) // Silent refresh
    }, 5000)

    return () => clearInterval(interval)
  }, [])

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    } catch (error) {
      return dateString
    }
  }

  if (isLoading && users.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="w-8 h-8 text-blue-400 animate-spin mx-auto mb-2" />
          <p className="text-slate-400">Loading users...</p>
        </div>
      </div>
    )
  }

  if (error && users.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-8 h-8 text-red-400 mx-auto mb-2" />
          <p className="text-slate-400">{error}</p>
          <button
            onClick={() => {
              setIsManualRefresh(true)
              fetchUsers(false)
            }}
            className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Header with refresh button */}
      <div className="px-6 py-3 border-b border-slate-700 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <p className="text-sm text-slate-400">
            {users.length} user{users.length !== 1 ? 's' : ''} registered
          </p>
          {lastUpdated && !isLoading && (
            <span className="text-xs text-slate-500">
              Updated {lastUpdated.toLocaleTimeString()}
            </span>
          )}
        </div>
        <button
          onClick={() => {
            setIsManualRefresh(true)
            fetchUsers(false)
          }}
          disabled={isLoading && isManualRefresh}
          className="flex items-center gap-2 px-3 py-1.5 text-sm bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed text-slate-200 rounded-lg transition-colors"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading && isManualRefresh ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Table Container */}
      <div className="flex-1 overflow-y-auto">
        {users.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <User className="w-12 h-12 text-slate-600 mx-auto mb-4" />
              <p className="text-slate-400">No users registered</p>
              <p className="text-sm text-slate-500 mt-2">
                Create a user using the chat interface
              </p>
            </div>
          </div>
        ) : (
          <div className="p-6">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-slate-700">
                    <th className="text-left py-3 px-4 text-sm font-semibold text-slate-300">
                      ID
                    </th>
                    <th className="text-left py-3 px-4 text-sm font-semibold text-slate-300">
                      Name
                    </th>
                    <th className="text-left py-3 px-4 text-sm font-semibold text-slate-300">
                      Date of Birth
                    </th>
                    <th className="text-left py-3 px-4 text-sm font-semibold text-slate-300">
                      Email
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr
                      key={user.id}
                      className="border-b border-slate-800 hover:bg-slate-800/50 transition-colors"
                    >
                      <td className="py-3 px-4 text-sm text-slate-300">
                        <div className="flex items-center gap-2">
                          <User className="w-4 h-4 text-blue-400" />
                          {user.id}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm text-slate-300">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-slate-200">
                            {user.firstName} {user.lastName}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm text-slate-300">
                        <div className="flex items-center gap-2">
                          <Calendar className="w-4 h-4 text-yellow-400" />
                          {formatDate(user.dob)}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm text-slate-300">
                        <div className="flex items-center gap-2">
                          <Mail className="w-4 h-4 text-green-400" />
                          {user.email}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default UserTable

