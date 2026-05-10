'use client'
import { useState, Suspense } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import client from '../../lib/client'

function ResetPasswordForm() {
  const searchParams = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (form.newPassword !== form.confirmPassword) {
      setError('Passwords do not match')
      return
    }
    if (!token) {
      setError('Invalid or missing reset token')
      return
    }
    setLoading(true); setError('')
    try {
      await client.post('/auth/reset-password', { token, newPassword: form.newPassword, confirmPassword: form.confirmPassword })
      setSuccess(true)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Reset failed. The link may have expired.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={page}>
      <div style={card}>
        <h1 style={logo}>NotePlus</h1>
        <p style={sub}>Choose a new password</p>
        {success ? (
          <>
            <div style={successBox}>Password updated successfully.</div>
            <p style={footer}><Link href="/login">Sign in</Link></p>
          </>
        ) : (
          <>
            {error && <div style={errorBox}>{error}</div>}
            {!token && <div style={errorBox}>Invalid reset link. Please request a new one.</div>}
            <form onSubmit={handleSubmit}>
              <label style={label}>New password</label>
              <input
                style={input}
                type="password"
                name="newPassword"
                value={form.newPassword}
                onChange={handleChange}
                required
                autoFocus
                minLength={6}
              />
              <label style={label}>Confirm password</label>
              <input
                style={input}
                type="password"
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={handleChange}
                required
                minLength={6}
              />
              <button style={btn} type="submit" disabled={loading || !token}>
                {loading ? 'Saving…' : 'Set new password'}
              </button>
            </form>
            <p style={footer}><Link href="/login">Back to sign in</Link></p>
          </>
        )}
      </div>
    </div>
  )
}

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordForm />
    </Suspense>
  )
}

const page: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }
const card: React.CSSProperties = { background: 'white', width: '100%', maxWidth: 400, padding: '40px 36px', borderRadius: 10, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }
const logo: React.CSSProperties = { fontSize: 24, fontWeight: 700, margin: '0 0 4px' }
const sub: React.CSSProperties = { fontSize: 14, color: '#6b7280', margin: '0 0 24px' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 16, marginBottom: 4 }
const input: React.CSSProperties = { width: '100%', padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }
const btn: React.CSSProperties = { width: '100%', marginTop: 24, padding: 11, background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 15, fontWeight: 600, cursor: 'pointer' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 8 }
const successBox: React.CSSProperties = { background: '#f0fdf4', color: '#166534', padding: '12px 14px', borderRadius: 6, fontSize: 14 }
const footer: React.CSSProperties = { marginTop: 20, textAlign: 'center', fontSize: 13, color: '#6b7280' }
