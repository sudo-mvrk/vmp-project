import { vi } from 'vitest'

type Handler = (init: RequestInit | undefined) => Response | Promise<Response>

// A fetch stub that answers by "METHOD path" (path without /api/v1 and without the query string),
// so a page that makes several requests can be tested without caring about their order.
export function stubFetch(routes: Record<string, Handler>) {
  const fetchMock = vi.fn<typeof fetch>(async (input, init) => {
    const url = new URL(String(input), 'http://localhost')
    const key = `${init?.method ?? 'GET'} ${url.pathname.replace('/api/v1', '')}`
    const handler = routes[key]
    if (!handler) {
      throw new Error(`No stub for ${key}`)
    }
    return handler(init)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}
