'use client'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Nav from './Nav'
import client from '../../lib/client'

interface Category { id: string; title: string }

export default function NoteForm({ id }: { id?: string }) {
  const router = useRouter()
  const isNew = !id
  const [form, setForm] = useState({ title: '', content: '', categoryId: '' })
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    client.get('/categories').then(r => setCategories(r.data)).catch(() => {})
    if (!isNew) {
      client.get(`/notes/${id}`).then(r => {
        const n = r.data
        setForm({ title: n.title, content: n.content ?? '', categoryId: n.categoryId ?? '' })
      }).catch(() => router.push('/notes'))
    }
  }, [id, isNew, router])

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    const payload = { ...form, categoryId: form.categoryId || null }
    try {
      if (isNew) {
        await client.post('/notes', payload)
        router.push('/notes')
      } else {
        await client.put(`/notes/${id}`, payload)
        setSaved(true)
      }
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Save failed')
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this note?')) return
    await client.delete(`/notes/${id}`)
    router.push('/notes')
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={card}>
          <h1 style={{ margin: '0 0 20px', fontSize: 20 }}>{isNew ? 'New Note' : 'Edit Note'}</h1>
          {error && <div style={errorBox}>{error}</div>}
          {saved && <div style={successBox}>Saved successfully.</div>}
          <form onSubmit={handleSubmit}>
            <label style={label}>Title *</label>
            <input style={inp} name="title" value={form.title} onChange={handleChange} required />
            <label style={label}>Content</label>
            <textarea style={{ ...inp, minHeight: 200, resize: 'vertical' }}
              name="content" value={form.content} onChange={handleChange} />
            <label style={label}>Category</label>
            <select style={inp} name="categoryId" value={form.categoryId} onChange={handleChange}>
              <option value="">— no category —</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.title}</option>)}
            </select>
            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button style={saveBtn} type="submit">{isNew ? 'Create' : 'Save'}</button>
              {!isNew && <button style={delBtn} type="button" onClick={handleDelete}>Delete</button>}
              <button style={cancelBtn} type="button" onClick={() => router.push('/notes')}>Cancel</button>
            </div>
          </form>
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
const delBtn: React.CSSProperties = { padding: '9px 18px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer' }
const cancelBtn: React.CSSProperties = { padding: '9px 18px', background: 'none', color: '#6b7280', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, cursor: 'pointer' }
