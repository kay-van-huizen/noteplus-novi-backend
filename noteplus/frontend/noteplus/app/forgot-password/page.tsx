'use client'
import { useState } from 'react'
import Link from 'next/link'
import client from '../../lib/client'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      await client.post('/auth/forgot-password', { email })
    } catch {
      // always show success regardless
    } finally {
      setLoading(false)
      setSent(true)
    }
  }

  return (
    <div style={page}>
      <div style={card}>
        <h1 style={logo}>NotePlus</h1>
        <p style={sub}>Reset your password</p>
        {sent ? (
          <div style={successBox}>
            If that email exists, a reset link has been sent.
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <label style={label}>Email address</label>
            <input
              style={input}
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              autoFocus
            />
            <button style={btn} type="submit" disabled={loading}>
              {loading ? 'Sending…' : 'Send reset link'}
            </button>
          </form>
        )}
        <p style={footer}><Link href="/login">Back to sign in</Link></p>
      </div>
    </div>
  )
}

const page: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }
const card: React.CSSProperties = { background: 'white', width: '100%', maxWidth: 400, padding: '40px 36px', borderRadius: 10, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }
const logo: React.CSSProperties = { fontSize: 24, fontWeight: 700, margin: '0 0 4px' }
const sub: React.CSSProperties = { fontSize: 14, color: '#6b7280', margin: '0 0 24px' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginBottom: 4 }
const input: React.CSSProperties = { width: '100%', padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }
const btn: React.CSSProperties = { width: '100%', marginTop: 16, padding: 11, background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 15, fontWeight: 600, cursor: 'pointer' }
const successBox: React.CSSProperties = { background: '#f0fdf4', color: '#166534', padding: '12px 14px', borderRadius: 6, fontSize: 14 }
const footer: React.CSSProperties = { marginTop: 20, textAlign: 'center', fontSize: 13, color: '#6b7280' }
