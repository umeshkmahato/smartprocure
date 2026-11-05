import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 select-none',
  {
    variants: {
      variant: {
        default:     'bg-blue-600 text-white shadow-sm hover:bg-blue-700 active:bg-blue-800',
        secondary:   'bg-slate-100 text-slate-800 hover:bg-slate-200 active:bg-slate-300 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700',
        outline:     'border border-slate-300 bg-transparent text-slate-700 hover:bg-slate-50 active:bg-slate-100 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800',
        ghost:       'bg-transparent text-slate-700 hover:bg-slate-100 active:bg-slate-200 dark:text-slate-300 dark:hover:bg-slate-800',
        danger:      'bg-red-600 text-white shadow-sm hover:bg-red-700 active:bg-red-800',
        'danger-outline': 'border border-red-300 text-red-700 hover:bg-red-50 active:bg-red-100',
        success:     'bg-emerald-600 text-white shadow-sm hover:bg-emerald-700',
        link:        'text-blue-600 underline-offset-4 hover:underline p-0 h-auto',
      },
      size: {
        xs:   'h-7  px-2.5 text-xs  rounded',
        sm:   'h-8  px-3   text-sm  rounded-md',
        md:   'h-9  px-4   text-sm  rounded-md',
        lg:   'h-10 px-5   text-base rounded-lg',
        xl:   'h-11 px-6   text-base rounded-lg',
        icon: 'h-9  w-9             rounded-md',
        'icon-sm': 'h-7 w-7         rounded',
      },
    },
    defaultVariants: { variant: 'default', size: 'md' },
  },
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
  loading?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, loading, children, disabled, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button'
    return (
      <Comp
        ref={ref}
        className={cn(buttonVariants({ variant, size, className }))}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? (
          <>
            <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            {children}
          </>
        ) : children}
      </Comp>
    )
  },
)
Button.displayName = 'Button'

export { Button, buttonVariants }
