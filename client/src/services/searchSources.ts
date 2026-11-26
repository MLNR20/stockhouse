import { formatDate, humanize, shortId } from '../utils/format.ts'

/** A record from any list; only the id is guaranteed. */
export type SearchRow = Record<string, unknown> & { id: string }

/** Names for the ids that records point at. A missing lookup just leaves the name out. */
export type SearchNames = {
  product: ReadonlyMap<string, string>
  supplier: ReadonlyMap<string, string>
  customer: ReadonlyMap<string, string>
}

export type SearchSource = {
  /** The page these records live on: a source is searched only if the user may open it. */
  page: string
  label: string
  /** GET {resource}/page?search= */
  resource: string
  title: (row: SearchRow, names: SearchNames) => string
  detail?: (row: SearchRow, names: SearchNames) => string
  /** Where a result leads. `term` is what was typed. */
  href: (row: SearchRow, term: string, names: SearchNames) => string
}

const text = (row: SearchRow, key: string) => {
  const value = row[key]
  return typeof value === 'string' || typeof value === 'number' ? String(value) : ''
}
const fullName = (row: SearchRow) => `${text(row, 'firstName')} ${text(row, 'lastName')}`.trim()
const nameOf = (map: ReadonlyMap<string, string>, row: SearchRow, key: string) => map.get(text(row, key)) ?? ''
const join = (...parts: string[]) => parts.filter(Boolean).join(' · ')
/** A list page with its search box already filled in. */
const list = (page: string, query: string) => `${page}?q=${encodeURIComponent(query)}`

/** A source whose records are found by name on their own list page. */
const named = (page: string, label: string, resource: string, key = 'name'): SearchSource => ({
  page,
  label,
  resource,
  title: (r) => text(r, key),
  href: (r) => list(page, text(r, key)),
})

export const SEARCH_SOURCES: SearchSource[] = [
  {
    ...named('/products', 'Products', '/api/products'),
    detail: (r) => join(text(r, 'categoryName'), text(r, 'basePrice')),
  },
  {
    page: '/orders',
    label: 'Orders',
    resource: '/api/orders',
    title: (r) => `Order #${shortId(r.id)}`,
    detail: (r, n) =>
      join(nameOf(n.customer, r, 'customerId'), humanize(text(r, 'status')), formatDate(text(r, 'targetDeliveryDate'))),
    href: (r) => `/orders/${r.id}`,
  },
  {
    page: '/reorders',
    label: 'Reorders',
    resource: '/api/reorders',
    title: (r, n) => `Reorder of ${nameOf(n.product, r, 'productId') || 'a product'}`,
    detail: (r, n) => join(nameOf(n.supplier, r, 'supplierId'), humanize(text(r, 'status'))),
    href: (r) => `/reorders/${r.id}`,
  },
  {
    page: '/customers',
    label: 'Customers',
    resource: '/api/customers',
    title: fullName,
    detail: (r) => join(text(r, 'email'), text(r, 'phoneNumber')),
    href: (r) => list('/customers', text(r, 'email')),
  },
  {
    ...named('/suppliers', 'Suppliers', '/api/suppliers', 'supplierName'),
    detail: (r) => join(text(r, 'email'), text(r, 'phoneNumber')),
  },
  {
    page: '/inventory',
    label: 'Inventory',
    resource: '/api/inventory',
    title: (r, n) => nameOf(n.product, r, 'productId') || 'Inventory record',
    detail: (r) => join(humanize(text(r, 'status')), `${text(r, 'baseQuantity')} on hand`),
    href: (r, term, n) => list('/inventory', nameOf(n.product, r, 'productId') || term),
  },
  {
    page: '/shipments',
    label: 'Shipments',
    resource: '/api/shipments',
    title: (r) => `Shipment on ${formatDate(text(r, 'scheduledDate'))}`,
    detail: (r) => humanize(text(r, 'status')),
    href: (_, term) => list('/shipments', term),
  },
  { ...named('/trucks', 'Trucks', '/api/trucks', 'plateNumber'), detail: (r) => text(r, 'model') },
  {
    page: '/employees',
    label: 'Employees',
    resource: '/api/warehouse-employees',
    title: fullName,
    detail: (r) => text(r, 'email'),
    href: (r) => list('/employees', text(r, 'email')),
  },
  { ...named('/categories', 'Categories', '/api/categories'), detail: (r) => text(r, 'description') },
  named('/units', 'Units of Measure', '/api/how-sold'),
  named('/warehouses', 'Warehouses', '/api/warehouses'),
  named('/aisles', 'Aisles', '/api/aisles'),
  named('/shelves', 'Shelves', '/api/shelves'),
  named('/rows', 'Shelf Rows', '/api/rows'),
  named('/containers', 'Containers', '/api/containers'),
  {
    page: '/access/users',
    label: 'Users',
    resource: '/api/users',
    title: fullName,
    detail: (r) => join(text(r, 'email'), text(r, 'roleName')),
    href: (r) => list('/access/users', text(r, 'email')),
  },
]
