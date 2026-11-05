import * as React from 'react'
import { cn } from '@/lib/utils'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, error, ...props }, ref) => (
    <input
      type={type}
      ref={ref}
      className={cn(
        'flex h-9 w-full rounded-md border bg-white px-3 py-1 text-sm text-slate-900 shadow-xs',
        'placeholder:text-slate-400 transition-colors duration-150',
        'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500',
        'disabled:cursor-not-allowed disabled:opacity-50 disabled:bg-slate-50',
        'dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500',
        error
          ? 'border-red-400 focus:ring-red-500 focus:border-red-500'
          : 'border-slate-300 dark:border-slate-700',
        className,
      )}
      {...props}
    />
  ),
)
Input.displayName = 'Input'

export { Input }
