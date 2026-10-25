// The one way the frontend talks to the backend (Q59): same-origin fetch through the Vite proxy,
// the CSRF token on every modifying request, and backend error bodies turned into ApiError.

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']

// Body of the backend's GlobalExceptionHandler. Not in the OpenAPI document, so declared here.
type ErrorBody = {
  message?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

let onUnauthorized: () => void = () => {}

// The app registers what a 401 means (forget the current user); the route guards then send
// the user to the login page. Kept out of this module so it has no router or React dependency.
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null
}

// The backend sets the cookie on the first request that reads the token, and logout clears it,
// so a missing cookie is normal: GET /auth/csrf sets it again and returns the same value.
async function csrfToken(): Promise<string> {
  const fromCookie = readCookie(CSRF_COOKIE)
  if (fromCookie) {
    return fromCookie
  }
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
  if (!response.ok) {
    throw new ApiError(response.status, 'Could not get a CSRF token')
  }
  const body = (await response.json()) as { token: string }
  return body.token
}

async function errorFrom(response: Response): Promise<ApiError> {
  let body: ErrorBody = {}
  try {
    body = (await response.json()) as ErrorBody
  } catch {
    // Not JSON (for example a proxy error page); the status text is all there is.
  }
  const message = body.message || response.statusText || `Request failed with status ${response.status}`
  return new ApiError(response.status, message, body.errors ?? {})
}

export async function api<T = void>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET'
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (!SAFE_METHODS.includes(method)) {
    headers[CSRF_HEADER] = await csrfToken()
  }

  const response = await fetch(`/api/v1${path}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: 'same-origin',
  })

  if (!response.ok) {
    if (response.status === 401) {
      onUnauthorized()
    }
    throw await errorFrom(response)
  }

  // Login, logout and the status changes answer 200 or 204 without a body.
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
