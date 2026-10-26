import { Alert, Anchor, Button, Paper, PasswordInput, SegmentedControl, Stack, Text, TextInput, Title } from '@mantine/core'
import { DateInput } from '@mantine/dates'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { homePathOf, useRegister } from './session'

type AccountType = 'VOLUNTEER' | 'ORGANIZATION'

// The same limits as the backend DTOs, so most mistakes are caught before the request.
const required = (label: string) => (v: string) => (v.trim() ? null : `${label} is required`)

export function RegisterPage() {
  const [accountType, setAccountType] = useState<AccountType>('VOLUNTEER')
  const [today] = useState(() => new Date())
  const navigate = useNavigate()
  const register = useRegister()

  const form = useForm({
    initialValues: {
      username: '',
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      dateOfBirth: null as string | null,
      organizationName: '',
    },
    validate: {
      username: (v) => (v.trim().length >= 3 && v.trim().length <= 25 ? null : 'Between 3 and 25 characters'),
      email: (v) => (/^\S+@\S+\.\S+$/.test(v) ? null : 'Enter a valid email'),
      password: (v) => (v.length >= 4 && v.length <= 35 ? null : 'Between 4 and 35 characters'),
      firstName: (v) => (accountType === 'VOLUNTEER' ? required('First name')(v) : null),
      lastName: (v) => (accountType === 'VOLUNTEER' ? required('Last name')(v) : null),
      dateOfBirth: (v) => (accountType === 'VOLUNTEER' && !v ? 'Date of birth is required' : null),
      organizationName: (v) => (accountType === 'ORGANIZATION' ? required('Organization name')(v) : null),
    },
  })

  const submit = form.onSubmit((values) => {
    const credentials = { username: values.username.trim(), email: values.email.trim(), password: values.password }
    const registration =
      accountType === 'VOLUNTEER'
        ? {
            role: 'VOLUNTEER' as const,
            request: {
              ...credentials,
              firstName: values.firstName.trim(),
              lastName: values.lastName.trim(),
              dateOfBirth: values.dateOfBirth ?? '',
            },
          }
        : { role: 'ORGANIZATION' as const, request: { ...credentials, organizationName: values.organizationName.trim() } }

    register.mutate(registration, {
      onSuccess: () => navigate(homePathOf(accountType), { replace: true }),
      // Field messages from the backend's validation go next to the fields.
      onError: (e) => e instanceof ApiError && form.setErrors(e.fieldErrors),
    })
  })

  return (
    <Paper maw={480} mx="auto" mt="xl" p="lg" withBorder>
      <form onSubmit={submit}>
        <Stack>
          <Title order={2}>Create an account</Title>
          <SegmentedControl
            value={accountType}
            onChange={(v) => setAccountType(v as AccountType)}
            data={[
              { value: 'VOLUNTEER', label: 'Volunteer' },
              { value: 'ORGANIZATION', label: 'Organization' },
            ]}
          />
          {register.error && <Alert color="wine">{register.error.message}</Alert>}
          <TextInput label="Username" autoComplete="username" {...form.getInputProps('username')} />
          <TextInput label="Email" type="email" autoComplete="email" {...form.getInputProps('email')} />
          <PasswordInput label="Password" autoComplete="new-password" {...form.getInputProps('password')} />
          {accountType === 'VOLUNTEER' ? (
            <>
              <TextInput label="First name" {...form.getInputProps('firstName')} />
              <TextInput label="Last name" {...form.getInputProps('lastName')} />
              <DateInput
                label="Date of birth"
                valueFormat="YYYY-MM-DD"
                maxDate={today}
                {...form.getInputProps('dateOfBirth')}
              />
            </>
          ) : (
            <TextInput label="Organization name" {...form.getInputProps('organizationName')} />
          )}
          <Button type="submit" loading={register.isPending}>
            Register
          </Button>
          <Text size="sm">
            Already registered?{' '}
            <Anchor component={Link} to="/login">
              Log in
            </Anchor>
          </Text>
        </Stack>
      </form>
    </Paper>
  )
}
