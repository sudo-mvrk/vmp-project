import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Activity, AdminUser, AdminUserPage } from '../api/types'
import { usersPath, type UserFilter } from './users'

export function useAdminUsers(filter: UserFilter) {
  return useQuery({
    queryKey: ['admin', 'users', filter],
    queryFn: () => api<AdminUserPage>(usersPath(filter)),
    placeholderData: keepPreviousData,
  })
}

export function useBanAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, action }: { userId: number; action: 'ban' | 'unban' }) =>
      api<AdminUser>(`/admin/users/${userId}/${action}`, { method: 'POST' }),
    // A ban also cancels activities and withdraws applications, so every cached list may be stale.
    onSuccess: () => queryClient.invalidateQueries(),
  })
}

// Q122: the organization's cancel without the ownership check (Q31).
export function useAdminCancelActivity(activityId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api<Activity>(`/admin/activities/${activityId}/cancel`, { method: 'POST' }),
    onSuccess: (activity) => {
      queryClient.setQueryData(['activity', activity.id], activity)
      return queryClient.invalidateQueries({ queryKey: ['activities'] })
    },
  })
}
