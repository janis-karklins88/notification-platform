import { env } from '../lib/env'
import { keycloak } from '../auth/keycloak'

type ApiRequestInit = RequestInit & {
  path: string
}

export async function apiFetch({ path, headers, ...init }: ApiRequestInit) {
  await keycloak.updateToken(30)
  const token = keycloak.token

  if (!token) {
    throw new Error('Auth token is missing')
  }

  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    let errorMessage = `API request failed with status ${response.status}`

    try {
      const errorResponse = (await response.json()) as { error?: string }
      if (errorResponse.error) {
        errorMessage = errorResponse.error
      }
    } catch {
      // Ignore JSON parsing failures and fall back to the status-based message.
    }

    throw new Error(errorMessage)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}
