'use client'
import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import Nav from '../../components/Nav'
import client from '../../../lib/client'

interface LearningPath {
  id: string
  title: string
  description?: string
  studentUsername: string
  coachUsername: string
  notes: { id: string }[]
}

export default function AdminLearningPathsPage() {
  const router = useRouter()
  const [paths, setPaths] = useState<LearningPath[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    client.get<LearningPath[]>('/learning-paths/all')
      .then(r => setPaths(r.data))
      .catch(err => {
        if (err.response?.status === 403) router.push('/notes')
      })
      .finally(() => setLoading(false))
  }, [router])

  return (
    <div style={pageWrap}>
      <Nav />
      <div style={content}>
        <h1 style={{ margin: '0 0 20px', fontSize: 22 }}>All Learning Paths</h1>
        <div style={card}>
          {loading ? (
            <p style={muted}>Loading…</p>
          ) : paths.length === 0 ? (
            <p style={muted}>No learning paths yet.</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
              <thead>
                <tr>
                  {['Name', 'Description', 'Student', 'Coach', 'Notes', ''].map(h => (
                    <th key={h} style={th}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {paths.map(p => (
                  <tr key={p.id}>
                    <td style={td}><strong>{p.title}</strong></td>
                    <td style={{ ...td, color: '#6b7280' }}>{p.description ?? '—'}</td>
                    <td style={td}>{p.studentUsername}</td>
                    <td style={td}>{p.coachUsername}</td>
                    <td style={{ ...td, textAlign: 'center' }}>{p.notes.length}</td>
                    <td style={td}>
                      <Link href={`/admin/learning-paths/${p.id}/edit`}>
                        <button style={editBtn}>Edit</button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}

const pageWrap: React.CSSProperties = { minHeight: '100vh', background: '#f5f5f5' }
const content: React.CSSProperties = { maxWidth: 1000, margin: '0 auto', padding: '32px 16px' }
const card: React.CSSProperties = { background: 'white', borderRadius: 8, padding: '8px 24px', boxShadow: '0 1px 4px rgba(0,0,0,0.1)' }
const muted: React.CSSProperties = { color: '#9ca3af', padding: '16px 0' }
const th: React.CSSProperties = { textAlign: 'left', padding: '14px 12px 14px 0', borderBottom: '2px solid #e5e7eb', fontWeight: 600, color: '#374151' }
const td: React.CSSProperties = { padding: '14px 12px 14px 0', verticalAlign: 'middle', borderBottom: '1px solid #f3f4f6' }
const editBtn: React.CSSProperties = { padding: '5px 14px', background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: 5, fontSize: 13, cursor: 'pointer' }
