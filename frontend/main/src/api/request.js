import axios from 'axios'

const appBase = import.meta.env.BASE_URL.replace(/\/$/, '')

const http = axios.create({
  baseURL: `${appBase}/api`,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'ngrok-skip-browser-warning': true,
  },
})

// 请求拦截器：自动携带 Bearer Token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('smalleel_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const DEBUG_MODE = localStorage.getItem('smalleel_token') === 'debug-token'

function redirectToLogin() {
  if (DEBUG_MODE) return
  localStorage.removeItem('smalleel_token')
  localStorage.removeItem('smalleel_user')
  if (window.location.pathname.startsWith(`${appBase}/login`)) return
  import('@/router/index.js').then(
    m => m.default.push('/login').catch(() => {}),
    () => { window.location.href = `${import.meta.env.BASE_URL}login` }
  )
}

// 响应拦截器：统一处理业务 & 网络错误
http.interceptors.response.use(
  res => {
    const body = res.data
    if (res.status === 204 || !body) return { success: true, code: 204, data: null }
    if (body.success === true || body.code === 1073741824) return body
    if (body.code === 200) return body
    if (body.code === 401) {
      console.log('[response] 业务 401')
      redirectToLogin()
      return Promise.reject(new Error(body.msg || 'Token 无效或已过期'))
    }
    return Promise.reject(new Error(body.msg || '请求失败'))
  },
  error => {
    const status = error.response?.status
    if (status === 401 || error.response?.data?.code === 401) {
      redirectToLogin()
    }
    if (status === 404) {
      console.error('请求的资源不存在 (404)')
    } else if (status === 500) {
      console.error('服务器内部错误 (500)')
    } else if (status === 403) {
      console.error('无权限访问 (403)')
    } else if (!status) {
      console.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default http
