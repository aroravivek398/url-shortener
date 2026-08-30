import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Link2, LogOut, BarChart3, Trash2, ExternalLink, Download } from 'lucide-react'

function Dashboard() {
  const [urls, setUrls] = useState([])
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const [originalUrl, setOriginalUrl] = useState('')
  const [customAlias, setCustomAlias] = useState('')
  const [expiryDays, setExpiryDays] = useState('')

  const handleLogout = () => {
    localStorage.removeItem('token')
    navigate('/login')
  }

  const handleDelete = async (shortUrl) => {
    const token = localStorage.getItem('token')
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/urls/${shortUrl}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!response.ok) throw new Error('Failed to delete URL')
      setUrls(urls.filter((url) => url.shortUrl !== shortUrl))
    } catch (err) {
      setError(err.message)
    }
  }

  const handleDownloadQr = async (shortUrl) => {
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/urls/${shortUrl}/qrcode`)
      const blob = await response.blob()
      const blobUrl = window.URL.createObjectURL(blob)

      const link = document.createElement('a')
      link.href = blobUrl
      link.download = `qrcode-${shortUrl}.png`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(blobUrl)
    } catch (err) {
      setError('Failed to download QR code')
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    const token = localStorage.getItem('token')

    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/urls/shorten`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          originalUrl,
          customAlias: customAlias || null,
          expiryDays: expiryDays ? parseInt(expiryDays) : null,
        }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Failed to create URL')
      }

      const newUrl = await response.json()
      setUrls([newUrl, ...urls])
      setOriginalUrl('')
      setCustomAlias('')
      setExpiryDays('')
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) {
      navigate('/login')
      return
    }

    const fetchUrls = async () => {
      try {
        const response = await fetch(`${import.meta.env.VITE_API_URL}/api/urls/myurls`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (!response.ok) throw new Error('Failed to load your URLs')
        const data = await response.json()
        setUrls(data)
      } catch (err) {
        setError(err.message)
      }
    }

    fetchUrls()
  }, [])

  return (
    <div className="min-h-screen px-6 py-8 max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-md bg-amber flex items-center justify-center">
            <Link2 size={18} className="text-ink" strokeWidth={2.5} />
          </div>
          <span className="font-display font-bold text-lg text-paper">shortlink</span>
        </div>
        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 text-sm text-muted hover:text-coral transition"
        >
          <LogOut size={15} /> Logout
        </button>
      </div>

      <div className="bg-panel rounded-xl border border-panel-2 p-6 mb-8">
        <h2 className="font-display font-bold text-paper mb-4">New link</h2>
        <form onSubmit={handleCreate} className="space-y-3">
          <input
            type="text"
            placeholder="https://example.com/very/long/path"
            value={originalUrl}
            onChange={(e) => setOriginalUrl(e.target.value)}
            className="w-full px-3 py-2.5 rounded-lg bg-ink border border-panel-2 text-paper text-sm font-mono focus:outline-none focus:border-amber transition"
          />
          <div className="flex gap-3">
            <input
              type="text"
              placeholder="custom-alias (optional)"
              value={customAlias}
              onChange={(e) => setCustomAlias(e.target.value)}
              className="flex-1 px-3 py-2.5 rounded-lg bg-ink border border-panel-2 text-paper text-sm font-mono focus:outline-none focus:border-amber transition"
            />
            <input
              type="number"
              placeholder="expires in days"
              value={expiryDays}
              onChange={(e) => setExpiryDays(e.target.value)}
              className="w-40 px-3 py-2.5 rounded-lg bg-ink border border-panel-2 text-paper text-sm focus:outline-none focus:border-amber transition"
            />
          </div>
          <button
            type="submit"
            className="bg-amber hover:bg-amber-dim text-ink font-semibold text-sm px-5 py-2.5 rounded-lg transition"
          >
            Shorten
          </button>
        </form>
      </div>

      {error && (
        <div className="bg-coral/10 border border-coral/30 text-coral text-sm px-3 py-2 rounded-lg mb-4">
          {error}
        </div>
      )}

      <div className="space-y-3">
        {urls.map((url) => (
          <div key={url.id} className="bg-panel rounded-xl border border-panel-2 p-4">
            <div className="flex gap-4">
              <button
                onClick={() => handleDownloadQr(url.shortUrl)}
                className="flex-shrink-0 group relative"
                aria-label="Download QR code"
              >
                <img
                  src={`${import.meta.env.VITE_API_URL}/api/urls/${url.shortUrl}/qrcode`}
                  alt="QR code"
                  className="w-28 h-28 rounded-lg bg-paper p-2"
                />
                <div className="absolute inset-0 bg-ink/70 rounded-lg opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
                  <Download size={20} className="text-paper" />
                </div>
              </button>

              <div className="flex-1 min-w-0">
                <p className="text-sm text-muted truncate">{url.originalUrl}</p>
                <a
                  href={`${import.meta.env.VITE_API_URL}/${url.shortUrl}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-mono text-amber text-sm flex items-center gap-1 hover:underline"
                >
                  /{url.shortUrl} <ExternalLink size={12} />
                </a>
                <p className="text-xs text-muted mt-1">{url.clickCount} clicks</p>
              </div>

              <div className="flex flex-col gap-2 items-end">
                <button
                  onClick={() => navigate(`/analytics/${url.shortUrl}`)}
                  className="text-muted hover:text-teal transition"
                  aria-label="View analytics"
                >
                  <BarChart3 size={16} />
                </button>
                <button
                  onClick={() => handleDelete(url.shortUrl)}
                  className="text-muted hover:text-coral transition"
                  aria-label="Delete"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default Dashboard