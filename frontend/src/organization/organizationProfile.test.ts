import { describe, expect, it } from 'vitest'
import { toUpdateRequest, type Values } from './organizationProfile'

const values: Values = {
  organizationName: ' Food Bank ',
  description: '',
  websiteUrl: '',
  location: { lat: 50.1, lon: 14.5 },
  city: 'Prague',
  fullAddress: '',
  countryCode: 'cz',
}

describe('toUpdateRequest', () => {
  it('trims, upper-cases the country code and sends the picked point', () => {
    expect(toUpdateRequest(values)).toEqual({
      organizationName: 'Food Bank',
      description: '',
      websiteUrl: '',
      lat: 50.1,
      lon: 14.5,
      city: 'Prague',
      fullAddress: '',
      countryCode: 'CZ',
    })
  })

  it('leaves out an empty country code and a missing point (both mean "keep")', () => {
    const request = toUpdateRequest({ ...values, countryCode: ' ', location: null })

    expect(request.countryCode).toBeUndefined()
    expect(request.lat).toBeUndefined()
    expect(request.lon).toBeUndefined()
  })
})
