import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authAPI, userAPI } from '../api'
import type { AuthResponse, AuthUser, LoginRequest, RegisterRequest, TargetRole } from '../types'
import { AUTH_RESET_EVENT, getStoredToken, resetAuthSession, setStoredToken } from '../utils/auth'

let resetListenerBound = false

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getStoredToken())
  const user = ref<AuthUser | null>(null)
  const initialized = ref(false)
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value) && Boolean(user.value))
  const needsOnboarding = computed(() => isAuthenticated.value && !user.value?.targetRole)

  function clearState() {
    token.value = ''
    user.value = null
  }

  function bindResetListener() {
    if (resetListenerBound || typeof window === 'undefined') {
      return
    }
    window.addEventListener(AUTH_RESET_EVENT, clearState)
    resetListenerBound = true
  }

  function applyAuthResponse(response: AuthResponse) {
    token.value = response.token
    user.value = response.user
    initialized.value = true
    setStoredToken(response.token)
  }

  async function bootstrap() {
    bindResetListener()
    if (initialized.value) {
      return
    }
    if (!token.value) {
      initialized.value = true
      return
    }

    loading.value = true
    try {
      user.value = await authAPI.me()
    } catch {
      resetAuthSession()
    } finally {
      initialized.value = true
      loading.value = false
    }
  }

  async function login(payload: LoginRequest) {
    const response = await authAPI.login(payload)
    applyAuthResponse(response)
    return response
  }

  async function register(payload: RegisterRequest) {
    const response = await authAPI.register(payload)
    applyAuthResponse(response)
    return response
  }

  async function refreshMe() {
    if (!token.value) {
      clearState()
      initialized.value = true
      return null
    }
    user.value = await authAPI.me()
    initialized.value = true
    return user.value
  }

  async function updateOnboarding(targetRole: TargetRole) {
    const response = await userAPI.updateOnboarding({ targetRole })
    if (user.value) {
      user.value = {
        ...user.value,
        targetRole: response.targetRole,
      }
    }
    return response
  }

  function logout() {
    initialized.value = true
    resetAuthSession()
  }

  bindResetListener()

  return {
    token,
    user,
    initialized,
    loading,
    isAuthenticated,
    needsOnboarding,
    bootstrap,
    login,
    register,
    refreshMe,
    updateOnboarding,
    logout,
  }
})
