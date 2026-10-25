export type GeoPoint = { lat: number; lon: number }

// Q68: without a stored point the map opens on the seed area (Prague).
export const DEFAULT_CENTER: GeoPoint = { lat: 50.08, lon: 14.42 }
export const DEFAULT_ZOOM = 11

// Six decimals are about 10 cm, more than enough and easier to read than the 15 of a click.
export const round = (value: number) => Math.round(value * 1e6) / 1e6
