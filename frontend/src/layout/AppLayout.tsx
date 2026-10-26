import { AppShell, Badge, Burger, Button, Group, NavLink, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useState } from 'react'
import { notifications } from '@mantine/notifications'
import { Link, NavLink as RouterNavLink, Outlet, useNavigate } from 'react-router'
import { useMyApplications } from '../activity/queries'
import { roleOf, useCurrentUser, useLogout, type Role } from '../auth/session'
import { pendingInvitationCount } from '../volunteer/myApplications'

type MenuItem = { to: string; label: string }

const menus: Record<Role | 'ANONYMOUS', MenuItem[]> = {
  ANONYMOUS: [{ to: '/activities', label: 'Activities' }],
  VOLUNTEER: [
    { to: '/volunteer/recommendations', label: 'Recommended for me' },
    { to: '/activities', label: 'All activities' },
    { to: '/volunteer/applications', label: 'My applications' },
    { to: '/volunteer/history', label: 'History' },
    { to: '/volunteer/profile', label: 'Profile' },
  ],
  ORGANIZATION: [
    { to: '/organization/activities', label: 'Our activities' },
    { to: '/activities', label: 'All activities' },
    { to: '/organization/profile', label: 'Profile' },
  ],
  // Q117, Q118: users and activities; everything else of the admin stays in the API.
  ADMIN: [
    { to: '/admin/users', label: 'Users' },
    { to: '/activities', label: 'Activities' },
  ],
}

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure()
  const { data: user } = useCurrentUser()
  const logout = useLogout()
  const navigate = useNavigate()
  const role = user ? (roleOf(user) ?? 'ANONYMOUS') : 'ANONYMOUS'
  const items = menus[role]
  // Q114: email is only a stub, so pending invitations are counted in the menu.
  const [now] = useState(() => new Date())
  const invitations = pendingInvitationCount(useMyApplications(role === 'VOLUNTEER').data, now)

  const onLogout = () =>
    logout.mutate(undefined, {
      onSuccess: () => navigate('/login', { replace: true }),
      onError: (e) => notifications.show({ color: 'red', title: 'Logout failed', message: e.message }),
    })

  return (
    <AppShell header={{ height: 56 }} navbar={{ width: 240, breakpoint: 'sm', collapsed: { mobile: !opened } }} padding="md">
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <Text component={Link} to="/" fw={700} size="lg">
              VolunNear
            </Text>
          </Group>
          {user ? (
            <Group>
              <Text size="sm">{user.username}</Text>
              <Button variant="default" size="xs" onClick={onLogout} loading={logout.isPending}>
                Log out
              </Button>
            </Group>
          ) : (
            <Group>
              <Button component={Link} to="/login" variant="default" size="xs">
                Log in
              </Button>
              <Button component={Link} to="/register" size="xs">
                Register
              </Button>
            </Group>
          )}
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="xs">
        {items.map((item) => (
          <NavLink
            key={item.to}
            component={RouterNavLink}
            to={item.to}
            label={item.label}
            onClick={close}
            rightSection={
              item.to === '/volunteer/applications' && invitations > 0 ? (
                <Badge size="sm" circle aria-label={`${invitations} pending invitations`}>
                  {invitations}
                </Badge>
              ) : null
            }
          />
        ))}
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
