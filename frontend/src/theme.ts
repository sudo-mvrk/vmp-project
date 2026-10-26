import { createTheme, type CSSVariablesResolver, type MantineColorsTuple } from '@mantine/core'

// The one place for the look of the app (Q55): colours, radius, fonts and component defaults.
// Design decisions Q123 … Q130: navy, restrained, with translucent layers.

// From a pale tint (0) to the deep navy of the header and hero (9); 8 is the primary shade.
const navy: MantineColorsTuple = [
  '#eef2f8',
  '#dce3ef',
  '#b6c4dc',
  '#8ea3c8',
  '#6c86b6',
  '#5573ab',
  '#3d5a8f',
  '#2c4574',
  '#1d3159',
  '#111f3d',
]

// Q130: muted tones for priorities and statuses, no bright signal colours.
const amber: MantineColorsTuple = [
  '#fbf6ec',
  '#f3e8d0',
  '#e7d0a0',
  '#dab66d',
  '#cfa146',
  '#c8942d',
  '#b5841f',
  '#9a6f17',
  '#7e5a12',
  '#5f430b',
]
const wine: MantineColorsTuple = [
  '#fbf0f2',
  '#f1dde1',
  '#e2b7c0',
  '#d38f9d',
  '#c66d7f',
  '#bf586c',
  '#a8455a',
  '#8f374b',
  '#742c3d',
  '#58202e',
]
const sage: MantineColorsTuple = [
  '#f0f6f3',
  '#dfeae4',
  '#bdd5c8',
  '#98bfaa',
  '#79ac91',
  '#669f81',
  '#548c6f',
  '#44765c',
  '#365e4a',
  '#274537',
]

export const BODY_FONT = 'Manrope, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'
export const DISPLAY_FONT = '"Cormorant Garamond", Georgia, "Times New Roman", serif'

// The page behind the light working pages (Q126), and the deep navy of header and hero.
export const PAGE_BACKGROUND = '#f4f6fa'
export const DEEP_NAVY = '#0c1830'

export const theme = createTheme({
  colors: { navy, amber, wine, sage },
  primaryColor: 'navy',
  primaryShade: { light: 8, dark: 6 },
  black: '#14213d',
  defaultRadius: 'md',
  radius: { xs: '4px', sm: '6px', md: '10px', lg: '16px', xl: '24px' },
  fontFamily: BODY_FONT,
  // Q123: the serif only for page titles (h1, h2); smaller headings stay in Manrope (styles.css).
  headings: {
    fontFamily: DISPLAY_FONT,
    fontWeight: '600',
    sizes: {
      h1: { fontSize: '3rem', lineHeight: '1.1' },
      h2: { fontSize: '2.25rem', lineHeight: '1.15' },
      h3: { fontSize: '1.25rem', lineHeight: '1.3', fontWeight: '700' },
      h4: { fontSize: '1.05rem', lineHeight: '1.35', fontWeight: '700' },
    },
  },
  shadows: {
    xs: '0 1px 2px rgba(17, 31, 61, 0.05)',
    sm: '0 2px 8px rgba(17, 31, 61, 0.06)',
    md: '0 8px 24px rgba(17, 31, 61, 0.08)',
    lg: '0 16px 40px rgba(17, 31, 61, 0.12)',
  },
  components: {
    // Pill buttons like the references (Q123 … Q130).
    Button: { defaultProps: { radius: 'xl' }, styles: { root: { fontWeight: 600, letterSpacing: '0.01em' } } },
    Paper: { defaultProps: { radius: 'lg' } },
    Card: { defaultProps: { radius: 'lg' } },
    Badge: { defaultProps: { radius: 'xl', variant: 'light' }, styles: { root: { fontWeight: 600, letterSpacing: '0.04em' } } },
    TextInput: { defaultProps: { radius: 'md' } },
    PasswordInput: { defaultProps: { radius: 'md' } },
    Select: { defaultProps: { radius: 'md' } },
    MultiSelect: { defaultProps: { radius: 'md' } },
    NumberInput: { defaultProps: { radius: 'md' } },
    Textarea: { defaultProps: { radius: 'md' } },
    SegmentedControl: { defaultProps: { radius: 'xl' } },
    Modal: { defaultProps: { radius: 'lg', centered: true, overlayProps: { backgroundOpacity: 0.45, blur: 3 } } },
    Notification: { defaultProps: { radius: 'lg' } },
    Tooltip: { defaultProps: { radius: 'md' } },
    // Labels a step heavier than the 500 body text (styles.css).
    InputWrapper: { styles: { label: { fontWeight: 600 }, description: { color: 'var(--mantine-color-dimmed)' } } },
    // Quiet tables: small spaced capitals in the head, more air in the rows.
    Table: {
      defaultProps: { verticalSpacing: 'sm', horizontalSpacing: 'lg', striped: false, highlightOnHover: true },
      styles: {
        table: { background: '#fff' },
        th: {
          fontSize: 11,
          fontWeight: 600,
          letterSpacing: '0.12em',
          textTransform: 'uppercase',
          color: 'var(--mantine-color-navy-5)',
          background: 'var(--mantine-color-navy-0)',
        },
      },
    },
  },
})

// Mantine writes its colour variables at runtime, after styles.css, so they are changed here (D1 review):
// secondary ("dimmed") text in a dark slate instead of the pale grey that faded on white, and quiet borders.
export const cssVariablesResolver: CSSVariablesResolver = () => ({
  variables: {},
  light: {
    '--mantine-color-dimmed': '#4a5568',
    '--mantine-color-default-border': 'rgba(17, 31, 61, 0.1)',
  },
  dark: {},
})
