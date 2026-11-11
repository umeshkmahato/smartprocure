import type { PageResponse } from '@/types'

/** Simulated network delay */
export const delay = (ms = 350) => new Promise<void>((r) => setTimeout(r, ms))

/** Paginate, sort, and optionally filter an in-memory array */
export function paginate<T>(
  items: T[],
  opts: {
    page?: number
    size?: number
    sort?: string
    dir?: string
    filter?: (item: T) => boolean
  } = {},
): PageResponse<T> {
  const { page = 0, size = 10, sort, dir = 'asc', filter } = opts
  let result = filter ? items.filter(filter) : [...items]

  if (sort) {
    result = result.sort((a, b) => {
      const av = (a as Record<string, unknown>)[sort]
      const bv = (b as Record<string, unknown>)[sort]
      if (av == null) return 1
      if (bv == null) return -1
      const cmp = String(av).localeCompare(String(bv), undefined, { numeric: true })
      return dir === 'desc' ? -cmp : cmp
    })
  }

  const totalElements = result.length
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  const content = result.slice(page * size, page * size + size)

  return {
    content,
    totalElements,
    totalPages,
    number: page,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
  }
}

/** Next auto-increment ID */
export const nextId = <T extends { id: number }>(items: T[]): number =>
  items.length ? Math.max(...items.map((i) => i.id)) + 1 : 1

export const now = () => new Date().toISOString()
export const daysAgo = (n: number) =>
  new Date(Date.now() - n * 86_400_000).toISOString()
export const daysFromNow = (n: number) =>
  new Date(Date.now() + n * 86_400_000).toISOString()

/** Keyword match across given string fields */
export function matchesKeyword<T>(
  item: T,
  keyword: string | undefined,
  fields: (keyof T)[],
): boolean {
  if (!keyword) return true
  const kw = keyword.toLowerCase()
  return fields.some((f) => String(item[f] ?? '').toLowerCase().includes(kw))
}
