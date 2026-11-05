import { useState, useCallback } from 'react'

export type ToastVariant = 'default' | 'success' | 'danger' | 'warning' | 'info'

export interface ToastMessage {
  id: string
  title: string
  description?: string
  variant?: ToastVariant
  duration?: number
}

let globalToastFn: ((toast: Omit<ToastMessage, 'id'>) => void) | null = null

export function registerToast(fn: (toast: Omit<ToastMessage, 'id'>) => void) {
  globalToastFn = fn
}

export function toast(message: Omit<ToastMessage, 'id'>) {
  if (globalToastFn) globalToastFn(message)
}

export function useToastState() {
  const [toasts, setToasts] = useState<ToastMessage[]>([])

  const addToast = useCallback((message: Omit<ToastMessage, 'id'>) => {
    const id = Math.random().toString(36).slice(2)
    setToasts((prev) => [...prev, { ...message, id }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, message.duration ?? 4000)
  }, [])

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return { toasts, addToast, removeToast }
}
