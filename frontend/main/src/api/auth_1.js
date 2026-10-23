import http from './request.js'

export function login(username, password) {
  return http.post('/v1/auth/login', { username, password })
}

export function getCurrentUser() {
  return http.post('/v1/auth/me')
}

export function changePassword(oldPassword, newPassword) {
  return http.post('/v1/auth/change-password', { oldPassword, newPassword })
}

export function sendResetCode(username, phone) {
  return http.post('/v1/auth/send-reset-code', { username, phone })
}

export function resetPassword(username, code, newPassword) {
  return http.post('/v1/auth/reset-password', { username, code, newPassword })
}

// ════════════════════════════════════════════════════════════════════════
//  Mock（后端未就绪时临时使用）
// ════════════════════════════════════════════════════════════════════════

export function mockLogin(username = 'admin', password = '123456') {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        code: 200,
        msg: '操作成功',
        data: {
          token: 'mock-token-' + Date.now(),
          username,
          role: 'ADMIN',
          realName: '管理员',
        },
        success: true,
      })
    }, 300)
  })
}

export function mockGetCurrentUser() {
  return new Promise(resolve => {
    setTimeout(() => {
      resolve({
        code: 200,
        msg: '操作成功',
        data: {
          username: 'admin',
          role: 'ADMIN',
          realName: '管理员',
        },
        success: true,
      })
    }, 200)
  })
}
