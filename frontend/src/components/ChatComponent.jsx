import { useState, useRef, useEffect } from 'react'
import { Send, Bot, User, Loader2 } from 'lucide-react'
import axios from 'axios'
import toast from 'react-hot-toast'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
// SockJS expects http:// or https://, not ws:// or wss://
// Convert wss:// to https:// and ws:// to http://
// IMPORTANT: If page is loaded over HTTPS, WebSocket must also use HTTPS
const getWebSocketUrl = () => {
  const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080/ws'
  
  // If empty or undefined, use default
  if (!wsUrl || wsUrl.trim() === '') {
    console.warn('VITE_WS_BASE_URL is not set, using default')
    return 'http://localhost:8080/ws'
  }
  
  // Ensure it's an absolute URL (starts with http:// or https://)
  if (!wsUrl.startsWith('http://') && !wsUrl.startsWith('https://') && 
      !wsUrl.startsWith('ws://') && !wsUrl.startsWith('wss://')) {
    console.error('VITE_WS_BASE_URL must be an absolute URL starting with http://, https://, ws://, or wss://')
    return 'http://localhost:8080/ws'
  }
  
  // If it starts with wss://, convert to https://
  if (wsUrl.startsWith('wss://')) {
    return wsUrl.replace('wss://', 'https://')
  }
  // If it starts with ws://, convert to https:// (not http://) if page is HTTPS
  if (wsUrl.startsWith('ws://')) {
    // If page is loaded over HTTPS, force HTTPS for WebSocket (browser security requirement)
    if (window.location.protocol === 'https:') {
      console.warn('Page is HTTPS, converting ws:// to https:// for WebSocket')
      return wsUrl.replace('ws://', 'https://')
    }
    return wsUrl.replace('ws://', 'http://')
  }
  
  // If page is loaded over HTTPS but WebSocket URL is HTTP, convert to HTTPS
  if (window.location.protocol === 'https:' && wsUrl.startsWith('http://')) {
    console.warn('Page is HTTPS, converting http:// to https:// for WebSocket (browser security requirement)')
    return wsUrl.replace('http://', 'https://')
  }
  
  // Otherwise, use as-is (should be http:// or https://)
  return wsUrl
}
const WS_BASE_URL = getWebSocketUrl()

// Log for debugging (remove in production if needed)
console.log('WebSocket URL:', WS_BASE_URL)
console.log('API URL:', API_BASE_URL)
console.log('Page protocol:', window.location.protocol)

