import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Link2 } from 'lucide-react'

function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/auth/public/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })

      if (!response.ok) throw new Error('Invalid username or password')

      const data = await response.json()
      localStorage.setItem('token', data.token)
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="flex items-center gap-2 mb-8 justify-center">
          <div className="w-8 h-8 rounded-md bg-amber flex items-center justify-center">
            <Link2 size={18} className="text-ink" strokeWidth={2.5} />
          </div>
          <span className="font-display font-bold text-lg text-paper">shortlink</span>
        </div>

        <div className="bg-panel rounded-xl border border-panel-2 overflow-hidden">
          <div className="flex items-center gap-1.5 px-4 py-3 border-b border-panel-2">
            <div className="w-2.5 h-2.5 rounded-full bg-coral/40" />
            <div className="w-2.5 h-2.5 rounded-full bg-amber/40" />
            <div className="w-2.5 h-2.5 rounded-full bg-teal/40" />
            <span className="ml-2 text-xs font-mono text-muted">login</span>
          </div>

          <form onSubmit={handleSubmit} className="p-6">
            <h1 className="font-display font-bold text-xl text-paper mb-1">Welcome back</h1>
            <p className="text-sm text-muted mb-6">Log in to manage your links</p>

            {error && (
              <div className="bg-coral/10 border border-coral/30 text-coral text-sm px-3 py-2 rounded-lg mb-4">
                {error}
              </div>
            )}

            <label className="block text-xs font-medium text-muted mb-1.5">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-3 py-2.5 mb-4 rounded-lg bg-ink border border-panel-2 text-paper text-sm focus:outline-none focus:border-amber transition"
            />

            <label className="block text-xs font-medium text-muted mb-1.5">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3 py-2.5 mb-6 rounded-lg bg-ink border border-panel-2 text-paper text-sm focus:outline-none focus:border-amber transition"
            />

            <button
              type="submit"
              className="w-full bg-amber hover:bg-amber-dim text-ink font-semibold text-sm py-2.5 rounded-lg transition"
            >
              Log in
            </button>

            <p className="text-sm text-muted mt-6 text-center">
              No account?{' '}
              <Link to="/register" className="text-amber hover:underline">Sign up</Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}

export default Login