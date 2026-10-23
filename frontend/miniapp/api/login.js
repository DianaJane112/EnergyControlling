import http from '@/common/request.js'

// ==================== 登录认证模块 ====================

/**
 * 微信登录
 * @param {string} code - wx.login() 获取的 code
 */
export function wechatLogin(code) {
  return http({
    url: '/api/v1/miniapp/auth/wechat-login',
    method: 'POST',
    data: { code }
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return http({
    url: '/api/v1/miniapp/auth/me',
    method: 'GET'
  })
}
