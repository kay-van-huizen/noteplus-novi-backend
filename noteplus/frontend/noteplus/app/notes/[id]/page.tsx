'use client'
import { use, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import Nav from '../../components/Nav'
import client from '../../../lib/client'

interface Note { id: string; title: string; content: string; categoryTitle?: string; createdAt: string }
interface Attachment { id: string; fileName: string; contentType: string; size: number; createdAt: string }

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function NoteDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const [note, setNote] = useState<Note | null>(null)
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    client.get<Note>(`/notes/${id}`)
      .then(r => setNote(r.data))
      .catch(() => router.push('/notes'))
    loadAttachments()
  }, [id, router])

  function loadAttachments() {
    client.get<Attachment[]>(`/notes/${id}/attachments`)
      .then(r => setAttachments(r.data))
      .catch(() => {})
  }

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) return
    setUploading(true)
    setUploadError('')
    const formData = new FormData()
    formData.append('file', file)
    try {
      await client.post(`/notes/${id}/attachments`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      if (fileRef.current) fileRef.current.value = ''
      loadAttachments()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setUploadError(msg || 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  async function handleDelete(attachmentId: string) {
    if (!confirm('Delete this attachment?')) return
    await client.delete(`/notes/${id}/attachments/${attachmentId}`)
    loadAttachments()
  }

  function handleDownload(attachmentId: string, fileName: string) {
    client.get(`/notes/${id}/attachments/${attachmentId}/download`, { responseType: 'blob' })
      .then(r => {
        const url = URL.createObjectURL(r.data as Blob)
        const a = document.createElement('a')
        a.href = url
        a.download = fileName
        a.click()
        URL.revokeObjectURL(url)
      })
  }

  if (!note) return <div style={pageWrap}><Nav /><div style={content}><p style={muted}>Loading…</p></div></div>

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        {/* Note card */}
        <div style={card}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h1 style={{ margin: '0 0 4px', fontSize: 22 }}>{note.title}</h1>
              {note.categoryTitle && <span style={categoryBadge}>{note.categoryTitle}</span>}
            </div>
            <Link href={`/notes/${id}/edit`}><button style={editBtn}>Edit</button></Link>
          </div>
          <p style={{ marginTop: 20, whiteSpace: 'pre-wrap', lineHeight: 1.7, color: '#374151' }}>{note.content}</p>
        </div>

        {/* Attachments */}
        <div style={{ ...card, marginTop: 20 }}>
          <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>Attachments</h2>

          {attachments.length === 0 && <p style={muted}>No attachments yet.</p>}
          {attachments.map(a => (
            <div key={a.id} style={attachRow}>
              <div>
                <span style={{ fontWeight: 500, fontSize: 14 }}>{a.fileName}</span>
                <span style={{ color: '#9ca3af', fontSize: 12, marginLeft: 8 }}>{formatBytes(a.size)}</span>
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                <button style={dlBtn} onClick={() => handleDownload(a.id, a.fileName)}>Download</button>
                <button style={delBtnSm} onClick={() => handleDelete(a.id)}>Delete</button>
              </div>
            </div>
          ))}

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
const categoryBadge: React.CSSProperties = { background: '#e0e7ff', color: '#3730a3', padding: '2px 8px', borderRadius: 10, fontSize: 12 }
const attachRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #f3f4f6' }
const editBtn: React.CSSProperties = { padding: '6px 16px', background: '#f3f4f6', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13 }
const dlBtn: React.CSSProperties = { padding: '5px 12px', background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: 5, cursor: 'pointer', fontSize: 12 }
const delBtnSm: React.CSSProperties = { padding: '5px 12px', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 5, cursor: 'pointer', fontSize: 12 }
const uploadBtn: React.CSSProperties = { padding: '8px 18px', background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 600 }
