import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Activity, ActivityPage, MyApplication, OrganizationProfile, RecommendedActivity } from '../api/types'

export const PAGE_SIZE = 20

// Kept apart from ['volunteer', 'me'] (the own profile), so invalidating one does not refetch the other.
export const recommendedActivitiesKey = ['volunteer', 'recommended-activities'] as const
export const myApplicationsKey = ['volunteer', 'applications'] as const

// Q84: the public list, open activities that have not started, soonest first. Pages count from 0.
export function useActivities(page: number) {
  return useQuery({
    queryKey: ['activities', page],
    queryFn: () => api<ActivityPage>(`/activities?page=${page}&size=${PAGE_SIZE}`),
    placeholderData: keepPreviousData,
  })
}

export function useActivity(id: number) {
  return useQuery({ queryKey: ['activity', id], queryFn: () => api<Activity>(`/activities/${id}`) })
}

// Q76: the activity has only the organization's id; its public profile gives the name.
export function useOrganization(id: number | undefined) {
  return useQuery({
    queryKey: ['organization', id],
    queryFn: () => api<OrganizationProfile>(`/organizations/${id}`),
    enabled: id !== undefined,
  })
}

export function useRecommendedActivities(limit: number) {
  return useQuery({
    queryKey: [...recommendedActivitiesKey, limit],
    queryFn: () => api<RecommendedActivity[]>(`/volunteers/me/recommended-activities?limit=${limit}`),
  })
}

// Q78: whether the volunteer already applied is read from their own applications.
export function useMyApplications(enabled: boolean) {
  return useQuery({
    queryKey: myApplicationsKey,
    queryFn: () => api<MyApplication[]>('/volunteers/me/applications'),
    enabled,
  })
}

// Q79: after applying, the activity leaves the recommendations (the backend skips applied ones)
// and shows the new status on its page.
export function useApply() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (activityId: number) => api(`/activities/${activityId}/applications`, { method: 'POST' }),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: recommendedActivitiesKey }),
        queryClient.invalidateQueries({ queryKey: myApplicationsKey }),
      ]),
  })
}
