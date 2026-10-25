// oxlint-disable react/only-export-components -- a test double of react-leaflet, not a module with fast refresh
import type { ReactNode } from 'react'
import { vi } from 'vitest'

// Leaflet needs a real layout engine; jsdom has none. Tests replace react-leaflet with this module
// (`vi.mock('react-leaflet', () => import('../test/leafletMock'))`): shapes render as plain elements
// with their props, and `clickMap` calls the click handler registered with useMapEvents.
type Handlers = { click?: (event: { latlng: { lat: number; lng: number } }) => void }
let handlers: Handlers = {}

export function clickMap(lat: number, lng: number) {
  handlers.click?.({ latlng: { lat, lng } })
}

export const useMapEvents = vi.fn((h: Handlers) => {
  handlers = h
  return null
})

export function MapContainer({ children }: { children: ReactNode }) {
  return <div data-testid="map">{children}</div>
}

export function TileLayer() {
  return null
}

export function CircleMarker({ center }: { center: [number, number] }) {
  return <div data-testid="marker" data-center={center.join(',')} />
}

export function Circle({ center, radius }: { center: [number, number]; radius: number }) {
  return <div data-testid="radius-circle" data-center={center.join(',')} data-radius={radius} />
}
