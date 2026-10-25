import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, setUnauthorizedHandler } from './client'

const fetchMock = vi.fn<typeof fetch>()

function jsonResponse(status: number, body?: unknown) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function setCookie(value: string) {
  document.cookie = `XSRF-TOKEN=${value}; path=/`
}

function clearCookie() {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
}

function headersOfCall(index: number): Record<string, string> {
  return fetchMock.mock.calls[index][1]?.headers as Record<string, string>
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  clearCookie()
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  setUnauthorizedHandler(() => {})
})

describe('api', () => {
  it('sends a GET without a CSRF header and returns the parsed body', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { id: 7 }))

    const result = await api<{ id: number }>('/auth/me')

    expect(result).toEqual({ id: 7 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/auth/me')
    expect(headersOfCall(0)['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('puts the token from the XSRF-TOKEN cookie into the header of a POST', async () => {
    setCookie('from-cookie')
    fetchMock.mockResolvedValueOnce(jsonResponse(200))

    await api('/auth/login', { method: 'POST', body: { username: 'a', password: 'b' } })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(headersOfCall(0)['X-XSRF-TOKEN']).toBe('from-cookie')
    expect(headersOfCall(0)['Content-Type']).toBe('application/json')
    expect(fetchMock.mock.calls[0][1]?.body).toBe('{"username":"a","password":"b"}')
  })

  it('asks GET /auth/csrf for a token when the cookie is missing', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, { headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'fresh' }))
      .mockResolvedValueOnce(jsonResponse(200))

    await api('/auth/logout', { method: 'POST' })

    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/auth/csrf')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/auth/logout')
    expect(headersOfCall(1)['X-XSRF-TOKEN']).toBe('fresh')
  })

  it('sends the token with PUT too', async () => {
    setCookie('t')
    fetchMock.mockResolvedValueOnce(jsonResponse(200, {}))

    await api('/volunteers/me', { method: 'PUT', body: {} })

    expect(headersOfCall(0)['X-XSRF-TOKEN']).toBe('t')
  })

  it('returns undefined for an empty body', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 200 }))

    await expect(api('/auth/me')).resolves.toBeUndefined()
  })

  it('calls the unauthorized handler on 401 and throws the backend message', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    fetchMock.mockResolvedValueOnce(jsonResponse(401, { message: 'Invalid username or password' }))

    const error = await api('/auth/me').catch((e: unknown) => e)

    expect(handler).toHaveBeenCalledTimes(1)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect((error as ApiError).message).toBe('Invalid username or password')
  })

  it('does not call the unauthorized handler for other errors', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    fetchMock.mockResolvedValueOnce(jsonResponse(409, { message: 'No free spot' }))

    await expect(api('/auth/me')).rejects.toThrow('No free spot')
    expect(handler).not.toHaveBeenCalled()
  })

  it('keeps the field errors of a validation failure', async () => {
    setCookie('t')
    fetchMock.mockResolvedValueOnce(
      jsonResponse(400, { message: 'Validation failed', errors: { email: 'must be a well-formed email address' } }),
    )

    const error = (await api('/auth/register/volunteer', { method: 'POST', body: {} }).catch((e: unknown) => e)) as ApiError

    expect(error.status).toBe(400)
    expect(error.fieldErrors).toEqual({ email: 'must be a well-formed email address' })
  })

  it('falls back to the status when the error body is not JSON', async () => {
    fetchMock.mockResolvedValueOnce(new Response('<html>Bad gateway</html>', { status: 502, statusText: 'Bad Gateway' }))

    await expect(api('/auth/me')).rejects.toThrow('Bad Gateway')
  })
})
