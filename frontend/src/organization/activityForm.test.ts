import { describe, expect, it } from 'vitest'
import type { OwnOrganizationProfile } from '../api/types'
import { activity } from '../test/fixtures'
import { formErrorsOf, newValues, toRequest, validate, valuesOf, type Values } from './activityForm'

const organization = {
  id: 3,
  username: 'org',
  organizationName: 'Prague Food Bank',
  description: null,
  websiteUrl: null,
  lat: 50.1,
  lon: 14.5,
  city: 'Prague',
  fullAddress: 'Na Poříčí 1',
  countryCode: 'CZ',
} satisfies OwnOrganizationProfile

function filled(overrides: Partial<Values> = {}): Values {
  return { ...newValues(organization), title: 'Help', date: '2026-11-14', startTime: '09:00', endTime: '12:00', ...overrides }
}

describe('newValues', () => {
  it("starts at the organization's place with MEDIUM priority and the platform's weights", () => {
    const values = newValues(organization)

    expect(values.location).toEqual({ lat: 50.1, lon: 14.5 })
    expect(values.city).toBe('Prague')
    expect(values.fullAddress).toBe('Na Poříčí 1')
    expect(values.priority).toBe('MEDIUM')
    expect(values.customWeights).toBe(false)
  })

  it('starts without a place when the organization has none', () => {
    expect(newValues({ ...organization, lat: null, lon: null }).location).toBeNull()
  })
})

describe('toRequest', () => {
  it('builds the whole activity with one date and two times', () => {
    expect(toRequest(filled({ skillIds: ['2', '5'], certificationIds: ['1'], priority: 'HIGH', spotsNeeded: 3 }))).toEqual({
      title: 'Help',
      shortDescription: '',
      fullDescription: '',
      priority: 'HIGH',
      lat: 50.1,
      lon: 14.5,
      city: 'Prague',
      fullAddress: 'Na Poříčí 1',
      startsAt: '2026-11-14T09:00',
      endsAt: '2026-11-14T12:00',
      spotsNeeded: 3,
      requiredSkillIds: [2, 5],
      requiredCertificationIds: [1],
      weights: undefined,
    })
  })

  it('sends the weights only when the override is switched on', () => {
    expect(toRequest(filled({ customWeights: true, alpha: 0.2, beta: 0.7, gamma: 0.1 })).weights).toEqual({
      alpha: 0.2,
      beta: 0.7,
      gamma: 0.1,
    })
    expect(toRequest(filled({ customWeights: false, alpha: 0.2, beta: 0.7, gamma: 0.1 })).weights).toBeUndefined()
  })

  it('round-trips a stored activity', () => {
    const stored = activity({ weights: { alpha: 0, beta: 1, gamma: 0 } })
    const request = toRequest(valuesOf(stored))

    expect(request.startsAt).toBe('2026-11-07T09:00')
    expect(request.endsAt).toBe('2026-11-07T12:00')
    expect(request.weights).toEqual({ alpha: 0, beta: 1, gamma: 0 })
  })
})

describe('validate', () => {
  it('accepts a complete form', () => {
    expect(validate(filled())).toEqual({})
  })

  it('needs an end after the start', () => {
    expect(validate(filled({ startTime: '12:00', endTime: '12:00' })).endTime).toBe('End must be after the start')
    expect(validate(filled({ startTime: '12:00', endTime: '09:30' })).endTime).toBe('End must be after the start')
  })

  it('needs title, place, date and times', () => {
    const errors = validate(filled({ title: ' ', location: null, date: null, startTime: '', endTime: '' }))

    expect(Object.keys(errors).sort()).toEqual(['date', 'endTime', 'location', 'startTime', 'title'])
  })

  it('checks that custom weights sum to 1, and only when they are on', () => {
    expect(validate(filled({ customWeights: true, alpha: 0.5, beta: 0.5, gamma: 0.5 })).gamma).toBe('α + β + γ must be 1')
    expect(validate(filled({ customWeights: true, alpha: 0.33, beta: 0.33, gamma: 0.34 }))).toEqual({})
    expect(validate(filled({ customWeights: false, alpha: 0.5, beta: 0.5, gamma: 0.5 }))).toEqual({})
  })

  it('keeps spots within 1 … 1000', () => {
    expect(validate(filled({ spotsNeeded: 0 })).spotsNeeded).toBe('Between 1 and 1000')
    expect(validate(filled({ spotsNeeded: '' })).spotsNeeded).toBe('Between 1 and 1000')
  })
})

describe('formErrorsOf', () => {
  it('puts backend field errors on the inputs that hold them', () => {
    expect(formErrorsOf({ startsAt: 'must not be null', lat: 'out of range', other: 'x' })).toEqual({
      startTime: 'must not be null',
      location: 'out of range',
    })
  })
})
