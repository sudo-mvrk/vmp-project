import { describe, expect, it } from 'vitest'
import { skillOptions } from './skills'

describe('skillOptions', () => {
  it('groups skills by category, both in alphabetical order', () => {
    const options = skillOptions([
      { id: 3, skillName: 'Food distribution', category: 'LOGISTICS' },
      { id: 1, skillName: 'Childcare', category: 'SOCIAL' },
      { id: 2, skillName: 'Event setup', category: 'LOGISTICS' },
      { id: 4, skillName: 'Elderly care', category: 'SOCIAL' },
    ])

    expect(options).toEqual([
      {
        group: 'Logistics',
        items: [
          { value: '2', label: 'Event setup' },
          { value: '3', label: 'Food distribution' },
        ],
      },
      {
        group: 'Social',
        items: [
          { value: '1', label: 'Childcare' },
          { value: '4', label: 'Elderly care' },
        ],
      },
    ])
  })
})
