'use client'
import { useState } from 'react'
import Nav from '../components/Nav'
import client from '../../lib/client'

export default function SettingsPage() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault(); setError(''); setSuccess('')
    if (form.newPassword !== form.confirmPassword) { setError('Passwords do not match'); return }
    try {
      await client.put('/users/me/password', {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      })
      setSuccess('Password changed successfully.')
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to change password')
    }
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={card}>
          <h2 style={{ margin: '0 0 20px', fontSize: 18 }}>Change Password</h2>
          {error && <div style={errorBox}>{error}</div>}
          {success && <div style={successBox}>{success}</div>}
          <form onSubmit={handleSubmit}>
            {([
              ['Current password', 'currentPassword'],
              ['New password', 'newPassword'],
              ['Confirm new password', 'confirmPassword'],
            ] as const).map(([lbl, name]) => (
              <div key={name}>
                <label style={label}>{lbl}</label>
                <input style={input} name={name} type="password"
                  value={form[name]} onChange={handleChange} required minLength={8} />
              </div>
            ))}
            <button style={btn} type="submit">Update password</button>
          </form>
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 480, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '28px 32px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 16, marginBottom: 4 }
const input: React.CSSProperties = { width: '100%', padding: '9px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 14 }
const successBox: React.CSSProperties = { background: '#f0fdf4', color: '#16a34a', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 14 }
const btn: React.CSSProperties = { marginTop: 22, padding: '10px 22px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' }
