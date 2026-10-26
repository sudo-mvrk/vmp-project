import { Text } from '@mantine/core'
import { Link } from 'react-router'
import { DISPLAY_FONT } from '../theme'
import classes from './Logo.module.css'

// Q129: the VMP monogram in a thin ring and the full name; the name hides on small screens.
export function Logo() {
  return (
    <Link to="/" className={classes.logo} aria-label="VMP, Volunteering Management Platform">
      <span className={classes.ring} style={{ fontFamily: DISPLAY_FONT }}>
        VMP
      </span>
      <Text component="span" className={classes.name} visibleFrom="md">
        Volunteering Management Platform
      </Text>
    </Link>
  )
}
