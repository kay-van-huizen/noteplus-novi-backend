'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import client from '../../lib/client'

export default function LoginPage() {
  const router = useRouter()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await client.post('/auth/login', form)
      router.push('/notes')
    } catch {
      setError('Invalid username or password')
    }
  }

  return (
    <div style={page}>
      <div style={card}>
        <h1 style={logo}>NotePlus</h1>
        <p style={sub}>Sign in to your account</p>
        {error && <div style={errorBox}>{error}</div>}
        <form onSubmit={handleSubmit}>
          <label style={label}>Username</label>
          <input style={input} name="username" value={form.username} onChange={handleChange} required autoFocus />
          <label style={label}>Password</label>
          <input style={input} name="password" type="password" value={form.password} onChange={handleChange} required />
          <button style={btn} type="submit">Sign in</button>
        </form>
        <p style={footer}>No account? <Link href="/register">Register</Link></p>
        <p style={{ ...footer, marginTop: 8 }}>
          <Link href="/forgot-password" style={{ color: '#6b7280' }}>Forgot password?</Link>
        </p>
      </div>
    </div>
  )
}

const page: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }
const card: React.CSSProperties = { background: 'white', width: '100%', maxWidth: 400, padding: '40px 36px', borderRadius: 10, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }
const logo: React.CSSProperties = { fontSize: 24, fontWeight: 700, margin: '0 0 4px' }
const sub: React.CSSProperties = { fontSize: 14, color: '#6b7280', margin: '0 0 24px' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 16, marginBottom: 4 }
const input: React.CSSProperties = { width: '100%', padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 8 }
const btn: React.CSSProperties = { width: '100%', marginTop: 24, padding: 11, background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 15, fontWeight: 600, cursor: 'pointer' }
const footer: React.CSSProperties = { marginTop: 20, textAlign: 'center', fontSize: 13, color: '#6b7280' }
