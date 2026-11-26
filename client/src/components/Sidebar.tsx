import { useState, type ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.ts'
import { SidebarSkeleton } from './Skeleton.tsx'
import { usePermissions } from '../auth/usePermissions.ts'

type SidebarProps = {
  /** Mobile drawer visibility */
  open: boolean
  onClose: () => void
  /** Desktop icon-only mode */
  collapsed: boolean
}

function Icon({ children }: { children: ReactNode }) {
  return (
    <svg
      className="h-5 w-5 shrink-0"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {children}
    </svg>
  )
}

type NavItem = { to: string; label: string; end?: boolean; icon: ReactNode }
/** A dropdown: a heading that opens and closes a list of pages. */
type NavGroup = { label: string; icon: ReactNode; children: NavItem[] }
type NavEntry = NavItem | NavGroup
type NavSection = { title: string; items: NavEntry[] }

const isGroup = (entry: NavEntry): entry is NavGroup => 'children' in entry

/** The menu, divided into sections. A section is hidden when the role may open none of its pages. */
const sections: NavSection[] = [
  {
    title: 'Overview',
    items: [
      {
        to: '/',
        label: 'Dashboard',
        end: true,
        icon: (
          <Icon>
            <path d="M4 13h6V4H4zM14 20h6v-9h-6zM4 20h6v-3H4zM14 7h6V4h-6z" />
          </Icon>
        ),
      },
      {
        to: '/analytics',
        label: 'Analytics',
        icon: (
          <Icon>
            <path d="M3 3v18h18M7 15l4-5 3 3 6-7" />
          </Icon>
        ),
      },
      {
        to: '/reports',
        label: 'Reports',
        icon: (
          <Icon>
            <path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8zM14 3v5h5M12 11v6M9 14l3 3 3-3" />
          </Icon>
        ),
      },
      {
        to: '/history',
        label: 'History',
        icon: (
          <Icon>
            <path d="M3 12a9 9 0 109-9 9.75 9.75 0 00-6.74 2.74L3 8M3 3v5h5M12 7v5l4 2" />
          </Icon>
        ),
      },
    ],
  },
  {
    title: 'Catalog',
    items: [
      {
        to: '/products',
        label: 'Products',
        icon: (
          <Icon>
            <path d="M20.6 13.4l-7.2 7.2a2 2 0 01-2.8 0L3 13V3h10l7.6 7.6a2 2 0 010 2.8zM7.5 7.5h.01" />
          </Icon>
        ),
      },
      {
        to: '/categories',
        label: 'Categories',
        icon: (
          <Icon>
            <path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z" />
          </Icon>
        ),
      },
      {
        to: '/units',
        label: 'Units of Measure (How Sold)',
        icon: (
          <Icon>
            <path d="M3 17L17 3l4 4L7 21zM8 12l2 2M11 9l2 2M14 6l2 2" />
          </Icon>
        ),
      },
      {
        to: '/conversions',
        label: 'Unit Conversions',
        icon: (
          <Icon>
            <path d="M7 16V4m0 0L3 8m4-4l4 4M17 8v12m0 0l4-4m-4 4l-4-4" />
          </Icon>
        ),
      },
    ],
  },
  {
    title: 'Warehouse',
    items: [
      {
        to: '/inventory',
        label: 'Inventory',
        icon: (
          <Icon>
            <path d="M21 8l-9-5-9 5 9 5 9-5zM3 8v8l9 5 9-5V8M12 13v8" />
          </Icon>
        ),
      },
      {
        to: '/reorders',
        label: 'Reorders',
        icon: (
          <Icon>
            <path d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" />
          </Icon>
        ),
      },
      {
        to: '/employees',
        label: 'Employees',
        icon: (
          <Icon>
            <path d="M17 21v-2a4 4 0 00-4-4H7a4 4 0 00-4 4v2M10 11a4 4 0 100-8 4 4 0 000 8zM21 21v-2a4 4 0 00-3-3.87M17 3.13a4 4 0 010 7.75" />
          </Icon>
        ),
      },
      {
        label: 'Storage Layout',
        icon: (
          <Icon>
            <path d="M3 9l9-6 9 6v11a1 1 0 01-1 1H4a1 1 0 01-1-1zM9 21v-8h6v8" />
          </Icon>
        ),
        children: [
          {
            to: '/warehouses',
            label: 'Warehouses',
            icon: (
              <Icon>
                <path d="M3 9l9-6 9 6v11a1 1 0 01-1 1H4a1 1 0 01-1-1zM9 21v-8h6v8" />
              </Icon>
            ),
          },
          {
            to: '/aisles',
            label: 'Aisles',
            icon: (
              <Icon>
                <path d="M4 3v18M20 3v18M4 9h16M4 15h16" />
              </Icon>
            ),
          },
          {
            to: '/shelves',
            label: 'Shelves',
            icon: (
              <Icon>
                <path d="M3 4h18M3 12h18M3 20h18M6 4v8M12 12v8M18 4v8" />
              </Icon>
            ),
          },
          {
            to: '/rows',
            label: 'Shelf Rows',
            icon: (
              <Icon>
                <path d="M4 6h16M4 12h16M4 18h16" />
              </Icon>
            ),
          },
          {
            to: '/containers',
            label: 'Containers',
            icon: (
              <Icon>
                <path d="M3 7h18v12H3zM3 7l2-3h14l2 3M9 11v4M15 11v4" />
              </Icon>
            ),
          },
        ],
      },
    ],
  },
  {
    title: 'Sales & Supply',
    items: [
      {
        to: '/orders',
        label: 'Orders',
        icon: (
          <Icon>
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 012-2h2a2 2 0 012 2M9 5h6M9 12h6M9 16h4" />
          </Icon>
        ),
      },
      {
        to: '/customers',
        label: 'Customers',
        icon: (
          <Icon>
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" />
          </Icon>
        ),
      },
      {
        to: '/shipments',
        label: 'Shipments',
        icon: (
          <Icon>
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
          </Icon>
        ),
      },
      {
        to: '/trucks',
        label: 'Trucks',
        icon: (
          <Icon>
            <path d="M1 6h13v10H1zM14 10h4l3 3v3h-7zM6 19a2 2 0 100-4 2 2 0 000 4zM17 19a2 2 0 100-4 2 2 0 000 4z" />
          </Icon>
        ),
      },
      {
        to: '/suppliers',
        label: 'Suppliers',
        icon: (
          <Icon>
            <path d="M3 21h18M5 21V7l8-4v18M19 21V11l-6-4M9 9h.01M9 12h.01M9 15h.01M9 18h.01" />
          </Icon>
        ),
      },
    ],
  },
  {
    title: 'Access Control',
    items: [
      {
        to: '/access/users',
        label: 'Users',
        icon: (
          <Icon>
            <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM19 8v6M22 11h-6" />
          </Icon>
        ),
      },
      {
        to: '/access/roles',
        label: 'Roles & Permissions',
        icon: (
          <Icon>
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10zM9 12l2 2 4-4" />
          </Icon>
        ),
      },
      {
        to: '/access/pages',
        label: 'Page Assignment',
        icon: (
          <Icon>
            <path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8zM14 3v5h5M9 13h6M9 17h4" />
          </Icon>
        ),
      },
    ],
  },
]

export default function Sidebar({ open, onClose, collapsed }: SidebarProps) {
  // Labels are always shown in the mobile drawer; they hide only on desktop when collapsed.
  const hideOnCollapse = collapsed ? 'md:hidden' : ''
  const { canOpen, loaded } = usePermissions()
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
    } finally {
      setLoggingOut(false)
      onClose()
      navigate('/login', { replace: true })
    }
  }

  const { pathname } = useLocation()
  // Dropdowns the user has opened or closed by hand; one that holds the current page is open unless closed.
  const [toggled, setToggled] = useState<Record<string, boolean>>({})

  const visibleSections = sections
    .map((section) => ({
      ...section,
      items: section.items
        .map((entry) =>
          isGroup(entry) ? { ...entry, children: entry.children.filter(({ to }) => canOpen(to)) } : entry,
        )
        .filter((entry) => (isGroup(entry) ? entry.children.length > 0 : canOpen(entry.to))),
    }))
    .filter((section) => section.items.length > 0)

  const linkClass =
    (indent: boolean) =>
    ({ isActive }: { isActive: boolean }) =>
      `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium ${
        collapsed ? 'md:justify-center md:px-0' : ''
      } ${indent && !collapsed ? 'md:pl-9' : ''} ${
        isActive ? 'bg-blue-50 text-blue-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
      }`

  const renderLink = ({ to, label, end, icon }: NavItem, indent = false) => (
    <NavLink
      key={to}
      to={to}
      end={end}
      onClick={onClose}
      title={collapsed ? label : undefined}
      className={linkClass(indent)}
    >
      {icon}
      <span className={hideOnCollapse}>{label}</span>
    </NavLink>
  )

  const renderGroup = (group: NavGroup) => {
    const holdsCurrentPage = group.children.some(({ to }) => pathname === to || pathname.startsWith(`${to}/`))
    const open = toggled[group.label] ?? holdsCurrentPage
    // With the sidebar collapsed to icons there is no room for a heading, so the pages are shown directly.
    const links = group.children.map((child) => renderLink(child, true))
    return (
      <div key={group.label}>
        <button
          type="button"
          onClick={() => setToggled((t) => ({ ...t, [group.label]: !open }))}
          aria-expanded={open}
          className={`flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 ${
            collapsed ? 'md:hidden' : ''
          }`}
        >
          {group.icon}
          <span className="flex-1 text-left">{group.label}</span>
          <svg
            className={`h-4 w-4 shrink-0 transition-transform ${open ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M6 9l6 6 6-6" />
          </svg>
        </button>
        <div className={`space-y-1 ${open ? 'mt-1' : 'hidden'} ${collapsed ? 'md:!mt-0 md:!block' : ''}`}>{links}</div>
      </div>
    )
  }

  return (
    <>
      {/* Mobile backdrop */}
      {open && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 md:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        // Mobile: a full-height drawer. Desktop: stuck to the top of the window at exactly the window's
        // height, so the page scrolls behind it and Log out always stays at the bottom of the screen.
        className={`fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-slate-200 bg-white transition-all duration-200 md:sticky md:top-0 md:bottom-auto md:h-screen md:self-start md:translate-x-0 ${
          collapsed ? 'md:w-16' : 'md:w-64'
        } ${open ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div
          className={`flex h-16 items-center justify-between border-b border-slate-200 px-4 ${
            collapsed ? 'md:justify-center md:px-0' : ''
          }`}
        >
          <div className="flex items-center gap-2.5">
            {/* Logo: a warehouse in a rounded-square badge. */}
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white shadow-sm">
              <svg
                className="h-5 w-5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={1.8}
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M3 9l9-6 9 6v11a1 1 0 01-1 1H4a1 1 0 01-1-1zM9 21v-8h6v8" />
              </svg>
            </span>
            <span className={`text-lg font-bold tracking-tight text-slate-900 ${hideOnCollapse}`}>Stockwise</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close sidebar"
            className="rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 6l12 12M6 18L18 6" />
            </svg>
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto overflow-x-hidden p-3" aria-label="Main">
          {/* Which pages to list is not known until the user's access has loaded. */}
          {!loaded && <SidebarSkeleton iconsOnly={collapsed} />}
          {visibleSections.map((section, index) => (
            <div
              key={section.title}
              role="group"
              aria-labelledby={`nav-section-${index}`}
              // Sections are told apart by their headings and spacing, with no divider lines.
              className={index > 0 ? 'mt-4' : ''}
            >
              <h2
                id={`nav-section-${index}`}
                className={`mb-1 px-3 text-xs font-semibold uppercase tracking-wide text-slate-400 ${hideOnCollapse}`}
              >
                {section.title}
              </h2>
              <div className="space-y-1">
                {section.items.map((entry) => (isGroup(entry) ? renderGroup(entry) : renderLink(entry)))}
              </div>
            </div>
          ))}
        </nav>

        {/* Pinned to the bottom: the nav above is flex-1. */}
        <div className="border-t border-slate-200 p-3">
          <button
            type="button"
            onClick={() => void handleLogout()}
            disabled={loggingOut}
            title={collapsed ? 'Log out' : undefined}
            className={`flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 hover:text-red-700 disabled:opacity-60 ${
              collapsed ? 'md:justify-center md:px-0' : ''
            }`}
          >
            <Icon>
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" />
            </Icon>
            <span className={hideOnCollapse}>{loggingOut ? 'Logging out…' : 'Log out'}</span>
          </button>
        </div>
      </aside>
    </>
  )
}
