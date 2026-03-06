import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref({
    id: '',
    name: '',
    email: '',
    isAuthenticated: false
  })

  function setUser(userData: { id: string; name: string; email: string }) {
    user.value.id = userData.id
    user.value.name = userData.name
    user.value.email = userData.email
    user.value.isAuthenticated = true
  }

  function logout() {
    user.value.id = ''
    user.value.name = ''
    user.value.email = ''
    user.value.isAuthenticated = false
  }

  return { user, setUser, logout }
})