import { createBrowserRouter, type RouteObject } from 'react-router'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireRole } from './auth/RequireRole'
import { AppLayout } from './layout/AppLayout'
import { HomePage } from './pages/HomePage'
import { NoAccessPage } from './pages/NoAccessPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { ProfilePage as VolunteerProfilePage } from './volunteer/ProfilePage'

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/no-access', element: <NoAccessPage /> },
      { path: '/activities', element: <PlaceholderPage title="Activities" slice="F2" /> },
      {
        element: <RequireRole role="VOLUNTEER" />,
        children: [
          { path: '/volunteer/recommendations', element: <PlaceholderPage title="Recommended for me" slice="F2" /> },
          { path: '/volunteer/applications', element: <PlaceholderPage title="My applications" slice="F5" /> },
          { path: '/volunteer/history', element: <PlaceholderPage title="History" slice="F5" /> },
          { path: '/volunteer/profile', element: <VolunteerProfilePage /> },
        ],
      },
      {
        element: <RequireRole role="ORGANIZATION" />,
        children: [
          { path: '/organization/activities', element: <PlaceholderPage title="Our activities" slice="F3" /> },
          { path: '/organization/profile', element: <PlaceholderPage title="Profile" slice="F3" /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
