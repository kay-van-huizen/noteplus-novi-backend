'use client'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import client from '../../lib/client'

export default function Nav() {
  const router = useRouter()

  async function handleLogout() {
    try { await client.post('/auth/logout') } catch {}
    router.push('/login')
  }

  return (
    <nav style={{
      background: '#1e293b', padding: '12px 24px',
      display: 'flex', alignItems: 'center', gap: '24px'
    }}>
      <span style={{ color: 'white', fontWeight: 700, fontSize: 16 }}>NotePlus</span>
      <Link href="/notes" style={linkStyle}>Notes</Link>
      <Link href="/categories" style={linkStyle}>Categories</Link>
      <Link href="/learning-paths" style={linkStyle}>Learning Paths</Link>
      <span style={{ marginLeft: 'auto' }} />
      <Link href="/settings" style={linkStyle}>Settings</Link>
      <button onClick={handleLogout} style={btnStyle}>Logout</button>
    </nav>
  )
}

const linkStyle: React.CSSProperties = { color: '#94a3b8', textDecoration: 'none', fontSize: 14 }
const btnStyle: React.CSSProperties = {
  background: 'none', border: '1px solid #475569', color: '#94a3b8',
  padding: '4px 12px', borderRadius: 4, cursor: 'pointer', fontSize: 14
}
