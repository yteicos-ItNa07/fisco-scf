import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '@/api/auth'

const DEMO_ACCOUNTS = {
  huawei_admin: {
    username: 'huawei_admin',
    password: '123456',
    realName: '华为管理员',
    role: 'CORE_ENTERPRISE',
    enterprise: {
      id: 1,
      name: '华为技术有限公司',
      role: 'CORE_ENTERPRISE'
    }
  },
  supplier1: {
    username: 'supplier1',
    password: '123456',
    realName: '精密零件管理员',
    role: 'SUPPLIER',
    enterprise: {
      id: 3,
      name: '深圳市精密零件有限公司',
      role: 'SUPPLIER'
    }
  },
  icbc_admin: {
    username: 'icbc_admin',
    password: '123456',
    realName: '工商银行管理员',
    role: 'FINANCIAL_INST',
    enterprise: {
      id: 7,
      name: '中国工商银行深圳分行',
      role: 'FINANCIAL_INST'
    }
  }
}

function applyAuthSession(tokenValue, userValue) {
  const token = tokenValue || 'demo-token'
  const user = userValue || { username: 'demo-user' }

  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))

  return { token, user }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const enterprise = computed(() => user.value?.enterprise)
  const enterpriseRole = computed(() => user.value?.enterprise?.role)

  async function login(username, password) {
    try {
      const res = await apiLogin({ username, password })
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      return res
    } catch (error) {
      const account = DEMO_ACCOUNTS[username]
      if (account && account.password === password) {
        const demoUser = {
          id: account.enterprise.id,
          username: account.username,
          realName: account.realName,
          role: account.role,
          enterprise: account.enterprise
        }
        const demoToken = `demo-${account.username}-${Date.now()}`
        token.value = demoToken
        user.value = demoUser
        applyAuthSession(demoToken, demoUser)
        return {
          data: {
            token: demoToken,
            user: demoUser
          }
        }
      }

      throw error
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    isLoggedIn,
    enterprise,
    enterpriseRole,
    login,
    logout
  }
})
