import { describe, expect, it } from 'vitest'
import { homePathOf } from '../auth/session'
import { banConsequences, initialFilter, userActionOf, usersPath } from './users'

describe('usersPath', () => {
  it('sends only the filters that are set', () => {
    expect(usersPath(initialFilter)).toBe('/admin/users?page=0&size=20')
    expect(usersPath({ query: ' vol0 ', role: 'ROLE_VOLUNTEER', status: 'BANNED', page: 2 })).toBe(
      '/admin/users?query=vol0&role=ROLE_VOLUNTEER&banned=true&page=2&size=20',
    )
    expect(usersPath({ ...initialFilter, status: 'ACTIVE' })).toBe('/admin/users?banned=false&page=0&size=20')
  })
})

describe('userActionOf', () => {
  it('offers ban to active and unban to banned volunteers and organizations', () => {
    expect(userActionOf({ roles: ['ROLE_VOLUNTEER'], banned: false })).toBe('ban')
    expect(userActionOf({ roles: ['ROLE_ORGANIZATION'], banned: true })).toBe('unban')
  })

  it('offers nothing for admins (Q30)', () => {
    expect(userActionOf({ roles: ['ROLE_ADMIN'], banned: false })).toBe('none')
  })
})

describe('banConsequences', () => {
  it('says what a ban does to each role', () => {
    expect(banConsequences({ roles: ['ROLE_VOLUNTEER'] })).toMatch(/Applications and invitations .* withdrawn or declined/)
    expect(banConsequences({ roles: ['ROLE_ORGANIZATION'] })).toMatch(/activities that have not started are cancelled/)
  })
})

describe('homePathOf', () => {
  it('sends an admin to the users page after login', () => {
    expect(homePathOf('ADMIN')).toBe('/admin/users')
  })
})
