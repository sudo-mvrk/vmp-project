import '@fontsource/manrope/400.css'
import '@fontsource/manrope/500.css'
import '@fontsource/manrope/600.css'
import '@fontsource/manrope/700.css'
import '@fontsource/cormorant-garamond/500.css'
import '@fontsource/cormorant-garamond/600.css'
import '@mantine/core/styles.css'
import '@mantine/dates/styles.css'
import '@mantine/notifications/styles.css'
import 'leaflet/dist/leaflet.css'
import './styles.css'

import { MantineProvider } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'
import { setUnauthorizedHandler } from './api/client'
import { forgetUserOn401 } from './auth/session'
import { router } from './router'
import { cssVariablesResolver, theme } from './theme'

const queryClient = new QueryClient({
  defaultOptions: {
    // A 4xx will not change on retry; one retry is enough for a network hiccup.
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
})
setUnauthorizedHandler(forgetUserOn401(queryClient))

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MantineProvider theme={theme} cssVariablesResolver={cssVariablesResolver}>
      <Notifications />
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>
  </StrictMode>,
)
