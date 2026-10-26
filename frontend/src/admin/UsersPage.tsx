import { Badge, Button, Center, Group, Loader, Modal, Pagination, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { notifications } from '@mantine/notifications'
import dayjs from 'dayjs'
import { useState } from 'react'
import type { AdminUser } from '../api/types'
import { ErrorAlert } from '../common/ErrorAlert'
import { useAdminUsers, useBanAction } from './queries'
import { banConsequences, initialFilter, roleLabel, roleOfUser, userActionOf, type UserFilter } from './users'

const ROLE_OPTIONS = [
  { value: 'ALL', label: 'All roles' },
  { value: 'ROLE_VOLUNTEER', label: 'Volunteer' },
  { value: 'ROLE_ORGANIZATION', label: 'Organization' },
  { value: 'ROLE_ADMIN', label: 'Admin' },
]
const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Any status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'BANNED', label: 'Banned' },
]

// Q119 … Q121: user accounts with search, filters, ban and unban (Q29, Q30).
export function UsersPage() {
  const [search, setSearch] = useState('')
  const [debounced] = useDebouncedValue(search, 300)
  const [filter, setFilter] = useState<Omit<UserFilter, 'query'>>(initialFilter)
  const [banning, setBanning] = useState<AdminUser | null>(null)
  const users = useAdminUsers({ ...filter, query: debounced })
  const banAction = useBanAction()

  // A changed filter starts again at the first page.
  const change = (patch: Partial<Omit<UserFilter, 'query'>>) => setFilter((f) => ({ ...f, page: 0, ...patch }))

  const run = (user: AdminUser, action: 'ban' | 'unban') =>
    banAction.mutate(
      { userId: user.id, action },
      {
        onSuccess: () => {
          setBanning(null)
          notifications.show({
            color: 'sage',
            message:
              action === 'ban'
                ? `${user.username} is banned`
                : `${user.username} can log in again. Nothing withdrawn or cancelled is restored.`,
          })
        },
        onError: (e) => {
          setBanning(null)
          notifications.show({ color: 'wine', title: 'Not done', message: e.message })
        },
      },
    )

  return (
    <Stack maw={1060}>
      <Title order={2}>Users</Title>
      <Group align="end">
        <TextInput
          label="Search"
          placeholder="Part of username or email"
          value={search}
          onChange={(e) => {
            setSearch(e.currentTarget.value)
            change({})
          }}
          w={280}
        />
        <Select
          label="Role"
          data={ROLE_OPTIONS}
          value={filter.role}
          onChange={(v) => v && change({ role: v as UserFilter['role'] })}
          allowDeselect={false}
          w={160}
        />
        <Select
          label="Status"
          data={STATUS_OPTIONS}
          value={filter.status}
          onChange={(v) => v && change({ status: v as UserFilter['status'] })}
          allowDeselect={false}
          w={150}
        />
      </Group>
      {users.isPending && (
        <Center mt="xl">
          <Loader />
        </Center>
      )}
      <ErrorAlert error={users.error} />
      {users.data && (
        <>
          <Text size="sm" c="dimmed">
            {users.data.totalElements} {users.data.totalElements === 1 ? 'user' : 'users'}
          </Text>
          {users.data.content.length > 0 && (
            <Table striped>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Username</Table.Th>
                  <Table.Th>Email</Table.Th>
                  <Table.Th>Role</Table.Th>
                  <Table.Th>Registered</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {users.data.content.map((user) => {
                  const role = roleOfUser(user)
                  const action = userActionOf(user)
                  return (
                    <Table.Tr key={user.id} data-testid="user-row">
                      <Table.Td>{user.username}</Table.Td>
                      <Table.Td>{user.email}</Table.Td>
                      <Table.Td>{role ? roleLabel[role] : '?'}</Table.Td>
                      <Table.Td>{dayjs(user.createdAt).format('D MMM YYYY')}</Table.Td>
                      <Table.Td>
                        <Badge color={user.banned ? 'wine' : 'sage'} variant="light">
                          {user.banned ? 'Banned' : 'Active'}
                        </Badge>
                      </Table.Td>
                      <Table.Td>
                        {action === 'ban' && (
                          <Button size="xs" color="wine" variant="light" onClick={() => setBanning(user)}>
                            Ban
                          </Button>
                        )}
                        {action === 'unban' && (
                          <Button
                            size="xs"
                            variant="default"
                            loading={banAction.isPending && banAction.variables?.userId === user.id}
                            onClick={() => run(user, 'unban')}
                          >
                            Unban
                          </Button>
                        )}
                      </Table.Td>
                    </Table.Tr>
                  )
                })}
              </Table.Tbody>
            </Table>
          )}
          {users.data.totalPages > 1 && (
            // Mantine counts pages from 1, the backend from 0.
            <Pagination total={users.data.totalPages} value={filter.page + 1} onChange={(p) => setFilter((f) => ({ ...f, page: p - 1 }))} />
          )}
        </>
      )}

      <Modal opened={banning !== null} onClose={() => setBanning(null)} title={banning && `Ban ${banning.username}?`}>
        {banning && (
          <Stack>
            <Text size="sm">{banConsequences(banning)}</Text>
            <Text size="sm" c="dimmed">
              Unbanning later restores nothing.
            </Text>
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setBanning(null)}>
                Back
              </Button>
              <Button color="wine" loading={banAction.isPending} onClick={() => run(banning, 'ban')}>
                Ban
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}