function ChatComponent({ onMessageSent }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isConnected, setIsConnected] = useState(false)
  const [isLoadingHistory, setIsLoadingHistory] = useState(true)
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)
  const stompClientRef = useRef(null)
  
  // Get or create session ID from localStorage
  const getOrCreateSessionId = () => {
    const stored = localStorage.getItem('chatSessionId')
    if (stored) {
      return stored
    }
    const newSessionId = `session-${Date.now()}`
    localStorage.setItem('chatSessionId', newSessionId)
    return newSessionId
  }
  
  const sessionIdRef = useRef(getOrCreateSessionId())

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // Load conversation history on mount
  useEffect(() => {
    const loadHistory = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/agent/history`, {
          headers: {
            'X-Session-Id': sessionIdRef.current,
          },
        })
        
        if (response.data && response.data.length > 0) {
          // Convert ConversationMessage objects to frontend message format
          const historyMessages = response.data.map((msg, index) => {
            const baseMessage = {
              id: index + 1,
              type: msg.role === 'USER' ? 'user' : 'assistant',
              content: msg.content || '',
              thinking: msg.role === 'THINKING',
              toolCalls: null,
            }
            
            // Handle thinking messages
            if (msg.role === 'THINKING') {
              baseMessage.type = 'thinking'
            }
            
            // Handle tool calls if present
            if (msg.toolCall) {
              baseMessage.toolCalls = {
                name: 'tool', // We don't have tool name in ConversationMessage
                args: msg.toolCall,
                result: msg.toolResult,
              }
            }
            
            return baseMessage
          })
          
          // Set messages from history
          setMessages(historyMessages)
        } else {
          // No history - show welcome message
          setMessages([
            {
              id: 1,
              type: 'assistant',
              content: "Hello! I'm your AI appointment scheduling assistant. This is a **demo application** running on Groq's free tier (100,000 tokens/day limit).\n\nI can help you:\n✅ Create, view, update, or cancel appointments (one at a time)\n✅ Create new user accounts\n✅ Update user information\n\nHow can I assist you today?",
              thinking: false,
              toolCalls: null,
            },
          ])
        }
      } catch (error) {
        console.error('Error loading conversation history:', error)
        // If history doesn't exist, show welcome message
        setMessages([
          {
            id: 1,
            type: 'assistant',
            content: "Hello! I'm your AI appointment scheduling assistant. This is a **demo application** running on Groq's free tier (100,000 tokens/day limit).\n\nI can help you:\n✅ Create, view, update, or cancel appointments (one at a time)\n✅ Create new user accounts\n✅ Update user information\n\nHow can I assist you today?",
            thinking: false,
            toolCalls: null,
          },
        ])
      } finally {
        setIsLoadingHistory(false)
      }
    }
    
    loadHistory()
  }, [])

  // Initialize WebSocket connection
  useEffect(() => {
    // Wait for history to load before connecting WebSocket
    if (isLoadingHistory) return
    
    const sessionId = sessionIdRef.current
    
    // Create SockJS connection
    const socket = new SockJS(WS_BASE_URL)
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected')
        setIsConnected(true)
        
        // Subscribe to thinking steps
        stompClient.subscribe(`/topic/thinking/${sessionId}`, (message) => {
          try {
            const data = JSON.parse(message.body)
            handleWebSocketMessage(data)
          } catch (e) {
            console.error('Error parsing WebSocket message:', e)
          }
        })
        
        // Subscribe to final responses
        stompClient.subscribe(`/topic/response/${sessionId}`, (message) => {
          try {
            const data = JSON.parse(message.body)
            handleWebSocketMessage(data)
          } catch (e) {
            console.error('Error parsing WebSocket message:', e)
          }
        })
        
        // Subscribe to errors
        stompClient.subscribe(`/topic/error/${sessionId}`, (message) => {
          try {
            const data = JSON.parse(message.body)
            handleWebSocketMessage(data)
          } catch (e) {
            console.error('Error parsing WebSocket message:', e)
          }
        })
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected')
        setIsConnected(false)
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        setIsConnected(false)
      },
    })
    
    stompClient.activate()
    stompClientRef.current = stompClient
    
    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate()
      }
    }
  }, [isLoadingHistory])

  const handleWebSocketMessage = (data) => {
    // Handle ThinkingStep object (from sendThinkingStep)
    if (data.toolName) {
      setMessages(prev => {
        // Remove only the most recent thinking message if it's a placeholder
        // Keep iteration messages and other thinking steps
        const filtered = prev.filter((msg, index) => {
          // Keep all non-thinking messages
          if (msg.type !== 'thinking' || !msg.thinking) return true
          // Keep iteration messages (they contain "Iteration")
          if (msg.content && msg.content.includes('Iteration')) return true
          // Keep messages with tool calls (they're already executed)
          if (msg.toolCalls && msg.toolCalls.result && msg.toolCalls.result !== 'Executing...') return true
          // Remove only the last placeholder thinking message
          return false
        })
        
        // Add new thinking step with tool call
        return [
          ...filtered,
          {
            id: Date.now(),
            type: 'thinking',
            content: data.thinking || `Using ${data.toolName}...`,
            thinking: true,
            toolCalls: {
              name: data.toolName,
              args: data.toolCall,
              result: data.toolResult || 'Executing...',
            },
          },
        ]
      })
    } else if (data.type === 'thinking') {
      // Add thinking message (keep all thinking messages, don't remove them)
      setMessages(prev => {
        // Only remove placeholder thinking messages without content
        const filtered = prev.filter(msg => {
          if (msg.type !== 'thinking' || !msg.thinking) return true
          // Keep all thinking messages with content
          if (msg.content && msg.content.trim().length > 0) return true
          return false
        })
        
        // Add new thinking message
        return [
          ...filtered,
          {
            id: Date.now(),
            type: 'thinking',
            content: data.content,
            thinking: true,
            toolCalls: null,
          },
        ]
      })
    } else if (data.type === 'response') {
      // Remove thinking messages and add final response
      setMessages(prev => {
        const filtered = prev.filter(msg => msg.type !== 'thinking' && !msg.thinking)
        return [
          ...filtered,
          {
            id: Date.now(),
            type: 'assistant',
            content: data.content,
            thinking: false,
            toolCalls: null,
          },
        ]
      })
      setIsLoading(false)
      onMessageSent()
      toast.success('Response received')
    } else if (data.type === 'error') {
      setMessages(prev => {
        const filtered = prev.filter(msg => msg.type !== 'thinking' && !msg.thinking)
        return [
          ...filtered,
          {
            id: Date.now(),
            type: 'assistant',
            content: data.content,
            thinking: false,
            toolCalls: null,
          },
        ]
      })
      setIsLoading(false)
      toast.error('An error occurred')
    }
  }

  const handleSend = async (e) => {
    e.preventDefault()
    if (!input.trim() || isLoading) return

    const userMessage = input.trim()
    setInput('')

    // Add user message
    const userMsg = {
      id: Date.now(),
      type: 'user',
      content: userMessage,
      thinking: false,
    }
    setMessages(prev => [...prev, userMsg])

    // Add thinking indicator
    const thinkingMsg = {
      id: Date.now() + 1,
      type: 'assistant',
      content: '',
      thinking: true,
    }
    setMessages(prev => [...prev, thinkingMsg])
    setIsLoading(true)

    try {
      // Send request via HTTP - WebSocket will receive real-time updates
      const response = await axios.post(`${API_BASE_URL}/agent/chat`, {
        message: userMessage,
      }, {
        headers: {
          'X-Session-Id': sessionIdRef.current, // Pass session ID for WebSocket routing
        },
      })

      // The WebSocket will handle real-time updates
      // This response is just a fallback if WebSocket fails
      if (!isConnected) {
        // Fallback to HTTP response if WebSocket not connected
        setMessages(prev => {
          const filtered = prev.filter(msg => !msg.thinking)
          
          // Add thinking steps if available
          const newMessages = [...filtered]
          if (response.data.thinkingSteps && response.data.thinkingSteps.length > 0) {
            response.data.thinkingSteps.forEach((step, index) => {
              newMessages.push({
                id: Date.now() + 100 + index,
                type: 'thinking',
                content: step.thinking || 'Thinking...',
                thinking: true,
                toolCalls: step.toolName ? {
                  name: step.toolName,
                  args: step.toolCall,
                  result: step.toolResult,
                } : null,
              })
            })
          }
          
          // Add final response
          newMessages.push({
            id: Date.now() + 2,
            type: 'assistant',
            content: response.data.finalResponse || response.data.response || 'Response received',
            thinking: false,
            toolCalls: null,
          })
          
          return newMessages
        })
        
        onMessageSent()
        toast.success('Message processed successfully')
        setIsLoading(false)
      }
      // If WebSocket is connected, it will handle the updates
      
    } catch (error) {
      console.error('Error sending message:', error)
      
      // Remove thinking indicator
      setMessages(prev => prev.filter(msg => !msg.thinking))

      // Add error message
      setMessages(prev => [
        ...prev,
        {
          id: Date.now() + 2,
          type: 'assistant',
          content: 'Sorry, I encountered an error processing your request. Please try again.',
          thinking: false,
        },
      ])

      if (error.response) {
        toast.error(`Error: ${error.response.data?.response || error.message}`)
      } else if (error.request) {
        toast.error('Backend server is not responding. Please make sure the Spring Boot server is running on port 8080.')
      } else {
        toast.error(`Error: ${error.message}`)
      }
      setIsLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Connection Status Indicator */}
      <div className="px-4 py-2 bg-slate-800 border-b border-slate-700 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-xs text-slate-400">Real-time thinking enabled</span>
          <div className="flex items-center gap-2 px-2 py-1 bg-blue-900/30 border border-blue-700/50 rounded">
            <User className="w-3 h-3 text-blue-400" />
            <span className="text-xs text-blue-300 font-medium">Demo User: Alperen Ugus</span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
          <span className="text-xs text-slate-400">{isConnected ? 'Connected' : 'Disconnected'}</span>
        </div>
      </div>

      {/* Messages Container */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {isLoadingHistory ? (
          <div className="flex items-center justify-center h-full">
            <div className="flex items-center gap-2 text-slate-400">
              <Loader2 className="w-5 h-5 animate-spin" />
              <span>Loading conversation...</span>
            </div>
          </div>
        ) : (
          messages.map((message) => (
          <div
            key={message.id}
            className={`flex gap-3 ${
              message.type === 'user' ? 'justify-end' : 'justify-start'
            }`}
          >
            {(message.type === 'assistant' || message.type === 'thinking') && (
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
                {message.thinking ? (
                  <Loader2 className="w-5 h-5 text-white animate-spin" />
                ) : (
                  <Bot className="w-5 h-5 text-white" />
                )}
              </div>
            )}
            <div
              className={`max-w-[80%] rounded-lg px-4 py-3 ${
                message.type === 'user'
                  ? 'bg-blue-600 text-white'
                  : message.type === 'thinking'
                  ? 'bg-slate-800 text-slate-200 border border-slate-600'
                  : message.thinking
                  ? 'bg-slate-700 text-slate-300 border border-slate-600'
                  : 'bg-slate-700 text-slate-100'
              }`}
            >
              {message.type === 'thinking' && message.toolCalls ? (
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm">
                    <Loader2 className="w-4 h-4 animate-spin text-blue-400" />
                    <span className="italic text-slate-300">{message.content}</span>
                  </div>
                  <div className="mt-2 pt-2 border-t border-slate-600">
                    <div className="text-xs text-slate-400 mb-1">
                      🔧 Using tool: <span className="font-mono text-blue-400">{message.toolCalls.name}</span>
                    </div>
                    {message.toolCalls.args && (
                      <div className="text-xs text-slate-400 mb-1">
                        <span className="text-slate-500">Arguments:</span>
                        <pre className="mt-1 p-2 bg-slate-900 rounded text-slate-300 overflow-x-auto text-xs">
                          {typeof message.toolCalls.args === 'string' 
                            ? message.toolCalls.args 
                            : JSON.stringify(message.toolCalls.args, null, 2)}
                        </pre>
                      </div>
                    )}
                    {message.toolCalls.result && (
                      <div className="text-xs text-slate-400 mt-2">
                        <span className="text-slate-500">Result:</span>
                        <div className="mt-1 p-2 bg-slate-900 rounded text-green-400 text-xs">
                          {message.toolCalls.result}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ) : message.thinking ? (
                <div className="flex items-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span className="text-sm italic">{message.content || 'Thinking...'}</span>
                </div>
              ) : (
                <p className="text-sm whitespace-pre-wrap">{message.content}</p>
              )}
            </div>
            {message.type === 'user' && (
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-slate-600 flex items-center justify-center">
                <User className="w-5 h-5 text-white" />
              </div>
            )}
          </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form */}
      <div className="border-t border-slate-700 p-4">
        <form onSubmit={handleSend} className="flex gap-2">
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your message... (e.g., 'Create an appointment for Alperen Ugus on...')"
            disabled={isLoading}
            className="flex-1 px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed"
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors flex items-center gap-2"
          >
            <Send className="w-4 h-4" />
            <span>Send</span>
          </button>
        </form>
      </div>
    </div>
  )
}

export default ChatComponent
