'use client'
import { useEffect, useState } from 'react'
import Nav from '../components/Nav'
import client from '../../lib/client'

interface LearningPath { id: string; title: string; description?: string }

export default function LearningPathsPage() {
  const [paths, setPaths] = useState<LearningPath[]>([])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')

  function load() {
    client.get('/learning-paths').then(r => setPaths(r.data)).catch(() => {})
  }

  useEffect(load, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault(); setError('')
    try {
      await client.post('/learning-paths', { title, description })
      setTitle(''); setDescription(''); load()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to create learning path')
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete?')) return
    try { await client.delete(`/learning-paths/${id}`); load() }
    catch { alert('Cannot delete this learning path') }
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={{ ...card, marginBottom: 20 }}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>New Learning Path</h2>
          {error && <div style={errorBox}>{error}</div>}
          <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 500 }}>
            <input style={inp} placeholder="Title *" value={title} onChange={e => setTitle(e.target.value)} required />
            <input style={inp} placeholder="Description" value={description} onChange={e => setDescription(e.target.value)} />
            <button style={addBtn} type="submit">Create</button>
          </form>
        </div>
        <div style={card}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>Learning Paths</h2>
          {paths.length === 0 ? <p style={muted}>No learning paths yet.</p> :
           paths.map(p => (
            <div key={p.id} style={row}>
              <div>
                <strong style={{ fontSize: 15 }}>{p.title}</strong>
                {p.description && <p style={{ margin: '2px 0 0', color: '#6b7280', fontSize: 13 }}>{p.description}</p>}
              </div>
              <button style={delBtn} onClick={() => handleDelete(p.id)}>Delete</button>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 800, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '24px 28px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const inp: React.CSSProperties = { padding: '9px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, fontFamily: 'inherit' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13 }
const addBtn: React.CSSProperties = { padding: '9px 20px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer', alignSelf: 'flex-start' }
const muted: React.CSSProperties = { color: '#9ca3af' }
const row: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', padding: '16px 0', borderBottom: '1px solid #f3f4f6' }
const delBtn: React.CSSProperties = { padding: '5px 14px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer', marginLeft: 16 }
