import { Group, NumberInput, Stack, Text } from '@mantine/core'
import { Circle, CircleMarker, MapContainer, TileLayer, useMapEvents } from 'react-leaflet'
import { DEFAULT_CENTER, DEFAULT_ZOOM, round, type GeoPoint } from './geo'

function ClickToPick({ onPick }: { onPick: (point: GeoPoint) => void }) {
  useMapEvents({
    click: (event) => onPick({ lat: round(event.latlng.lat), lon: round(event.latlng.lng) }),
  })
  return null
}

type Props = {
  value: GeoPoint | null
  onChange: (point: GeoPoint) => void
  // Q69: the volunteer's search radius, drawn around the point.
  radiusKm?: number | null
  error?: string
}

// Q60: a click on the map sets the point; the two fields show it and can edit it. No geocoding.
export function LocationPicker({ value, onChange, radiusKm, error }: Props) {
  const center = value ?? DEFAULT_CENTER
  const setLat = (lat: number | string) =>
    typeof lat === 'number' && onChange({ lat, lon: value?.lon ?? DEFAULT_CENTER.lon })
  const setLon = (lon: number | string) =>
    typeof lon === 'number' && onChange({ lat: value?.lat ?? DEFAULT_CENTER.lat, lon })

  return (
    <Stack gap="xs">
      <MapContainer center={[center.lat, center.lon]} zoom={DEFAULT_ZOOM} style={{ height: 320, borderRadius: 8 }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <ClickToPick onPick={onChange} />
        {value && radiusKm ? (
          <Circle center={[value.lat, value.lon]} radius={radiusKm * 1000} pathOptions={{ weight: 1, fillOpacity: 0.06 }} />
        ) : null}
        {value && <CircleMarker center={[value.lat, value.lon]} radius={7} pathOptions={{ fillOpacity: 0.9 }} />}
      </MapContainer>
      <Text size="xs" c="dimmed">
        {value ? 'Click on the map to move the point.' : 'No location yet: click on the map to set it.'}
      </Text>
      <Group grow>
        <NumberInput label="Latitude" min={-90} max={90} decimalScale={6} value={value?.lat ?? ''} onChange={setLat} />
        <NumberInput label="Longitude" min={-180} max={180} decimalScale={6} value={value?.lon ?? ''} onChange={setLon} />
      </Group>
      {error && (
        <Text size="sm" c="wine">
          {error}
        </Text>
      )}
    </Stack>
  )
}
