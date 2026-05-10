'use client'
import { use, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Nav from '../../components/Nav'
import client from '../../../lib/client'

const COLORS = ['DEFAULT', 'PINK', 'RED', 'ORANGE', 'YELLOW', 'GREEN', 'TEAL', 'BLUE', 'PURPLE', 'GRAY']

interface Category { id: string; title: string; description?: string; color: string; status: string; parentId?: string }

export default function CategoryEditPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const [form, setForm] = useState({ title: '', description: '', color: 'DEFAULT', status: 'ACTIVE' })
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    client.get<Category>(`/categories/${id}`).then(r => {
      const c = r.data
      setForm({
        title: c.title,
        description: c.description ?? '',
        color: c.color,
        status: c.status,
      })
    }).catch(() => router.push('/categories'))

    client.get<Category[]>('/categories').then(r =>
      setCategories(r.data.filter(c => c.id !== id))
    ).catch(() => {})
  }, [id, router])

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) { setError('Title is required'); return }
    if (!form.color) { setError('Color is required'); return }
    setSaving(true); setError('')
    try {
      await client.put(`/categories/${id}`, {
        title: form.title.trim(),
        description: form.description || null,
        color: form.color,
        status: form.status,
      })
      router.push('/categories')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={card}>
          <h1 style={{ margin: '0 0 20px', fontSize: 20 }}>Edit Category</h1>
          {error && <div style={errorBox}>{error}</div>}
          <form onSubmit={handleSubmit}>
            <label style={label}>Title *</label>
            <input style={inp} name="title" value={form.title} onChange={handleChange} required />

            <label style={label}>Description</label>
            <textarea style={{ ...inp, minHeight: 80, resize: 'vertical' }} name="description" value={form.description} onChange={handleChange} />

            <label style={label}>Color *</label>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <select style={inp} name="color" value={form.color} onChange={handleChange}>
                {COLORS.map(c => <option key={c}>{c}</option>)}
              </select>
              <span style={{ width: 24, height: 24, borderRadius: '50%', background: form.color === 'DEFAULT' ? '#d1d5db' : form.color.toLowerCase(), border: '1px solid #e5e7eb', display: 'inline-block' }} />
            </div>

            <label style={label}>Status</label>
            <select style={inp} name="status" value={form.status} onChange={handleChange}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>

            <div style={{ display: 'flex', gap: 10, marginTop: 24 }}>
              <button style={saveBtn} type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
              <button style={cancelBtn} type="button" onClick={() => router.push('/categories')}>Cancel</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 520, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '28px 32px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const label: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, color: '#374151', marginTop: 16, marginBottom: 4 }
const inp: React.CSSProperties = { width: '100%', padding: '9px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box', fontFamily: 'inherit' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 12 }
const saveBtn: React.CSSProperties = { padding: '9px 22px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' }
const cancelBtn: React.CSSProperties = { padding: '9px 18px', background: 'none', color: '#6b7280', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, cursor: 'pointer' }
