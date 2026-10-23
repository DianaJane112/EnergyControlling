import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser } from '@/api/auth.js'

const TOKEN_KEY = 'smalleel_token'
const USER_KEY = 'smalleel_user'

// 调试模式：后端不可用时临时设置 token 以绕过登录
const DEBUG_MODE = false
if (DEBUG_MODE && !localStorage.getItem(TOKEN_KEY)) {
  localStorage.setItem(TOKEN_KEY, 'debug-token')
  localStorage.setItem(USER_KEY, JSON.stringify({ username: 'admin', role: 'ADMIN', realName: '调试模式' }))
}

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

export function useAuth() {
  const isAuthenticated = computed(() => !!token.value)

  async function login(username, password) {
    const res = await loginApi(username, password)
    const data = res.data
    // 尝试所有可能的 token 字段
    const t = data.token || data.accessToken || data.access_token
      || (data.tokenInfo && data.tokenInfo.accessToken)
      || (data.tokenInfo && data.tokenInfo.access_token)
    console.log('[auth] token:', t ? t.substring(0, 20) + '...' : '!!! EMPTY !!!')
    const uname = data.username || data.userName || data.account || username
    const role = data.role || data.roles?.[0] || data.roleName
    const rname = data.realName || data.nickName || data.name
    const u = { username: uname, role, realName: rname }
    token.value = t
    user.value = u
    localStorage.setItem(TOKEN_KEY, t)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    return u
  }

  async function fetchUser() {
    try {
      const res = await getCurrentUser()
      const data = res.data
      console.log('[auth] getCurrentUser 返回:', JSON.stringify(data))
      // 兼容多种后端字段命名
      const username = data.username || data.userName || data.account
      const role = data.role || data.roles?.[0] || data.roleName
      const realName = data.realName || data.nickName || data.name
      if (username || role) {
        const u = {
          username: username || user.value?.username,
          role: role || user.value?.role,
          realName: realName || user.value?.realName,
        }
        user.value = u
        localStorage.setItem(USER_KEY, JSON.stringify(u))
      }
    } catch {
      // token 无效时静默失败，由响应拦截器处理跳转
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isAuthenticated, login, logout, fetchUser }
}
