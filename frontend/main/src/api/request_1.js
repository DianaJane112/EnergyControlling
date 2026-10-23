import axios from 'axios'
import { useToast } from '@/utils/toast.js'

const http = axios.create({
  baseURL: '/api',
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
  if (window.location.pathname.startsWith('/login')) return
  import('@/router/index.js').then(
    m => m.default.push('/login').catch(() => {}),
    () => { window.location.href = '/login' }
  )
}

export function translateError(msg) {
  if (!msg) return msg
  // 数据重复
  const dupMatch = msg.match(/数据重复[：:]\s*字段\[([^\]]+)\]/)
  if (dupMatch) {
    const field = dupMatch[1]
    const nameMap = {
      uk_group_name_merchant: '同一商户下分组名称',
      uk_group_name: '分组名称',
      db_no: '档案编号',
      steal_no: '钢印号',
      coll_code: '集中器编号',
    }
    const friendly = nameMap[field] || field
    return msg.replace(/字段\[[^\]]+\]/, `「${friendly}」`).replace(/的值\[([^\]]+)\]/, '「$1」')
  }
  return msg
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
    return Promise.reject(new Error(translateError(body.msg) || '请求失败'))
  },
  error => {
    const status = error.response?.status
    if (status === 401 || error.response?.data?.code === 401) {
      redirectToLogin()
      return Promise.reject(error)
    }

    const { error: showError } = useToast()

    if (status === 400) {
      const msg = translateError(error.response?.data?.msg) || '请求参数有误'
      showError(msg)
    } else if (status === 403) {
      showError('无权限访问该资源')
    } else if (status === 404) {
      showError('请求的资源不存在')
    } else if (status === 500) {
      showError('服务器内部错误，请稍后重试')
    } else if (status === 502 || status === 503) {
      showError('服务暂时不可用，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      showError('请求超时，请检查网络后重试')
    } else if (!status) {
      showError('网络连接失败，请检查网络')
    } else {
      showError('请求异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default http
