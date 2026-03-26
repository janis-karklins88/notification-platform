import { env } from '../lib/env'

type ApiRequestInit = RequestInit & {
  path: string
}

export async function apiFetch({ path, headers, ...init }: ApiRequestInit) {
  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  })

  if (!response.ok) {
    throw new Error(`API request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}
