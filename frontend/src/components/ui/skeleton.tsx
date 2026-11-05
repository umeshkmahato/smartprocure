import { cn } from '@/lib/utils'

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'line' | 'circle' | 'rect'
}

function Skeleton({ className, variant = 'rect', ...props }: SkeletonProps) {
  return (
    <div
      className={cn(
        'animate-pulse bg-slate-200 dark:bg-slate-800',
        variant === 'circle' && 'rounded-full',
        variant === 'line'   && 'rounded h-4',
        variant === 'rect'   && 'rounded-lg',
        className,
      )}
      {...props}
    />
  )
}

/** Full table row skeleton — pass rowCount to render multiple */
function TableSkeleton({ rows = 6, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
      {Array.from({ length: rows }).map((_, r) => (
        <tr key={r}>
          {Array.from({ length: cols }).map((_, c) => (
            <td key={c} className="px-5 py-3.5">
              <Skeleton variant="line" className={c === 0 ? 'w-24' : c === cols - 1 ? 'w-16' : 'w-32'} />
            </td>
          ))}
        </tr>
      ))}
    </tbody>
  )
}

/** KPI card skeleton */
function KpiSkeleton() {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-start justify-between mb-4">
        <Skeleton variant="rect" className="h-9 w-9" />
        <Skeleton variant="line" className="w-12 h-5" />
      </div>
      <Skeleton variant="line" className="w-20 h-8 mb-2" />
      <Skeleton variant="line" className="w-32 h-4" />
    </div>
  )
}

/** Page header skeleton */
function PageHeaderSkeleton() {
  return (
    <div className="flex items-center justify-between mb-6">
      <div className="space-y-2">
        <Skeleton variant="line" className="w-48 h-7" />
        <Skeleton variant="line" className="w-32 h-4" />
      </div>
      <Skeleton variant="rect" className="w-32 h-9 rounded-lg" />
    </div>
  )
}

export { Skeleton, TableSkeleton, KpiSkeleton, PageHeaderSkeleton }
