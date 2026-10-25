import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { AvailabilitySlot, HeldCertificationRequest, OwnVolunteerProfile, VolunteerProfileUpdate } from '../api/types'

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
