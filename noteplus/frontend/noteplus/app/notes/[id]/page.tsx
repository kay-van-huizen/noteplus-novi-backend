'use client'
import { use, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import Nav from '../../components/Nav'
import client from '../../../lib/client'

interface Note {
  id: string
  title: string
  content: string
  ownerUsername: string
  categoryTitle?: string
  createdAt: string
  updatedAt: string
}
interface FileAttachment { id: string; fileName: string; contentType: string; size: number }
interface Reference { id: string; title: string; fileAttachment: FileAttachment | null }

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export default function NoteDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const [note, setNote] = useState<Note | null>(null)
  const [references, setReferences] = useState<Reference[]>([])
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    client.get<Note>(`/notes/${id}`)
      .then(r => setNote(r.data))
      .catch(err => {
        if (err.response?.status === 404 || err.response?.status === 403) {
          setNotFound(true)
        } else {
          router.push('/notes')
        }
      })
      .finally(() => setLoading(false))
    loadAttachments()
  }, [id, router])

  function loadAttachments() {
    client.get<Reference[]>(`/notes/${id}/references`)
      .then(r => setReferences(r.data.filter(ref => ref.fileAttachment != null)))
      .catch(() => {})
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) return
    setUploading(true)
    setUploadError('')
    try {
      const refRes = await client.post(`/notes/${id}/references`, { title: 'Attachment' })
      const fd = new FormData()
      fd.append('file', file)
      await client.post(`/notes/${id}/references/${refRes.data.id}/attachment`, fd)
      if (fileRef.current) fileRef.current.value = ''
      loadAttachments()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setUploadError(msg || 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  async function handleDeleteRef(referenceId: string) {
    if (!confirm('Delete this attachment?')) return
    await client.delete(`/notes/${id}/references/${referenceId}`)
    loadAttachments()
  }

  function handleDownload(referenceId: string, fileName: string) {
    client.get(`/notes/${id}/references/${referenceId}/attachment`, { responseType: 'blob' })
      .then(r => {
        const url = URL.createObjectURL(r.data as Blob)
        const a = document.createElement('a')
        a.href = url
        a.download = fileName
        a.click()
        URL.revokeObjectURL(url)
      })
  }

  if (loading) {
    return (
      <div style={pageWrap}>
        <Nav />
        <div style={content}><p style={muted}>Loading…</p></div>
      </div>
    )
  }

  if (notFound || !note) {
    return (
      <div style={pageWrap}>
        <Nav />
        <div style={content}>
          <div style={card}>
            <p style={{ fontSize: 16, fontWeight: 600, margin: '0 0 8px', color: '#374151' }}>Note not found</p>
            <p style={{ ...muted, margin: '0 0 16px' }}>
              This note may have been deleted or you do not have access to it.
            </p>
            <Link href="/notes" style={{ fontSize: 13, color: '#2563eb' }}>← Back to notes</Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        {/* Note card */}
        <div style={card}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <h1 style={{ margin: '0 0 6px', fontSize: 22, wordBreak: 'break-word' }}>{note.title}</h1>
              <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8 }}>
                {note.categoryTitle && <span style={categoryBadge}>{note.categoryTitle}</span>}
              </div>
              <p style={meta}>
                By <strong>{note.ownerUsername}</strong>
                <span style={metaDot}>·</span>
                Created {formatDate(note.createdAt)}
                {note.updatedAt !== note.createdAt && (
                  <>
                    <span style={metaDot}>·</span>
                    Updated {formatDate(note.updatedAt)}
                  </>
                )}
              </p>
            </div>
            <Link href={`/notes/${id}/edit`} style={{ marginLeft: 16, flexShrink: 0 }}>
              <button style={editBtn}>Edit</button>
            </Link>
          </div>

          <div style={divider} />
          <p style={{ whiteSpace: 'pre-wrap', lineHeight: 1.75, color: '#374151', margin: 0 }}>
            {note.content}
          </p>
        </div>

        {/* Attachments */}
        <div style={{ ...card, marginTop: 20 }}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>Attachments</h2>

          {references.length === 0 && <p style={muted}>No attachments yet.</p>}
          {references.map(ref => {
            const fa = ref.fileAttachment!
            return (
              <div key={ref.id} style={attachRow}>
                <div>
                  <span style={{ fontWeight: 500, fontSize: 14 }}>{fa.fileName}</span>
                  <span style={{ color: '#9ca3af', fontSize: 12, marginLeft: 8 }}>{formatBytes(fa.size)}</span>
                </div>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button style={dlBtn} onClick={() => handleDownload(ref.id, fa.fileName)}>
                    Download attachment
                  </button>
                  <button style={delBtnSm} onClick={() => handleDeleteRef(ref.id)}>Delete</button>
                </div>
              </div>
            )
          })}

          <form onSubmit={handleUpload} style={{ marginTop: 16, display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <input ref={fileRef} type="file" style={{ fontSize: 13 }} required />
            <button style={uploadBtn} type="submit" disabled={uploading}>
              {uploading ? 'Uploading…' : 'Upload'}
            </button>
          </form>
          {uploadError && <p style={{ color: '#dc2626', fontSize: 13, marginTop: 8 }}>{uploadError}</p>}
        </div>

        <div style={{ marginTop: 12 }}>
          <Link href="/notes" style={{ fontSize: 13, color: '#6b7280' }}>← Back to notes</Link>
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 760, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '24px 28px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const muted: React.CSSProperties = { color: '#9ca3af', fontSize: 14 }
const meta: React.CSSProperties = { color: '#6b7280', fontSize: 13, margin: '8px 0 0' }
const metaDot: React.CSSProperties = { margin: '0 6px', color: '#d1d5db' }
const divider: React.CSSProperties = { height: 1, background: '#f3f4f6', margin: '20px 0' }
const categoryBadge: React.CSSProperties = { background: '#e0e7ff', color: '#3730a3', padding: '2px 8px', borderRadius: 10, fontSize: 12 }
const attachRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #f3f4f6' }
const editBtn: React.CSSProperties = { padding: '6px 16px', background: '#f3f4f6', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13 }
const dlBtn: React.CSSProperties = { padding: '5px 12px', background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: 5, cursor: 'pointer', fontSize: 12 }
const delBtnSm: React.CSSProperties = { padding: '5px 12px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, cursor: 'pointer', fontSize: 12 }
const uploadBtn: React.CSSProperties = { padding: '8px 18px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 600 }
