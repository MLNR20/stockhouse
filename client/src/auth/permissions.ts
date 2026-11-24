/**
 * What can be granted. Which role holds what is not decided here: it is stored on the server (Roles & Permissions,
 * Page Assignment) and arrives with GET /api/auth/me (see accessStore.ts).
 */
export const RESOURCES = [
  'dashboard',
  'inventory',
  'units',
  'orders',
  'suppliers',
  'products',
  'categories',
  'warehouses',
  'aisles',
  'shelves',
  'trucks',
  'shipments',
  'employees',
  'reorders',
  'history',
  'users',
  'roles',
  'pages',
] as const
export const ACTIONS = ['view', 'create', 'update', 'delete'] as const

export type Resource = (typeof RESOURCES)[number]
export type Action = (typeof ACTIONS)[number]
/** e.g. 'inventory:update' */
export type Permission = `${Resource}:${Action}`

/** Sidebar order; the first one a user may open is where they land when the current page is not allowed. */
export const PAGE_ORDER = [
  '/',
  '/inventory',
  '/orders',
  '/reorders',
  '/shipments',
  '/products',
  '/categories',
  '/units',
  '/conversions',
  '/warehouses',
  '/aisles',
  '/shelves',
  '/trucks',
  '/suppliers',
  '/employees',
  '/history',
  '/access/users',
  '/access/roles',
  '/access/pages',
] as const

/** Whether a pathname falls under an assigned page path ('/orders' covers '/orders/123'). */
export function pathMatches(page: string, pathname: string): boolean {
  return page === '/' ? pathname === '/' : pathname === page || pathname.startsWith(`${page}/`)
}

/** Page assigned to the role that covers the pathname, or undefined if the app has no such page. */
export function matchPage(pathname: string): string | undefined {
  return [...PAGE_ORDER].filter((p) => pathMatches(p, pathname)).sort((a, b) => b.length - a.length)[0]
}

/** First page the pages list allows; used as the redirect target when access is lost. */
export function firstAllowedPath(pages: ReadonlySet<string>): string {
  return PAGE_ORDER.find((p) => pages.has(p)) ?? '/forbidden'
}
