import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react'
import { cn } from '@/lib/utils'

interface Props {
  label: string
  field: string
  currentSort: string
  currentDir: 'asc' | 'desc'
  onSort: (field: string) => void
  className?: string
}

/**
 * A <th> that shows sort direction indicators and calls onSort when clicked.
 * Unsorted columns show a neutral double-arrow; sorted columns show the
 * active direction with a blue tint.
 */
export default function SortableHeader({
  label, field, currentSort, currentDir, onSort, className,
}: Props) {
  const active = currentSort === field

  return (
    <th
      className={cn(
        'px-5 py-3 text-left text-xs font-medium uppercase cursor-pointer select-none',
        'transition-colors',
        active ? 'text-blue-600 bg-blue-50' : 'text-slate-500 hover:bg-slate-100',
        className,
      )}
      onClick={() => onSort(field)}
    >
      <span className="inline-flex items-center gap-1">
        {label}
        {active ? (
          currentDir === 'asc'
            ? <ChevronUp size={13} className="text-blue-600" />
            : <ChevronDown size={13} className="text-blue-600" />
        ) : (
          <ChevronsUpDown size={13} className="text-slate-400" />
        )}
      </span>
    </th>
  )
}
