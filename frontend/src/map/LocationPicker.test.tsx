import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { clickMap } from '../test/leafletMock'
import { renderWithProviders } from '../test/render'
import { LocationPicker } from './LocationPicker'

vi.mock('react-leaflet', () => import('../test/leafletMock'))

describe('LocationPicker', () => {
  it('sets the point from a map click, rounded to six decimals', () => {
    const onChange = vi.fn()
    renderWithProviders(<LocationPicker value={null} onChange={onChange} />)

    clickMap(50.123456789, 14.987654321)

    expect(onChange).toHaveBeenCalledWith({ lat: 50.123457, lon: 14.987654 })
  })

  it('shows the point in the fields and draws the marker and the radius circle', () => {
    renderWithProviders(<LocationPicker value={{ lat: 50.1, lon: 14.4 }} onChange={() => {}} radiusKm={25} />)

    expect(screen.getByLabelText('Latitude')).toHaveValue('50.1')
    expect(screen.getByLabelText('Longitude')).toHaveValue('14.4')
    expect(screen.getByTestId('marker')).toHaveAttribute('data-center', '50.1,14.4')
    expect(screen.getByTestId('radius-circle')).toHaveAttribute('data-radius', '25000')
  })

  it('draws no marker and no circle without a point', () => {
    renderWithProviders(<LocationPicker value={null} onChange={() => {}} radiusKm={25} />)

    expect(screen.queryByTestId('marker')).toBeNull()
    expect(screen.queryByTestId('radius-circle')).toBeNull()
    expect(screen.getByText('No location yet: click on the map to set it.')).toBeInTheDocument()
  })

  it('edits the point through the fields, keeping the other coordinate', () => {
    const onChange = vi.fn()
    renderWithProviders(<LocationPicker value={{ lat: 50.1, lon: 14.4 }} onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('Latitude'), { target: { value: '49.5' } })

    expect(onChange).toHaveBeenLastCalledWith({ lat: 49.5, lon: 14.4 })
  })

  it('starts from the default centre when a field is typed without a point', () => {
    const onChange = vi.fn()
    renderWithProviders(<LocationPicker value={null} onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('Longitude'), { target: { value: '15' } })

    expect(onChange).toHaveBeenLastCalledWith({ lat: 50.08, lon: 15 })
  })
})
