import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Certification, Skill } from '../api/types'

// The dictionaries change only with a migration, so they are loaded once per session.
export function useSkills() {
  return useQuery({ queryKey: ['skills'], queryFn: () => api<Skill[]>('/skills'), staleTime: Infinity })
}

export function useCertifications() {
  return useQuery({
    queryKey: ['certifications'],
    queryFn: () => api<Certification[]>('/certifications'),
    staleTime: Infinity,
  })
}
