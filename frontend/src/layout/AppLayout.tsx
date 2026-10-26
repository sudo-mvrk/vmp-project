import { AppShell, Badge, Burger, Button, Container, Drawer, Group, Stack, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, NavLink, Outlet, useMatches, useNavigate } from 'react-router'
import { useMyApplications } from '../activity/queries'
import { roleOf, useCurrentUser, useLogout, type Role } from '../auth/session'
import { PAGE_BACKGROUND } from '../theme'
import { pendingInvitationCount } from '../volunteer/myApplications'
import classes from './AppLayout.module.css'
import { Logo } from './Logo'

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

const linkClass = (base: string) => ({ isActive }: { isActive: boolean }) => (isActive ? `${base} active` : base)

// Q128: a top header instead of the sidebar; on a phone the links move into a drawer.
export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure()
  const { data: user } = useCurrentUser()
  const logout = useLogout()
  const navigate = useNavigate()
  const role = user ? (roleOf(user) ?? 'ANONYMOUS') : 'ANONYMOUS'
  const items = menus[role]
  const fullBleed = useMatches().some((match) => (match.handle as { fullBleed?: boolean } | undefined)?.fullBleed)
  // Q114: email is only a stub, so pending invitations are counted in the menu.
  const [now] = useState(() => new Date())
  const invitations = pendingInvitationCount(useMyApplications(role === 'VOLUNTEER').data, now)

  const onLogout = () =>
    logout.mutate(undefined, {
      onSuccess: () => navigate('/login', { replace: true }),
      onError: (e) => notifications.show({ color: 'wine', title: 'Logout failed', message: e.message }),
    })

  const badgeOf = (item: MenuItem) =>
    item.to === '/volunteer/applications' && invitations > 0 ? (
      <Badge size="sm" circle color="amber" variant="filled" aria-label={`${invitations} pending invitations`}>
        {invitations}
      </Badge>
    ) : null

  return (
    <AppShell header={{ height: 68 }} padding={0} styles={{ main: { background: PAGE_BACKGROUND } }}>
      <AppShell.Header className={classes.header}>
        <Container size="xl" h="100%">
          <Group h="100%" justify="space-between" wrap="nowrap">
            <Logo />
            <Group gap={2} visibleFrom="sm" wrap="nowrap">
              {items.map((item) => (
                <NavLink key={item.to} to={item.to} className={linkClass(classes.link)}>
                  {item.label}
                  {badgeOf(item)}
                </NavLink>
              ))}
            </Group>
            <Group gap="sm" wrap="nowrap">
              {user ? (
                <>
                  <Text size="sm" className={classes.user} visibleFrom="md">
                    {user.username}
                  </Text>
                  <Button variant="white" color="navy" size="xs" onClick={onLogout} loading={logout.isPending} visibleFrom="sm">
                    Log out
                  </Button>
                </>
              ) : (
                <Group gap="xs" visibleFrom="sm" wrap="nowrap">
                  <Button component={Link} to="/login" variant="subtle" color="gray.0" size="xs">
                    Log in
                  </Button>
                  <Button component={Link} to="/register" variant="white" color="navy" size="xs">
                    Register
                  </Button>
                </Group>
              )}
              <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" color="white" aria-label="Menu" />
            </Group>
          </Group>
        </Container>
      </AppShell.Header>

      <Drawer opened={opened} onClose={close} position="right" size="80%" title={user?.username ?? 'Menu'} hiddenFrom="sm">
        <Stack gap={4}>
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} onClick={close} className={linkClass(classes.drawerLink)}>
              {item.label}
              {badgeOf(item)}
            </NavLink>
          ))}
          {user ? (
            <Button mt="md" variant="light" onClick={onLogout} loading={logout.isPending}>
              Log out
            </Button>
          ) : (
            <Group mt="md" grow>
              <Button component={Link} to="/login" variant="default" onClick={close}>
                Log in
              </Button>
              <Button component={Link} to="/register" onClick={close}>
                Register
              </Button>
            </Group>
          )}
        </Stack>
      </Drawer>

      <AppShell.Main>
        {fullBleed ? (
          <Outlet />
        ) : (
          <Container size="xl" py="xl" px={{ base: 'md', sm: 'xl' }}>
            <Outlet />
          </Container>
        )}
      </AppShell.Main>
    </AppShell>
  )
}
