'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import client from '../../lib/client'

export default function RegisterPage() {
  const router = useRouter()
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await client.post('/auth/register', form)
      router.push('/notes')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Registration failed')
    }
  }

  const fields: { label: string; name: keyof typeof form; type: string }[] = [
    { label: 'Username', name: 'username', type: 'text' },
    { label: 'Email', name: 'email', type: 'email' },
    { label: 'Password', name: 'password', type: 'password' },
  ]

  return (
    <div style={page}>
      <div style={card}>
        <h1 style={logo}>NotePlus</h1>
        <p style={sub}>Create your account</p>
        {error && <div style={errorBox}>{error}</div>}
        <form onSubmit={handleSubmit}>
          {fields.map(f => (
            <div key={f.name}>
              <label style={label}>{f.label}</label>
              <input style={input} name={f.name} type={f.type}
                value={form[f.name]} onChange={handleChange} required />
            </div>
          ))}
          <button style={btn} type="submit">Create account</button>
        </form>
        <p style={footer}>Already have an account? <Link href="/login">Sign in</Link></p>
      </div>
    </div>
  )
}

const page: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }
const card: React.CSSProperties = { background: 'white', width: '100%', maxWidth: 420, padding: '40px 36px', borderRadius: 10, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }
const logo: React.CSSProperties = { fontSize: 24, fontWeight: 700, margin: '0 0 4px' }
const sub: React.CSSProperties = { fontSize: 14, color: '#6b7280', margin: '0 0 24px' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 16, marginBottom: 4 }
const input: React.CSSProperties = { width: '100%', padding: '10px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 8 }
const btn: React.CSSProperties = { width: '100%', marginTop: 24, padding: 11, background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 15, fontWeight: 600, cursor: 'pointer' }
const footer: React.CSSProperties = { marginTop: 20, textAlign: 'center', fontSize: 13, color: '#6b7280' }
