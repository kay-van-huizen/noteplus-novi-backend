'use client'
import { useEffect, useState } from 'react'
import Link from 'next/link'
import Nav from '../components/Nav'
import client from '../../lib/client'

interface LearningPath { id: string; title: string; description?: string; studentUsername: string; coachUsername: string }
interface User { id: string; username: string; email: string }

export default function LearningPathsPage() {
  const [paths, setPaths] = useState<LearningPath[]>([])
  const [coaches, setCoaches] = useState<User[]>([])
  const [students, setStudents] = useState<User[]>([])
  const [form, setForm] = useState({ title: '', description: '', coachId: '', studentId: '' })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  function load() {
    client.get<LearningPath[]>('/learning-paths').then(r => setPaths(r.data)).catch(() => {})
  }

  useEffect(() => {
    load()
    client.get<User[]>('/users/coaches').then(r => setCoaches(r.data)).catch(() => {})
    client.get<User[]>('/users/students').then(r => setStudents(r.data)).catch(() => {})
  }, [])

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
    setFieldErrors(fe => ({ ...fe, [e.target.name]: '' }))
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!form.coachId) errs.coachId = 'Please select a coach'
    if (!form.studentId) errs.studentId = 'Please select a student'
    if (Object.keys(errs).length > 0) { setFieldErrors(errs); return }
    setError('')
    try {
      await client.post('/learning-paths', {
        title: form.title,
        description: form.description || null,
        coachId: form.coachId,
        studentId: form.studentId,
      })
      setForm({ title: '', description: '', coachId: '', studentId: '' })
      load()
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
          <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
            <label style={label}>Title *</label>
            <input style={inp} name="title" placeholder="Title" value={form.title} onChange={handleChange} required />

            <label style={label}>Description</label>
            <input style={inp} name="description" placeholder="Description (optional)" value={form.description} onChange={handleChange} />

            <label style={label}>Coach *</label>
            <select style={{ ...inp, borderColor: fieldErrors.coachId ? '#dc2626' : '#d1d5db' }} name="coachId" value={form.coachId} onChange={handleChange}>
              <option value="">— select coach —</option>
              {coaches.map(c => <option key={c.id} value={c.id}>{c.username}</option>)}
            </select>
            {fieldErrors.coachId && <p style={fieldErr}>{fieldErrors.coachId}</p>}

            <label style={label}>Student *</label>
            <select style={{ ...inp, borderColor: fieldErrors.studentId ? '#dc2626' : '#d1d5db' }} name="studentId" value={form.studentId} onChange={handleChange}>
              <option value="">— select student —</option>
              {students.map(s => <option key={s.id} value={s.id}>{s.username}</option>)}
            </select>
            {fieldErrors.studentId && <p style={fieldErr}>{fieldErrors.studentId}</p>}

            <button style={{ ...addBtn, marginTop: 16 }} type="submit">Create</button>
          </form>
        </div>

        <div style={card}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>Learning Paths</h2>
          {paths.length === 0 ? <p style={muted}>No learning paths yet.</p> :
           paths.map(p => (
            <div key={p.id} style={row}>
              <div>
                <strong style={{ fontSize: 15 }}>{p.title}</strong>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>
                  Coach: <strong>{p.coachUsername}</strong> · Student: <strong>{p.studentUsername}</strong>
                </div>
                {p.description && <p style={{ margin: '4px 0 0', color: '#6b7280', fontSize: 13 }}>{p.description}</p>}
              </div>
              <div style={{ display: 'flex', gap: 8, marginLeft: 16 }}>
                <Link href={`/learning-paths/${p.id}/edit`}>
                  <button style={editBtn}>Edit</button>
                </Link>
                <button style={delBtn} onClick={() => handleDelete(p.id)}>Delete</button>
              </div>
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
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 12, marginBottom: 4 }
const inp: React.CSSProperties = { width: '100%', padding: '9px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, fontFamily: 'inherit', boxSizing: 'border-box' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 12 }
const fieldErr: React.CSSProperties = { color: '#dc2626', fontSize: 12, margin: '4px 0 0' }
const addBtn: React.CSSProperties = { padding: '9px 20px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer', alignSelf: 'flex-start' }
const muted: React.CSSProperties = { color: '#9ca3af', fontSize: 14 }
const row: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', padding: '16px 0', borderBottom: '1px solid #f3f4f6' }
const editBtn: React.CSSProperties = { padding: '5px 14px', background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer' }
const delBtn: React.CSSProperties = { padding: '5px 14px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer' }
