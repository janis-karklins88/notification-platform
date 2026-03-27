import { env } from '../lib/env'
import { keycloak } from '../auth/keycloak'

type ApiRequestInit = RequestInit & {
  path: string
}

export async function apiFetch({ path, headers, ...init }: ApiRequestInit) {

  await keycloak.updateToken(30)
  const token = keycloak.token;

  if(!token){
    throw new Error('Auth token is missing');
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
    throw new Error(`API request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}
