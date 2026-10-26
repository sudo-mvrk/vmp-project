import { CircleMarker, MapContainer, TileLayer } from 'react-leaflet'
import { DEFAULT_ZOOM, type GeoPoint } from './geo'

// Q85: a read-only map with one point, for the place of an activity.
export function PointMap({ point }: { point: GeoPoint }) {
  return (
    <MapContainer center={[point.lat, point.lon]} zoom={DEFAULT_ZOOM + 2} style={{ height: 280, borderRadius: 8 }}>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <CircleMarker center={[point.lat, point.lon]} radius={8} pathOptions={{ fillOpacity: 0.9 }} />
    </MapContainer>
  )
}
