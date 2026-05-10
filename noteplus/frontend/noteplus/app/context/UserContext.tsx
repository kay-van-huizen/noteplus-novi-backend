'use client'
import { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import client from '../../lib/client'

interface MeResponse { id: string; name: string; username: string; roles: string[] }

interface UserContextValue {
  user: MeResponse | null
  loading: boolean
  isAdmin: boolean
}

const UserContext = createContext<UserContextValue>({ user: null, loading: true, isAdmin: false })

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    client.get<MeResponse>('/users/me')
      .then(r => setUser(r.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  return (
    <UserContext.Provider value={{ user, loading, isAdmin: user?.roles.includes('ROLE_ADMIN') ?? false }}>
      {children}
    </UserContext.Provider>
  )
}

export function useUser() {
  return useContext(UserContext)
}
