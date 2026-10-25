import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// The dev server forwards /api to the backend, so the browser sees one origin and the
// SESSION and XSRF-TOKEN cookies work without CORS (Q58). VITE_PROXY_TARGET points it elsewhere.
const backend = process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      // Swagger UI for the admin (Q61: no admin screens).
      '/swagger-ui': backend,
      '/v3/api-docs': backend,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
