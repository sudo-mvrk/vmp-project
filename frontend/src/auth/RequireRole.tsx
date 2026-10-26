import { Alert, Center, Loader } from '@mantine/core'
import { Navigate, Outlet, useLocation } from 'react-router'
import { roleOf, useCurrentUser, type Role } from './session'

// Layout route that lets only one role through: anonymous users go to the login page
// (and come back afterwards through ?next=), other roles to the "no access" page.
export function RequireRole({ role }: { role: Role }) {
  const { data: user, isPending, isError, error } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return (
      <Center mt="xl">
        <Loader />
      </Center>
    )
  }
  if (isError) {
    return (
      <Alert color="wine" title="The server cannot be reached">
        {error.message}
      </Alert>
    )
  }
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  if (roleOf(user) !== role) {
    return <Navigate to="/no-access" replace />
  }
  return <Outlet />
}
