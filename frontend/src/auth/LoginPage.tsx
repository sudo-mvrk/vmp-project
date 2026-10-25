import { Alert, Anchor, Button, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { Link, Navigate, useSearchParams } from 'react-router'
import { homePathOf, roleOf, useCurrentUser, useLogin } from './session'

// Only paths inside the app are followed after login, never a full URL from the query string.
function safeNext(next: string | null): string | null {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : null
}

export function LoginPage() {
  const [params] = useSearchParams()
  const { data: user } = useCurrentUser()
  const login = useLogin()
  const next = safeNext(params.get('next'))

  const form = useForm({
    initialValues: { username: '', password: '' },
    validate: {
      username: (v) => (v.trim() ? null : 'Enter your username or email'),
      password: (v) => (v ? null : 'Enter your password'),
    },
  })

  // Also the way out after a successful login: the refetched current user lands here.
  if (user && !login.isPending) {
    return <Navigate to={next ?? homePathOf(roleOf(user))} replace />
  }

  const submit = form.onSubmit((values) => login.mutate(values))

  return (
    <Paper maw={420} mx="auto" mt="xl" p="lg" withBorder>
      <form onSubmit={submit}>
        <Stack>
          <Title order={2}>Log in</Title>
          {login.error && <Alert color="red">{login.error.message}</Alert>}
          <TextInput label="Username or email" autoComplete="username" {...form.getInputProps('username')} />
          <PasswordInput label="Password" autoComplete="current-password" {...form.getInputProps('password')} />
          <Button type="submit" loading={login.isPending}>
            Log in
          </Button>
          <Text size="sm">
            No account yet?{' '}
            <Anchor component={Link} to="/register">
              Register
            </Anchor>
          </Text>
        </Stack>
      </form>
    </Paper>
  )
}
