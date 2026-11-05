import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface ThemeStore {
  theme: 'light' | 'dark'
  toggle: () => void
  setTheme: (t: 'light' | 'dark') => void
}

export const useThemeStore = create<ThemeStore>()(
  persist(
    (set, get) => ({
      theme: 'light',
      toggle: () => {
        const next = get().theme === 'light' ? 'dark' : 'light'
        document.documentElement.classList.toggle('dark', next === 'dark')
        set({ theme: next })
      },
      setTheme: (t) => {
        document.documentElement.classList.toggle('dark', t === 'dark')
        set({ theme: t })
      },
    }),
    { name: 'sp-theme' },
  ),
)

/** Call once on app boot to restore persisted theme */
export function initTheme() {
  const stored = localStorage.getItem('sp-theme')
  if (stored) {
    try {
      const { state } = JSON.parse(stored)
      if (state?.theme === 'dark') document.documentElement.classList.add('dark')
    } catch {}
  }
}
