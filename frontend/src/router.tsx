import { createBrowserRouter, type RouteObject } from 'react-router'
import { ActivityListPage } from './activity/ActivityListPage'
import { ActivityPage } from './activity/ActivityPage'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireRole } from './auth/RequireRole'
import { AppLayout } from './layout/AppLayout'
import { HomePage } from './pages/HomePage'
import { NoAccessPage } from './pages/NoAccessPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ActivitiesPage as OrganizationActivitiesPage } from './organization/ActivitiesPage'
import { EditActivityPage } from './organization/EditActivityPage'
import { ManageActivityPage } from './organization/ManageActivityPage'
import { NewActivityPage } from './organization/NewActivityPage'
import { ProfilePage as OrganizationProfilePage } from './organization/ProfilePage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { ProfilePage as VolunteerProfilePage } from './volunteer/ProfilePage'
import { RecommendationsPage } from './volunteer/RecommendationsPage'

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/no-access', element: <NoAccessPage /> },
      { path: '/activities', element: <ActivityListPage /> },
      { path: '/activities/:id', element: <ActivityPage /> },
      {
        element: <RequireRole role="VOLUNTEER" />,
        children: [
          { path: '/volunteer/recommendations', element: <RecommendationsPage /> },
          { path: '/volunteer/applications', element: <PlaceholderPage title="My applications" slice="F5" /> },
          { path: '/volunteer/history', element: <PlaceholderPage title="History" slice="F5" /> },
          { path: '/volunteer/profile', element: <VolunteerProfilePage /> },
        ],
      },
      {
        element: <RequireRole role="ORGANIZATION" />,
        children: [
          { path: '/organization/activities', element: <OrganizationActivitiesPage /> },
          { path: '/organization/activities/new', element: <NewActivityPage /> },
          { path: '/organization/activities/:id', element: <ManageActivityPage /> },
          { path: '/organization/activities/:id/edit', element: <EditActivityPage /> },
          { path: '/organization/profile', element: <OrganizationProfilePage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
