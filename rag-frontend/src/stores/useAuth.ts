import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'rag_token'
const USERNAME_KEY = 'rag_username'
const ROLES_KEY = 'rag_roles'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref(localStorage.getItem(USERNAME_KEY) || '')
  const roles = ref(localStorage.getItem(ROLES_KEY) || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'))

  function setAuth(newToken: string, newUsername: string, userRoles = '') {
    token.value = newToken
    username.value = newUsername
    roles.value = userRoles
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USERNAME_KEY, newUsername)
    localStorage.setItem(ROLES_KEY, userRoles)
  }

  function logout() {
    token.value = ''
    username.value = ''
    roles.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(ROLES_KEY)
  }

  return { token, username, roles, isLoggedIn, isAdmin, setAuth, logout }
})
