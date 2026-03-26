const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''

export const env = {
  apiBaseUrl,
} as const
