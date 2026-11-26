import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.ts'
import { usePermissions } from '../auth/usePermissions.ts'
import NotificationBell from './NotificationBell.tsx'

type NavbarProps = {
  /** Opens the mobile drawer */
  onMenuClick: () => void
  /** Collapses / expands the desktop sidebar */
  onToggleCollapse: () => void
  collapsed: boolean
}

function MenuIcon() {
  return (
    <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
    </svg>
  )
}

export default function Navbar({ onMenuClick, onToggleCollapse, collapsed }: NavbarProps) {
  const buttonClass = 'rounded-md p-2 text-slate-600 hover:bg-slate-100'
  const { role } = usePermissions()
  const { user } = useAuth()
  const fullName = user ? `${user.firstName} ${user.lastName}`.trim() : ''
  const initial = (user?.firstName || user?.email || 'U').charAt(0).toUpperCase()
  const navigate = useNavigate()
  const [term, setTerm] = useState('')

  function submitSearch(event: FormEvent) {
    event.preventDefault()
    const query = term.trim()
    if (query) navigate(`/search?q=${encodeURIComponent(query)}`)
  }

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onMenuClick}
          aria-label="Open sidebar"
          className={`${buttonClass} md:hidden`}
        >
          <MenuIcon />
        </button>
        <button
          type="button"
          onClick={onToggleCollapse}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          aria-expanded={!collapsed}
          className={`${buttonClass} hidden md:block`}
        >
          <MenuIcon />
        </button>
      </div>

      <div className="flex items-center gap-3">
        {role && (
          <span
            className="hidden rounded-full bg-blue-50 px-3 py-1 text-xs font-medium capitalize text-blue-700 sm:inline"
            title="Your role"
          >
            {role}
          </span>
        )}
        <form onSubmit={submitSearch} role="search" className="hidden sm:block">
          <input
            type="search"
            value={term}
            onChange={(e) => setTerm(e.target.value)}
            aria-label="Search"
            placeholder="Search…"
            className="w-56 rounded-md border border-slate-300 px-3 py-1.5 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
        </form>
        {/* Small screens have no room for the box: this opens the Search page, which has its own. */}
        <Link to="/search" aria-label="Search" className={`${buttonClass} sm:hidden`}>
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M11 18a7 7 0 100-14 7 7 0 000 14z" />
          </svg>
        </Link>
        <NotificationBell />
        <div
          className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-600 text-sm font-medium text-white"
          aria-label={fullName ? `Signed in as ${fullName}` : 'User menu'}
          title={fullName || undefined}
        >
          {initial}
        </div>
      </div>
    </header>
  )
}
