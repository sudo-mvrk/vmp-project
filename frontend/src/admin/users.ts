import type { AdminUser, AuthorityRole } from '../api/types'

export const PAGE_SIZE = 20

export type UserFilter = {
  query: string
  role: AuthorityRole | 'ALL'
  status: 'ALL' | 'ACTIVE' | 'BANNED'
  page: number
}

export const initialFilter: UserFilter = { query: '', role: 'ALL', status: 'ALL', page: 0 }

// Q119: GET /admin/users takes optional query, role and banned (Q29); absent means "any".
export function usersPath(filter: UserFilter): string {
  const params = new URLSearchParams()
  const query = filter.query.trim()
  if (query) params.set('query', query)
  if (filter.role !== 'ALL') params.set('role', filter.role)
  if (filter.status !== 'ALL') params.set('banned', String(filter.status === 'BANNED'))
  params.set('page', String(filter.page))
  params.set('size', String(PAGE_SIZE))
  return `/admin/users?${params}`
}

// An account has exactly one role (Q22).
export const roleOfUser = (user: Pick<AdminUser, 'roles'>): AuthorityRole | undefined => user.roles[0]

export const roleLabel: Record<AuthorityRole, string> = {
  ROLE_VOLUNTEER: 'Volunteer',
  ROLE_ORGANIZATION: 'Organization',
  ROLE_ADMIN: 'Admin',
}

export type UserAction = 'ban' | 'unban' | 'none'

// Q120: admins (oneself included) cannot be banned (Q30), so they get no button.
export function userActionOf(user: Pick<AdminUser, 'roles' | 'banned'>): UserAction {
  if (roleOfUser(user) === 'ROLE_ADMIN') return 'none'
  return user.banned ? 'unban' : 'ban'
}

// Q121: what a ban does, by role (Q23, Q25, Q26).
export function banConsequences(user: Pick<AdminUser, 'roles'>): string {
  return roleOfUser(user) === 'ROLE_ORGANIZATION'
    ? 'All sessions end and login is refused. Its activities that have not started are cancelled, and their volunteers get an email.'
    : 'All sessions end and login is refused. Applications and invitations for activities that have not started are withdrawn or declined, and the organizations get an email.'
}
