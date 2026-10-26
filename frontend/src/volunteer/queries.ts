import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { myApplicationsKey, recommendedActivitiesKey } from '../activity/queries'
import type {
  AvailabilitySlot,
  HeldCertificationRequest,
  HistoryItem,
  MyApplication,
  OwnVolunteerProfile,
  VolunteerProfileUpdate,
} from '../api/types'

export const ownVolunteerProfileKey = ['volunteer', 'me'] as const

export function useOwnVolunteerProfile() {
  return useQuery({ queryKey: ownVolunteerProfileKey, queryFn: () => api<OwnVolunteerProfile>('/volunteers/me') })
}

// Every PUT of the profile answers with the whole profile, which replaces the cached one (Q72).
function useProfilePut<T>(path: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: T) => api<OwnVolunteerProfile>(path, { method: 'PUT', body }),
    onSuccess: (profile) => queryClient.setQueryData(ownVolunteerProfileKey, profile),
  })
}

export const useUpdateBasicProfile = () => useProfilePut<VolunteerProfileUpdate>('/volunteers/me')
export const useReplaceSkills = () => useProfilePut<{ skillIds: number[] }>('/volunteers/me/skills')
export const useReplaceCertifications = () =>
  useProfilePut<{ certifications: HeldCertificationRequest[] }>('/volunteers/me/certifications')
export const useReplaceAvailability = () => useProfilePut<{ slots: AvailabilitySlot[] }>('/volunteers/me/availability')

// Q113: completed activities the volunteer took part in, newest first (backend order).
export function useHistory() {
  return useQuery({ queryKey: ['volunteer', 'history'], queryFn: () => api<HistoryItem[]>('/volunteers/me/history') })
}

export type OwnAction = 'withdraw' | 'accept' | 'decline'

const PATH_OF: Record<OwnAction, (id: number) => string> = {
  withdraw: (id) => `/applications/${id}/withdraw`,
  accept: (id) => `/invitations/${id}/accept`,
  decline: (id) => `/invitations/${id}/decline`,
}

// Q116: an action changes the own list, the free capacity (so the recommendations) and the activity's
// accepted count.
export function useOwnApplicationAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ application, action }: { application: MyApplication; action: OwnAction }) =>
      api<MyApplication>(PATH_OF[action](application.id), { method: 'POST' }),
    onSuccess: (_saved, { application }) =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: myApplicationsKey }),
        queryClient.invalidateQueries({ queryKey: recommendedActivitiesKey }),
        queryClient.invalidateQueries({ queryKey: ['activity', application.activityId] }),
      ]),
  })
}
