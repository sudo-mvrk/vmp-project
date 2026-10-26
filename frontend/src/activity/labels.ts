import type { ApplicationStatus, Priority } from '../api/types'

export const priorityColor: Record<Priority, string> = {
  LOW: 'gray',
  MEDIUM: 'blue',
  HIGH: 'orange',
  URGENT: 'red',
}

export const applicationStatusColor: Record<ApplicationStatus, string> = {
  PENDING: 'yellow',
  ACCEPTED: 'green',
  REJECTED: 'red',
  WITHDRAWN: 'gray',
  DECLINED: 'gray',
}
