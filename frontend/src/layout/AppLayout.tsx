import { AppShell, Burger, Button, Group, NavLink, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { Link, NavLink as RouterNavLink, Outlet, useNavigate } from 'react-router'
import { roleOf, useCurrentUser, useLogout, type Role } from '../auth/session'

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
  // Q61: no admin screens; the admin works through Swagger or the API.
  ADMIN: [{ to: '/activities', label: 'Activities' }],
}

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure()
  const { data: user } = useCurrentUser()
  const logout = useLogout()
  const navigate = useNavigate()
  const items = menus[user ? (roleOf(user) ?? 'ANONYMOUS') : 'ANONYMOUS']

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
          <NavLink key={item.to} component={RouterNavLink} to={item.to} label={item.label} onClick={close} />
        ))}
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
