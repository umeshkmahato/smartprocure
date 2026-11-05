import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Allowed sort fields – mirrors the server-side SortValidator whitelist.
// Any value outside this set is rejected before it ever reaches JPA.
// ---------------------------------------------------------------------------
const SORT_FIELDS = new Set([
  'name',
  'email',
  'country',
  'category',
  'status',
  'createdAt',
])

// ---------------------------------------------------------------------------
// Validation helper (client-side mirror of SortValidator.validate)
// ---------------------------------------------------------------------------
function validateSortField(sort: string | undefined): void {
  if (!sort) return // no sort param → nothing to validate
  // Strip any direction suffix (e.g. "name,asc" → "name")
  const field = sort.split(',')[0].trim()
  if (!SORT_FIELDS.has(field)) {
    throw new Error(
      `Invalid sort field '${field}'. Allowed values: ${[...SORT_FIELDS].join(', ')}.`
    )
  }
}

interface Props {
  /** 0-based current page index (matches Spring's Page.number) */
  page: number
  totalPages: number
  totalElements: number
  size: number
  /**
   * Optional sort parameter forwarded to the server.
   * Format: "<field>" or "<field>,<asc|desc>".
   * Must be one of: name, email, country, category, status, createdAt.
   * An invalid value is rejected client-side (HTTP 400 equivalent) before
   * the request is ever sent, mirroring the server-side SortValidator flow:
   *
   *   Client sends: GET /api/v1/vendors?sort=; DROP TABLE vendors--
   *                                      ↓
   *   SortValidator.validate(sort, SORT_FIELDS)
   *     → field not in {"name","email","country","category","status","createdAt"}
   *     → throws BusinessException(VALIDATION_ERROR, "Invalid sort field '…'")
   *     → GlobalExceptionHandler returns HTTP 400 (never reaches JPA)
   */
  sort?: string
  onPageChange: (page: number, sort?: string) => void
}

/**
 * Full-featured server-side pagination bar.
 * Shows: First | Prev | page window | Next | Last
 * Plus a "Showing X – Y of Z" summary.
 *
 * Sort field validation is enforced client-side before any page-change
 * callback fires, preventing injection payloads from reaching the server.
 */
export default function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  sort,
  onPageChange,
}: Props) {
  if (totalPages <= 1) return null

  const from = page * size + 1
  const to = Math.min((page + 1) * size, totalElements)

  /** Build the sliding window of page numbers (max 5 shown) */
  const buildWindow = () => {
    const half = 2
    let start = Math.max(0, page - half)
    let end = Math.min(totalPages - 1, page + half)
    if (end - start < 4) {
      if (start === 0) end = Math.min(4, totalPages - 1)
      else start = Math.max(0, end - 4)
    }
    const pages: number[] = []
    for (let i = start; i <= end; i++) pages.push(i)
    return pages
  }

  /**
   * Validated page-change handler.
   * Validates the sort field before invoking the callback, mirroring the
   * server-side SortValidator so that an invalid sort string is caught
   * immediately on the client (never sent to the API).
   */
  const handlePageChange = (targetPage: number) => {
    try {
      validateSortField(sort)
    } catch (err) {
      // Surface the validation error to the developer / UI layer.
      // In a real app you would dispatch this to your error/toast system.
      console.error('[Pagination] Sort validation failed:', (err as Error).message)
      return // abort – do not call onPageChange with an invalid sort value
    }
    onPageChange(targetPage, sort)
  }

  const btnBase =
    'inline-flex items-center justify-center h-8 w-8 rounded-md text-sm transition-colors disabled:opacity-40 disabled:cursor-not-allowed'
  const btnIdle = 'border border-slate-200 bg-white hover:bg-slate-50 text-slate-600'
  const btnActive = 'bg-blue-600 text-white font-semibold border border-blue-600'

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-2">
      {/* Summary */}
      <p className="text-xs text-slate-500">
        Showing <span className="font-medium text-slate-700">{from}</span>–
        <span className="font-medium text-slate-700">{to}</span> of{' '}
        <span className="font-medium text-slate-700">{totalElements}</span> records
      </p>

      {/* Controls */}
      <div className="flex items-center gap-1">
        {/* First */}
        <button
          disabled={page === 0}
          onClick={() => handlePageChange(0)}
          className={cn(btnBase, btnIdle)}
          title="First page"
        >
          <ChevronsLeft size={14} />
        </button>

        {/* Prev */}
        <button
          disabled={page === 0}
          onClick={() => handlePageChange(page - 1)}
          className={cn(btnBase, btnIdle)}
          title="Previous page"
        >
          <ChevronLeft size={14} />
        </button>

        {/* Page window */}
        {buildWindow().map((p) => (
          <button
            key={p}
            onClick={() => handlePageChange(p)}
            className={cn(btnBase, p === page ? btnActive : btnIdle)}
          >
            {p + 1}
          </button>
        ))}

        {/* Next */}
        <button
          disabled={page === totalPages - 1}
          onClick={() => handlePageChange(page + 1)}
          className={cn(btnBase, btnIdle)}
          title="Next page"
        >
          <ChevronRight size={14} />
        </button>

        {/* Last */}
        <button
          disabled={page === totalPages - 1}
          onClick={() => handlePageChange(totalPages - 1)}
          className={cn(btnBase, btnIdle)}
          title="Last page"
        >
          <ChevronsRight size={14} />
        </button>
      </div>
    </div>
  )
}
