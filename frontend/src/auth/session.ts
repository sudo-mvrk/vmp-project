import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'
import type {
  AppUser,
  LoginRequest,
  OrganizationRegistrationRequest,
  VolunteerRegistrationRequest,
} from '../api/types'

export type Role = 'VOLUNTEER' | 'ORGANIZATION' | 'ADMIN'

export const currentUserKey = ['currentUser'] as const

// The backend sends Spring authorities ("ROLE_VOLUNTEER"); an account has exactly one role.
export function roleOf(user: AppUser): Role | null {
  const role = user.roles?.find((r) => r.startsWith('ROLE_'))?.slice('ROLE_'.length)
  return role === 'VOLUNTEER' || role === 'ORGANIZATION' || role === 'ADMIN' ? role : null
}

// GET /auth/me answers 401 without a session: that means "anonymous", not an error.
async function fetchCurrentUser(): Promise<AppUser | null> {
  try {
    return await api<AppUser>('/auth/me')
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      return null
    }
    throw e
  }
}

// The session lives in a cookie, so after a reload the user is found again by asking the backend.
export function useCurrentUser() {
  return useQuery({
    queryKey: currentUserKey,
    queryFn: fetchCurrentUser,
    staleTime: Infinity,
    retry: false,
  })
}

// Any 401 means the session is gone (expired, logged out elsewhere, banned): forget the user,
// and the route guard of a protected page sends them to the login page.
export function forgetUserOn401(queryClient: QueryClient) {
  return () => queryClient.setQueryData(currentUserKey, null)
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (credentials: LoginRequest) => api('/auth/login', { method: 'POST', body: credentials }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: currentUserKey }),
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api('/auth/logout', { method: 'POST' }),
    onSuccess: () => {
      // Nothing cached for the previous user may leak into the next session.
      queryClient.clear()
      queryClient.setQueryData(currentUserKey, null)
    },
  })
}

export type Registration =
  | { role: 'VOLUNTEER'; request: VolunteerRegistrationRequest }
  | { role: 'ORGANIZATION'; request: OrganizationRegistrationRequest }

// Registration does not open a session, so a successful one is followed by a normal login.
export function useRegister() {
  const login = useLogin()
  return useMutation({
    mutationFn: async (registration: Registration) => {
      const path = registration.role === 'VOLUNTEER' ? '/auth/register/volunteer' : '/auth/register/organization'
      await api<AppUser>(path, { method: 'POST', body: registration.request })
      await login.mutateAsync({ username: registration.request.username, password: registration.request.password })
    },
  })
}

export function homePathOf(role: Role | null): string {
  switch (role) {
    case 'VOLUNTEER':
      return '/volunteer/recommendations'
    case 'ORGANIZATION':
      return '/organization/activities'
    default:
      return '/'
  }
}
