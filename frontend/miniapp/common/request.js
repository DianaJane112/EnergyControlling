// 微信小程序 uni.request 统一封装

// 正式域名由构建环境注入；未配置时仅保留现有联调地址作为开发兜底。
// 禁止在小程序前端放置 WECHAT_SECRET。
const BASE_URL = process.env.VUE_APP_API_BASE_URL || 'https://1d0eb430.r27.cpolar.top'

// #ifdef H5
const BASE_URL_H5 = '/api'
// #endif

let isLogouting = false

const STATUS_MESSAGE = {
  400: '请求参数有误，请联系客服',
  401: '登录已过期，请重新登录',
  403: '暂无访问权限',
  404: '请求的服务不存在',
  408: '请求超时，请检查网络后重试',
  500: '服务器繁忙，请稍后重试',
  502: '服务器维护中，请稍后重试',
  503: '服务暂不可用，请稍后重试',
  504: '服务器响应超时，请稍后重试'
}

function getErrorMessage(statusCode, data) {
  if (statusCode === 200 && data) {
    return data.msg || '请求失败，请稍后重试'
  }
  return STATUS_MESSAGE[statusCode] || `服务异常（${statusCode}），请稍后重试`
}

export default function http(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    // #ifdef H5
    const url = BASE_URL_H5 + options.url
    // #endif
    // #ifdef MP-WEIXIN
    const url = BASE_URL + options.url
    // #endif

    if (process.env.NODE_ENV === 'development') {
      console.log(`[HTTP] ${options.method || 'GET'} ${url}`, options.data || '')
    }

    uni.request({
      url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'ngrok-skip-browser-warning': 'true',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.header
      },
      success(res) {
        const { statusCode, data } = res

        if (process.env.NODE_ENV === 'development') {
          console.log(`[HTTP] 响应 ${statusCode}:`, data)
        }

        if (statusCode === 200) {
          if (data.success || data.code === 0 || data.code === 200) {
            resolve(data.data)
            return
          }
          console.error(`[HTTP] 业务异常:`, data.msg || '请求失败', data)
          if (!options.silent) {
            uni.showToast({ title: data.msg || '请求失败，请稍后重试', icon: 'none', duration: 2500 })
          }
          reject(data)
          return
        }

        if (statusCode === 401) {
          console.error(`[HTTP] Token 过期，跳转登录`)
          uni.removeStorageSync('token')
          if (!isLogouting) {
            isLogouting = true
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject(data)
          return
        }

        console.error(`[HTTP] 服务器异常 ${statusCode}:`, data)
        if (!options.silent) {
          uni.showToast({ title: getErrorMessage(statusCode, data), icon: 'none', duration: 2500 })
        }
        reject(data)
      },
      fail(err) {
        console.error(`[HTTP] 网络异常:`, err)
        const netMsg = err.errMsg === 'request:fail timeout'
          ? '请求超时，请检查网络后重试'
          : '网络连接失败，请检查网络设置'
        uni.showToast({ title: netMsg, icon: 'none', duration: 2500 })
        reject(err)
      }
    })
  })
}
