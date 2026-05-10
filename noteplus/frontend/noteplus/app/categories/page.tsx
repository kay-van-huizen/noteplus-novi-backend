'use client'
import { useEffect, useState } from 'react'
import Link from 'next/link'
import Nav from '../components/Nav'
import { UserProvider, useUser } from '../context/UserContext'
import client from '../../lib/client'

interface Category { id: string; title: string; color: string; status: string; parentId?: string; parentTitle?: string }

const COLORS = ['DEFAULT', 'PINK', 'RED', 'ORANGE', 'YELLOW', 'GREEN', 'TEAL', 'BLUE', 'PURPLE', 'GRAY']

function CategoriesContent() {
  const { isAdmin } = useUser()
  const [categories, setCategories] = useState<Category[]>([])
  const [title, setTitle] = useState('')
  const [color, setColor] = useState('DEFAULT')
  const [error, setError] = useState('')

  function load() {
    client.get<Category[]>('/categories').then(r => setCategories(r.data)).catch(() => {})
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

  async function handleToggleStatus(cat: Category) {
    const newStatus = cat.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await client.patch(`/categories/${cat.id}`, { status: newStatus })
      load()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      alert(msg || 'Failed to update status')
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this category?')) return
    try { await client.delete(`/categories/${id}`); load() }
    catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      alert(msg || 'Cannot delete — may have notes or subcategories attached')
    }
  }

  // Group: roots first, then children indented
  const roots = categories.filter(c => !c.parentId)
  const children = categories.filter(c => !!c.parentId)
  const ordered: Array<Category & { indent: boolean }> = []
  for (const root of roots) {
    ordered.push({ ...root, indent: false })
    for (const child of children.filter(c => c.parentId === root.id)) {
      ordered.push({ ...child, indent: true })
    }
  }
  // Any orphaned children (parentId set but parent not in list)
  const seenIds = new Set(ordered.map(c => c.id))
  for (const child of children.filter(c => !seenIds.has(c.id))) {
    ordered.push({ ...child, indent: true })
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        {isAdmin && <p style={adminBadge}>All categories (Admin view)</p>}

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
          {ordered.length === 0 && <p style={muted}>No categories yet.</p>}
          {ordered.map(c => (
            <div key={c.id} style={{ ...row, opacity: c.status === 'INACTIVE' ? 0.55 : 1, marginLeft: c.indent ? 24 : 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                {c.indent && <span style={{ color: '#9ca3af', fontSize: 12 }}>↳</span>}
                <span style={{ fontWeight: 500, fontSize: 14 }}>{c.title}</span>
                {c.status === 'INACTIVE' && <span style={inactiveBadge}>Inactive</span>}
                <span style={colorDot(c.color)} title={c.color} />
              </div>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <button style={toggleBtn(c.status)} onClick={() => handleToggleStatus(c)}>
                  {c.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                </button>
                <Link href={`/categories/${c.id}`}><button style={editBtnSm}>Edit</button></Link>
                <button style={delBtn} onClick={() => handleDelete(c.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function CategoriesPage() {
  return (
    <UserProvider>
      <CategoriesContent />
    </UserProvider>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 760, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '24px 28px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const inp: React.CSSProperties = { padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, fontFamily: 'inherit', boxSizing: 'border-box' }
const errorBox: React.CSSProperties = { background: '#fef2f2', color: '#dc2626', padding: '10px 14px', borderRadius: 6, fontSize: 13, marginBottom: 12 }
const addBtn: React.CSSProperties = { padding: '9px 20px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' }
const muted: React.CSSProperties = { color: '#9ca3af', fontSize: 14 }
const row: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid #f3f4f6', transition: 'opacity 0.2s' }
const inactiveBadge: React.CSSProperties = { background: '#f3f4f6', color: '#6b7280', padding: '1px 7px', borderRadius: 10, fontSize: 11 }
const colorDot = (c: string): React.CSSProperties => ({ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: c === 'DEFAULT' ? '#d1d5db' : c.toLowerCase(), border: '1px solid #e5e7eb' })
const toggleBtn = (status: string): React.CSSProperties => ({ padding: '4px 10px', background: status === 'ACTIVE' ? '#fef9c3' : '#dcfce7', color: status === 'ACTIVE' ? '#92400e' : '#166534', border: 'none', borderRadius: 5, fontSize: 12, cursor: 'pointer' })
const editBtnSm: React.CSSProperties = { padding: '4px 10px', background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: 5, fontSize: 12, cursor: 'pointer' }
const delBtn: React.CSSProperties = { padding: '4px 10px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, fontSize: 12, cursor: 'pointer' }
const adminBadge: React.CSSProperties = { background: '#fef3c7', color: '#92400e', padding: '8px 14px', borderRadius: 6, fontSize: 13, marginBottom: 16, display: 'inline-block' }
