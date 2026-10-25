import { Alert, List } from '@mantine/core'
import { ApiError } from '../api/client'

// A failed request: the backend message, and field errors that have no input of their own on the page
// (for example "location" for lat and lon given apart). Fields with an input show their error there.
export function ErrorAlert({ error, shownFields = [] }: { error: Error | null; shownFields?: string[] }) {
  if (!error) {
    return null
  }
  const otherFieldErrors =
    error instanceof ApiError ? Object.entries(error.fieldErrors).filter(([field]) => !shownFields.includes(field)) : []
  return (
    <Alert color="red" title={error.message}>
      {otherFieldErrors.length > 0 && (
        <List size="sm">
          {otherFieldErrors.map(([field, message]) => (
            <List.Item key={field}>
              {field}: {message}
            </List.Item>
          ))}
        </List>
      )}
    </Alert>
  )
}
