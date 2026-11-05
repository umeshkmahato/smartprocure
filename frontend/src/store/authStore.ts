import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  username: string | null
  email: string | null
  roles: string[]
  setAuth: (payload: {
    accessToken: string
    refreshToken: string
    username: string
    email: string
    roles: string[]
  }) => void
  clear: () => void
  hasRole: (role: string) => boolean
  isAdmin: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      username: null,
      email: null,
      roles: [],
      setAuth: (payload) => set({ ...payload }),
      clear: () =>
        set({ accessToken: null, refreshToken: null, username: null, email: null, roles: [] }),
      hasRole: (role) => get().roles.includes(role),
      isAdmin: () => get().roles.includes('ROLE_ADMIN'),
    }),
    { name: 'sp-auth' },
  ),
)
