'use client'
import { use, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import Nav from '../../../../components/Nav'
import client from '../../../../../lib/client'

interface LearningPath {
  id: string
  title: string
  description?: string
  studentUsername: string
  coachUsername: string
  notes: Note[]
}
interface Note { id: string; title: string; ownerUsername: string }

export default function EditLearningPathPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const [form, setForm] = useState({ title: '', description: '' })
  const [notes, setNotes] = useState<Note[]>([])
  const [allNotes, setAllNotes] = useState<Note[]>([])
  const [search, setSearch] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [noteError, setNoteError] = useState('')

  useEffect(() => {
    client.get<LearningPath>(`/learning-paths/${id}`)
      .then(r => {
        setForm({ title: r.data.title, description: r.data.description ?? '' })
        setNotes(r.data.notes)
      })
      .catch(() => router.push('/admin/learning-paths'))

    client.get<Note[]>('/notes/all')
      .then(r => setAllNotes(r.data))
      .catch(() => {})
  }, [id, router])

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) { setError('Title is required'); return }
    setSaving(true); setError('')
    try {
      await client.put(`/learning-paths/${id}`, { title: form.title.trim(), description: form.description || null })
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  async function addNote(note: Note) {
    if (notes.some(n => n.id === note.id)) return
    setNoteError('')
    try {
      const res = await client.post<LearningPath>(`/learning-paths/${id}/notes/${note.id}`)
      setNotes(res.data.notes)
      setSearch('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setNoteError(msg || 'Failed to add note')
    }
  }

  async function removeNote(noteId: string) {
    setNoteError('')
    try {
      const res = await client.delete<LearningPath>(`/learning-paths/${id}/notes/${noteId}`)
      setNotes(res.data.notes)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setNoteError(msg || 'Failed to remove note')
    }
  }

  const filtered = search.trim()
    ? allNotes.filter(n =>
        !notes.some(linked => linked.id === n.id) &&
        n.title.toLowerCase().includes(search.toLowerCase())
      )
    : []

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={{ marginBottom: 12 }}>
          <Link href="/admin/learning-paths" style={{ fontSize: 13, color: '#6b7280', textDecoration: 'none' }}>
            &larr; Back to all paths
          </Link>
        </div>

        {/* Details form */}
        <div style={card}>
          <h1 style={{ margin: '0 0 20px', fontSize: 20 }}>Edit Learning Path</h1>
          {error && <div style={errorBox}>{error}</div>}
          {saved && <div style={successBox}>Saved successfully.</div>}
          <form onSubmit={handleSave}>
            <label style={label}>Title *</label>
            <input style={inp} name="title" value={form.title} onChange={handleChange} required />

            <label style={label}>Description</label>
            <textarea style={{ ...inp, minHeight: 80, resize: 'vertical' }}
              name="description" value={form.description} onChange={handleChange} />

            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button
                style={{ ...saveBtn, opacity: saving ? 0.7 : 1, cursor: saving ? 'not-allowed' : 'pointer' }}
                type="submit"
                disabled={saving}
              >
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button style={cancelBtn} type="button" onClick={() => router.push('/admin/learning-paths')}>
                Cancel
              </button>
            </div>
          </form>
        </div>

        {/* Notes section */}
        <div style={{ ...card, marginTop: 20 }}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>Notes in this path</h2>
          {noteError && <div style={errorBox}>{noteError}</div>}

          {notes.length === 0 ? (
            <p style={muted}>No notes linked yet.</p>
          ) : (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
              {notes.map(n => (
                <div key={n.id} style={noteChip}>
                  <span style={{ fontSize: 13 }}>{n.title}</span>
                  <span style={{ fontSize: 11, color: '#6b7280', marginLeft: 4 }}>({n.ownerUsername})</span>
                  <button style={chipX} type="button" onClick={() => removeNote(n.id)}>×</button>
                </div>
              ))}
            </div>
          )}

          <label style={label}>Search notes to add</label>
          <div style={{ position: 'relative' }}>
            <input
              style={inp}
              placeholder="Type note title…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            {filtered.length > 0 && (
              <div style={dropdown}>
                {filtered.slice(0, 8).map(n => (
                  <div key={n.id} style={dropdownItem} onClick={() => addNote(n)}>
                    <strong style={{ fontSize: 13 }}>{n.title}</strong>
                    <span style={{ fontSize: 11, color: '#6b7280', marginLeft: 8 }}>{n.ownerUsername}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          {search.trim() && filtered.length === 0 && (
            <p style={{ ...muted, marginTop: 4, fontSize: 13 }}>No matching notes found.</p>
          )}
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 700, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '28px 32px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 16, marginBottom: 4 }
const inp: React.CSSProperties = { width: '100%', padding: '9px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box', fontFamily: 'inherit' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 12 }
const successBox: React.CSSProperties = { background: '#f0fdf4', color: '#16a34a', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 12 }
const saveBtn: React.CSSProperties = { padding: '9px 22px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' }
const cancelBtn: React.CSSProperties = { padding: '9px 18px', background: 'none', color: '#6b7280', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, cursor: 'pointer' }
const muted: React.CSSProperties = { color: '#9ca3af', fontSize: 14 }
const noteChip: React.CSSProperties = { display: 'inline-flex', alignItems: 'center', gap: 4, background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: 20, padding: '5px 12px' }
const chipX: React.CSSProperties = { background: 'none', border: 'none', cursor: 'pointer', color: '#6b7280', fontSize: 15, padding: '0 0 0 4px', lineHeight: 1 }
const dropdown: React.CSSProperties = { position: 'absolute', top: '100%', left: 0, right: 0, background: 'white', border: '1px solid #e5e7eb', borderRadius: 6, boxShadow: '0 4px 12px rgba(0,0,0,0.1)', zIndex: 100, marginTop: 4 }
const dropdownItem: React.CSSProperties = { padding: '10px 14px', cursor: 'pointer', borderBottom: '1px solid #f3f4f6' }
