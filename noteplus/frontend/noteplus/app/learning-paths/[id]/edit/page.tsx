'use client'
import { use, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import Nav from '../../../components/Nav'
import client from '../../../../lib/client'

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
  const [myNotes, setMyNotes] = useState<Note[]>([])
  const [search, setSearch] = useState('')
  const [saving, setSaving] = useState(false)
  const [addingId, setAddingId] = useState<string | null>(null)
  const [removingId, setRemovingId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [noteError, setNoteError] = useState('')

  useEffect(() => {
    client.get<LearningPath>(`/learning-paths/${id}`)
      .then(r => {
        setForm({ title: r.data.title, description: r.data.description ?? '' })
        setNotes(r.data.notes)
      })
      .catch(() => router.push('/learning-paths'))

    client.get<Note[]>('/notes')
      .then(r => setMyNotes(r.data))
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
    setAddingId(note.id); setNoteError('')
    try {
      const res = await client.post<LearningPath>(`/learning-paths/${id}/notes/${note.id}`)
      setNotes(res.data.notes)
      setSearch('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setNoteError(msg || 'Failed to add note')
    } finally {
      setAddingId(null)
    }
  }

  async function removeNote(noteId: string) {
    setRemovingId(noteId); setNoteError('')
    try {
      const res = await client.delete<LearningPath>(`/learning-paths/${id}/notes/${noteId}`)
      setNotes(res.data.notes)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setNoteError(msg || 'Failed to remove note')
    } finally {
      setRemovingId(null)
    }
  }

  const linkedIds = new Set(notes.map(n => n.id))
  const searchResults = search.trim()
    ? myNotes.filter(n => n.title.toLowerCase().includes(search.toLowerCase()))
    : []

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={{ marginBottom: 12 }}>
          <Link href="/learning-paths" style={{ fontSize: 13, color: '#6b7280', textDecoration: 'none' }}>
            &larr; Back to learning paths
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
              <button style={cancelBtn} type="button" onClick={() => router.push('/learning-paths')}>
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
            notes.map(n => (
              <div key={n.id} style={noteRow}>
                <div>
                  <span style={{ fontSize: 14, fontWeight: 500 }}>{n.title}</span>
                  <span style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>by {n.ownerUsername}</span>
                </div>
                <button
                  style={{ ...chipX, opacity: removingId === n.id ? 0.5 : 1 }}
                  type="button"
                  disabled={removingId === n.id}
                  onClick={() => removeNote(n.id)}
                >
                  ×
                </button>
              </div>
            ))
          )}

          <div style={{ marginTop: 20 }}>
            <label style={label}>Search your notes to add</label>
            <input
              style={inp}
              placeholder="Type note title…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />

            {searchResults.length > 0 && (
              <div style={resultList}>
                {searchResults.slice(0, 8).map(n => {
                  const linked = linkedIds.has(n.id)
                  const adding = addingId === n.id
                  return (
                    <div key={n.id} style={resultRow}>
                      <div>
                        <span style={{ fontSize: 13, fontWeight: 500 }}>{n.title}</span>
                        <span style={{ fontSize: 11, color: '#6b7280', marginLeft: 6 }}>by {n.ownerUsername}</span>
                      </div>
                      <button
                        style={{
                          ...addNoteBtn,
                          opacity: linked || adding ? 0.5 : 1,
                          cursor: linked || adding ? 'not-allowed' : 'pointer',
                        }}
                        type="button"
                        disabled={linked || adding}
                        onClick={() => addNote(n)}
                      >
                        {adding ? 'Adding…' : linked ? 'Added' : '+ Add'}
                      </button>
                    </div>
                  )
                })}
              </div>
            )}
            {search.trim() && searchResults.length === 0 && (
              <p style={{ ...muted, marginTop: 6, fontSize: 13 }}>No matching notes found.</p>
            )}
          </div>
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
const noteRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #f3f4f6' }
const chipX: React.CSSProperties = { background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: 20, padding: '0 4px', lineHeight: 1 }
const resultList: React.CSSProperties = { marginTop: 8, border: '1px solid #e5e7eb', borderRadius: 6, overflow: 'hidden' }
const resultRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', borderBottom: '1px solid #f3f4f6', background: 'white' }
const addNoteBtn: React.CSSProperties = { padding: '5px 14px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 5, fontSize: 12, fontWeight: 600, cursor: 'pointer' }
