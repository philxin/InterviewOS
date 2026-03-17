export const TOKEN_STORAGE_KEY = 'interviewos.token'
export const AUTH_RESET_EVENT = 'interviewos:auth-reset'

export function getStoredToken(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.localStorage.getItem(TOKEN_STORAGE_KEY) ?? ''
}

export function setStoredToken(token: string) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function resetAuthSession() {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.removeItem(TOKEN_STORAGE_KEY)
  window.dispatchEvent(new Event(AUTH_RESET_EVENT))
}
