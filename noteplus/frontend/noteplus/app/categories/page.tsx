'use client'
import { useEffect, useState } from 'react'
import Nav from '../components/Nav'
import client from '../../lib/client'

interface Category { id: string; title: string; color: string; status: string }

const COLORS = ['DEFAULT', 'PINK', 'RED', 'ORANGE', 'YELLOW', 'GREEN', 'TEAL', 'BLUE', 'PURPLE', 'GRAY']

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [title, setTitle] = useState('')
  const [color, setColor] = useState('DEFAULT')
  const [error, setError] = useState('')

  function load() {
    client.get('/categories').then(r => setCategories(r.data)).catch(() => {})
  }

  useEffect(load, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault(); setError('')
    try {
      await client.post('/categories', { title, color, status: 'ACTIVE' })
      setTitle(''); setColor('DEFAULT'); load()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to create category')
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this category?')) return
    try { await client.delete(`/categories/${id}`); load() }
    catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      alert(msg || 'Cannot delete — may have notes attached')
    }
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={{ ...card, marginBottom: 20 }}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>New Category</h2>
          {error && <div style={errorBox}>{error}</div>}
          <form onSubmit={handleCreate} style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <input style={{ ...inp, flex: 1 }} placeholder="Title *" value={title}
              onChange={e => setTitle(e.target.value)} required />
            <select style={inp} value={color} onChange={e => setColor(e.target.value)}>
              {COLORS.map(c => <option key={c}>{c}</option>)}
            </select>
            <button style={addBtn} type="submit">Add</button>
          </form>
        </div>
        <div style={card}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>Categories</h2>
          {categories.length === 0 ? <p style={muted}>No categories yet.</p> :
           categories.map(c => (
            <div key={c.id} style={row}>
              <span style={{ fontWeight: 500 }}>{c.title}</span>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <span style={colorBadge}>{c.color}</span>
                <span style={statusBadge(c.status)}>{c.status}</span>
                <button style={delBtn} onClick={() => handleDelete(c.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 700, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '24px 28px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const inp: React.CSSProperties = { padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, fontFamily: 'inherit', boxSizing: 'border-box' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 12 }
const addBtn: React.CSSProperties = { padding: '9px 20px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' }
const muted: React.CSSProperties = { color: '#9ca3af' }
const row: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid #f3f4f6' }
const colorBadge: React.CSSProperties = { background: '#e0e7ff', color: '#3730a3', padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 500 }
const statusBadge = (s: string): React.CSSProperties => ({ background: s === 'ACTIVE' ? '#dcfce7' : '#f3f4f6', color: s === 'ACTIVE' ? '#16a34a' : '#6b7280', padding: '2px 10px', borderRadius: 12, fontSize: 12 })
const delBtn: React.CSSProperties = { padding: '4px 12px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, fontSize: 12, cursor: 'pointer' }
