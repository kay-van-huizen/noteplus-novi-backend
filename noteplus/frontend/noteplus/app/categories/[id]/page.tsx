'use client'
import { use, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import Nav from '../../components/Nav'
import client from '../../../lib/client'

const COLORS = ['DEFAULT', 'PINK', 'RED', 'ORANGE', 'YELLOW', 'GREEN', 'TEAL', 'BLUE', 'PURPLE', 'GRAY']

interface Category { id: string; title: string; description?: string; color: string; status: string; parentId?: string }
interface SubCategory { id: string; title: string; color: string; status: string }

export default function CategoryEditPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const [form, setForm] = useState({ title: '', description: '', color: 'DEFAULT', status: 'ACTIVE' })
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const [subcategories, setSubcategories] = useState<SubCategory[]>([])
  const [newSubTitle, setNewSubTitle] = useState('')
  const [subcatError, setSubcatError] = useState('')
  const [addingSubcat, setAddingSubcat] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {
    client.get<Category>(`/categories/${id}`).then(r => {
      const c = r.data
      setForm({ title: c.title, description: c.description ?? '', color: c.color, status: c.status })
    }).catch(() => router.push('/categories'))

    client.get<Category[]>('/categories').then(r =>
      setCategories(r.data.filter(c => c.id !== id))
    ).catch(() => {})

    client.get<SubCategory[]>(`/categories/${id}/children`)
      .then(r => setSubcategories(r.data))
      .catch(() => {})

    client.get('/users/me')
      .then(r => setIsAdmin(r.data.roles?.includes('ROLE_ADMIN') ?? false))
      .catch(() => {})
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

  async function addSubcat() {
    if (!newSubTitle.trim()) return
    setAddingSubcat(true); setSubcatError('')
    try {
      const res = await client.post<SubCategory>('/categories', {
        title: newSubTitle.trim(),
        color: 'DEFAULT',
        status: 'ACTIVE',
        parentId: id,
      })
      setSubcategories(s => [...s, res.data])
      setNewSubTitle('')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setSubcatError(msg || 'Failed to add subcategory')
    } finally {
      setAddingSubcat(false)
    }
  }

  async function removeSubcat(subcatId: string) {
    if (!confirm('Delete this subcategory?')) return
    setSubcatError('')
    try {
      await client.delete(`/categories/${subcatId}`)
      setSubcategories(s => s.filter(sub => sub.id !== subcatId))
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setSubcatError(msg || 'Failed to remove subcategory')
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
              <button
                style={{ ...saveBtn, opacity: saving ? 0.7 : 1, cursor: saving ? 'not-allowed' : 'pointer' }}
                type="submit"
                disabled={saving}
              >
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button style={cancelBtn} type="button" onClick={() => router.push('/categories')}>Cancel</button>
            </div>
          </form>
        </div>

        <div style={{ ...card, marginTop: 20 }}>
          <h2 style={{ margin: '0 0 4px', fontSize: 18 }}>Subcategories</h2>
          <p style={{ margin: '0 0 16px', fontSize: 13, color: '#6b7280' }}>
            {isAdmin ? 'You can add and remove subcategories.' : 'You can add subcategories.'}
          </p>
          {subcatError && <div style={errorBox}>{subcatError}</div>}

          {subcategories.length === 0 ? (
            <p style={muted}>No subcategories yet.</p>
          ) : (
            subcategories.map(s => (
              <div key={s.id} style={subRow}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ width: 10, height: 10, borderRadius: '50%', background: s.color === 'DEFAULT' ? '#d1d5db' : s.color.toLowerCase(), display: 'inline-block', border: '1px solid #e5e7eb' }} />
                  <span style={{ fontSize: 14 }}>{s.title}</span>
                  {s.status === 'INACTIVE' && <span style={inactiveBadge}>Inactive</span>}
                </div>
                {isAdmin && (
                  <button style={removeBtn} type="button" onClick={() => removeSubcat(s.id)}>×</button>
                )}
              </div>
            ))
          )}

          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <input
              style={{ ...inp, flex: 1 }}
              placeholder="New subcategory name"
              value={newSubTitle}
              onChange={e => setNewSubTitle(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addSubcat() } }}
            />
            <button
              style={{ ...addBtn, opacity: addingSubcat ? 0.7 : 1, cursor: addingSubcat ? 'not-allowed' : 'pointer' }}
              type="button"
              onClick={addSubcat}
              disabled={addingSubcat}
            >
              {addingSubcat ? 'Adding…' : 'Add'}
            </button>
          </div>
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
const muted: React.CSSProperties = { color: '#9ca3af', fontSize: 14 }
const subRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #f3f4f6' }
const inactiveBadge: React.CSSProperties = { background: '#f3f4f6', color: '#6b7280', padding: '1px 7px', borderRadius: 10, fontSize: 11 }
const removeBtn: React.CSSProperties = { background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: 18, padding: '0 4px', lineHeight: 1 }
const addBtn: React.CSSProperties = { padding: '9px 20px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' }
