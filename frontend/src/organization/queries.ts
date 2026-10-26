import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type {
  Activity,
  ActivityApplication,
  ActivityRequest,
  OrganizationProfileUpdate,
  OwnOrganizationProfile,
  RecommendedVolunteer,
} from '../api/types'

export const ownOrganizationKey = ['organization', 'me'] as const
export const myActivitiesKey = ['organization', 'me', 'activities'] as const

export function useOwnOrganization() {
  return useQuery({ queryKey: ownOrganizationKey, queryFn: () => api<OwnOrganizationProfile>('/organizations/me') })
}

export function useUpdateOrganization() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: OrganizationProfileUpdate) =>
      api<OwnOrganizationProfile>('/organizations/me', { method: 'PUT', body }),
    onSuccess: (profile) => queryClient.setQueryData(ownOrganizationKey, profile),
  })
}

export function useMyActivities() {
  return useQuery({ queryKey: myActivitiesKey, queryFn: () => api<Activity[]>('/organizations/me/activities') })
}

// Every change answers with the whole activity: it replaces the cached one, and the lists are read again.
function activityChanged(queryClient: QueryClient, activity: Activity) {
  queryClient.setQueryData(['activity', activity.id], activity)
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: myActivitiesKey }),
    queryClient.invalidateQueries({ queryKey: ['activities'] }),
  ])
}

export function useCreateActivity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ActivityRequest) => api<Activity>('/activities', { method: 'POST', body }),
    onSuccess: (activity) => activityChanged(queryClient, activity),
  })
}

export function useUpdateActivity(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ActivityRequest) => api<Activity>(`/activities/${id}`, { method: 'PUT', body }),
    onSuccess: (activity) => activityChanged(queryClient, activity),
  })
}

export type StatusAction = 'publish' | 'cancel' | 'complete'

export function useActivityAction(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (action: StatusAction) => api<Activity>(`/activities/${id}/${action}`, { method: 'POST' }),
    onSuccess: (activity) => activityChanged(queryClient, activity),
  })
}

// Both live under ['activity', id], so invalidating the activity also reads its applications and
// recommendations again (Q106): an accept changes the free spots, an invite moves a volunteer between them.
export function useActivityApplications(activityId: number, enabled = true) {
  return useQuery({
    queryKey: ['activity', activityId, 'applications'],
    queryFn: () => api<ActivityApplication[]>(`/activities/${activityId}/applications`),
    enabled,
  })
}

export function useRecommendedVolunteers(activityId: number, limit: number, enabled: boolean) {
  return useQuery({
    queryKey: ['activity', activityId, 'recommended-volunteers', limit],
    queryFn: () => api<RecommendedVolunteer[]>(`/activities/${activityId}/recommended-volunteers?limit=${limit}`),
    enabled,
  })
}

function invalidateActivity(queryClient: QueryClient, activityId: number) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['activity', activityId] }),
    queryClient.invalidateQueries({ queryKey: myActivitiesKey }),
  ])
}

export type Decision = 'accept' | 'reject'

export function useDecideApplication(activityId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ applicationId, decision }: { applicationId: number; decision: Decision }) =>
      api<ActivityApplication>(`/applications/${applicationId}/${decision}`, { method: 'POST' }),
    onSuccess: () => invalidateActivity(queryClient, activityId),
  })
}

export function useInvite(activityId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (volunteerId: number) =>
      api<ActivityApplication>(`/activities/${activityId}/invitations`, { method: 'POST', body: { volunteerId } }),
    onSuccess: () => invalidateActivity(queryClient, activityId),
  })
}
