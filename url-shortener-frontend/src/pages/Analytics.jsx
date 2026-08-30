import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Smartphone, Monitor, Tablet, HelpCircle } from 'lucide-react'

function Analytics() {
  const { shortUrl } = useParams()
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const token = localStorage.getItem('token')

    if (!token) {
      navigate('/login')
      return
    }

    const fetchStats = async () => {
      try {
        const response = await fetch(
          `${import.meta.env.VITE_API_URL}/api/urls/analytics/${shortUrl}/devices`,
          { headers: { Authorization: `Bearer ${token}` } }
        )
        if (!response.ok) throw new Error('Failed to load analytics')
        const data = await response.json()
        setStats(data)
      } catch (err) {
        setError(err.message)
      }
    }

    fetchStats()
  }, [shortUrl])

  const total = stats
    ? stats.mobileClicks + stats.desktopClicks + stats.tabletClicks + stats.unknownClicks
    : 0

  const rows = stats
    ? [
        { label: 'Mobile', value: stats.mobileClicks, icon: Smartphone, color: 'bg-amber' },
        { label: 'Desktop', value: stats.desktopClicks, icon: Monitor, color: 'bg-teal' },
        { label: 'Tablet', value: stats.tabletClicks, icon: Tablet, color: 'bg-coral' },
        { label: 'Unknown', value: stats.unknownClicks, icon: HelpCircle, color: 'bg-muted' },
      ]
    : []

  return (
    <div className="min-h-screen px-6 py-8 max-w-2xl mx-auto">
      <button
        onClick={() => navigate('/dashboard')}
        className="flex items-center gap-1.5 text-sm text-muted hover:text-paper transition mb-6"
      >
        <ArrowLeft size={15} /> Back to dashboard
      </button>

      <h1 className="font-display font-bold text-2xl text-paper mb-1">Analytics</h1>
      <p className="font-mono text-sm text-amber mb-8">/{shortUrl}</p>

      {error && (
        <div className="bg-coral/10 border border-coral/30 text-coral text-sm px-3 py-2 rounded-lg">
          {error}
        </div>
      )}

      {stats && (
        <div className="bg-panel rounded-xl border border-panel-2 p-6">
          <p className="text-sm text-muted mb-6">
            <span className="text-paper font-display font-bold text-xl">{total}</span> total clicks by device
          </p>

          <div className="space-y-4">
            {rows.map(({ label, value, icon: Icon, color }) => {
              const pct = total > 0 ? Math.round((value / total) * 100) : 0
              return (
                <div key={label}>
                  <div className="flex items-center justify-between mb-1.5">
                    <div className="flex items-center gap-2">
                      <Icon size={15} className="text-muted" />
                      <span className="text-sm text-paper">{label}</span>
                    </div>
                    <span className="text-sm text-muted">{value} · {pct}%</span>
                  </div>
                  <div className="h-2 bg-ink rounded-full overflow-hidden">
                    <div
                      className={`h-full ${color} rounded-full transition-all`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

export default Analytics