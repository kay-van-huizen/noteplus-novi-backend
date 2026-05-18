'use client'
import { useEffect, useState } from 'react'
import Link from 'next/link'
import Nav from '../components/Nav'
import client from '../../lib/client'

interface Note { id: string; title: string; content: string; categoryTitle?: string }

export default function NotesPage() {
  const [notes, setNotes] = useState<Note[]>([])
  const [loading, setLoading] = useState(true)

  function load() {
    client.get('/notes').then(r => setNotes(r.data)).catch(() => {}).finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function deleteNote(id: string) {
    if (!confirm('Delete this note?')) return
    await client.delete(`/notes/${id}`)
    setNotes(n => n.filter(x => x.id !== id))
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h1 style={{ margin: 0, fontSize: 22 }}>Notes</h1>
          <Link href="/notes/new"><button style={addBtn}>+ New note</button></Link>
        </div>
        <div style={card}>
          {loading ? <p style={muted}>Loading...</p> :
           notes.length === 0 ? <p style={muted}>No notes yet. Create your first one.</p> :
           notes.map(note => (
            <div key={note.id} style={row}>
              <div>
                <Link href={`/notes/${note.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                  <strong style={{ fontSize: 15 }}>{note.title}</strong>
                </Link>
                {note.categoryTitle && <span style={badge}>{note.categoryTitle}</span>}
                <div style={{ color: '#6b7280', fontSize: 13, marginTop: 2 }}>
                  {note.content?.substring(0, 100)}{(note.content?.length ?? 0) > 100 ? '…' : ''}
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8, marginLeft: 16 }}>
                <Link href={`/notes/${note.id}`}><button style={viewBtn}>View</button></Link>
                <Link href={`/notes/${note.id}/edit`}><button style={editBtn}>Edit</button></Link>
                <button style={delBtn} onClick={() => deleteNote(note.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 760, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '8px 24px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const muted: React.CSSProperties = { color: '#9ca3af', padding: '16px 0' }
const row: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', padding: '16px 0', borderBottom: '1px solid #f3f4f6' }
const badge: React.CSSProperties = { marginLeft: 8, background: '#e0e7ff', color: '#3730a3', padding: '2px 8px', borderRadius: 10, fontSize: 11 }
const addBtn: React.CSSProperties = { padding: '9px 18px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 600, cursor: 'pointer' }
const viewBtn: React.CSSProperties = { padding: '6px 14px', background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer' }
const editBtn: React.CSSProperties = { padding: '6px 14px', background: '#f3f4f6', color: '#374151', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer' }
const delBtn: React.CSSProperties = { padding: '6px 14px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer' }
