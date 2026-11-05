import { useEffect } from 'react'
import {
  Toast, ToastClose, ToastDescription, ToastProvider,
  ToastTitle, ToastViewport,
} from '@/components/ui/toast'
import { useToastState, registerToast } from '@/hooks/useToast'

export function Toaster() {
  const { toasts, addToast, removeToast } = useToastState()

  useEffect(() => {
    registerToast(addToast)
  }, [addToast])

  return (
    <ToastProvider>
      {toasts.map(({ id, title, description, variant }) => (
        <Toast key={id} variant={variant} onOpenChange={(open) => { if (!open) removeToast(id) }}>
          <div>
            <ToastTitle>{title}</ToastTitle>
            {description && <ToastDescription>{description}</ToastDescription>}
          </div>
          <ToastClose />
        </Toast>
      ))}
      <ToastViewport />
    </ToastProvider>
  )
}
