import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'
import type { AppUser } from '../api/types'
import { currentUserKey } from '../auth/session'
import { theme } from '../theme'

// Renders routes at a path with a known current user (null = anonymous), so no request for
// GET /auth/me is made. Returns the router to check where navigation ended.
export function renderRoutes(routes: RouteObject[], path: string, user: AppUser | null) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  queryClient.setQueryData(currentUserKey, user)
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <MantineProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>,
  )
  return { router, queryClient }
}

// Renders one component with Mantine and a fresh query client, without a router.
export function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(
    <MantineProvider theme={theme}>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MantineProvider>,
  )
  return { queryClient }
}
