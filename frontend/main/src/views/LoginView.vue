<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth.js'
import { sendResetCode, resetPassword } from '@/api/auth.js'
import loginBgUrl from '@/assets/login-bg.png'

const router = useRouter()
const { login } = useAuth()

const account = ref('')
const password = ref('')
const showPassword = ref(false)
const accountError = ref('')
const passwordError = ref('')
const loading = ref(false)
const loginError = ref('')

function validate() {
  let valid = true
  if (!account.value.trim()) {
    accountError.value = '请输入账号'
    valid = false
  } else {
    accountError.value = ''
  }
  if (!password.value) {
    passwordError.value = '请输入密码'
    valid = false
  } else {
    passwordError.value = ''
  }
  return valid
}

async function handleLogin() {
  loginError.value = ''
  if (!validate()) return
  loading.value = true
  try {
    await login(account.value.trim(), password.value)
    const redirect = router.currentRoute.value.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch (e) {
    const backendMsg = e.response?.data?.msg
    loginError.value = backendMsg || e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

// ── 忘记密码 ────────────────────────────────────────
const showPwdReset = ref(false)
const resetStep = ref(1)    // 1=填信息 2=输入验证码
const resetLoading = ref(false)
const resetError = ref('')
const resetSuccess = ref(false)
const codeSent = ref(false)
const countdown = ref(0)
let countdownTimer = null

const resetForm = reactive({
  username: '',
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

function openResetDialog() {
  showPwdReset.value = true
  resetStep.value = 1
  resetError.value = ''
  resetSuccess.value = false
  codeSent.value = false
  countdown.value = 0
  Object.assign(resetForm, { username: '', phone: '', code: '', newPassword: '', confirmPassword: '' })
}

function closeResetDialog() {
  showPwdReset.value = false
  if (countdownTimer) clearInterval(countdownTimer)
}

async function handleSendCode() {
  resetError.value = ''
  if (!resetForm.username.trim()) {
    resetError.value = '请输入账号'
    return
  }
  if (!resetForm.phone.trim()) {
    resetError.value = '请输入手机号'
    return
  }
  resetLoading.value = true
  try {
    await sendResetCode(resetForm.username.trim(), resetForm.phone.trim())
    codeSent.value = true
    resetStep.value = 2
    countdown.value = 60
    countdownTimer = setInterval(() => {
      if (countdown.value > 0) countdown.value--
      else clearInterval(countdownTimer)
    }, 1000)
  } catch (e) {
    resetError.value = e.response?.data?.msg || e.message || '发送失败'
  } finally {
    resetLoading.value = false
  }
}

async function handleResetPassword() {
  resetError.value = ''
  if (!resetForm.code.trim()) {
    resetError.value = '请输入验证码'
    return
  }
  if (!resetForm.newPassword) {
    resetError.value = '请输入新密码'
    return
  }
  if (resetForm.newPassword.length < 6 || resetForm.newPassword.length > 20) {
    resetError.value = '新密码长度需为 6-20 位'
    return
  }
  if (resetForm.newPassword !== resetForm.confirmPassword) {
    resetError.value = '两次输入的新密码不一致'
    return
  }
  resetLoading.value = true
  try {
    await resetPassword(resetForm.username.trim(), resetForm.code.trim(), resetForm.newPassword)
    resetSuccess.value = true
    if (countdownTimer) clearInterval(countdownTimer)
  } catch (e) {
    resetError.value = e.response?.data?.msg || e.message || '重置失败'
  } finally {
    resetLoading.value = false
  }
}

function backToLogin() {
  closeResetDialog()
}

// ── 微信登录 ────────────────────────────────────────
const wechatAppId = 'wxb1e7e4d8a1f2c3d4' // TODO: 替换为实际 AppID
const wechatRedirectUri = computed(() => encodeURIComponent(
  new URL('login', `${window.location.origin}${import.meta.env.BASE_URL}`).href
))

function handleWechatLogin() {
  const url = `https://open.weixin.qq.com/connect/qrconnect?appid=${wechatAppId}&redirect_uri=${wechatRedirectUri.value}&response_type=code&scope=snsapi_login&state=wechat_${Date.now()}`
  window.location.href = url
}
</script>

<template>
  <div class="login-page" :style="{ backgroundImage: `url(${loginBgUrl})` }">
    <div class="login-bg-overlay"></div>

    <div class="login-content">
      <div class="login-left">
        <p class="login-left-sub">智能物业管理平台</p>
        <h1 class="login-left-title">明思管家</h1>
        <p class="login-left-desc">高效 · 智能 · 便捷，让物业管理更简单</p>
      </div>

      <div class="login-right">
        <div class="login-form-inner">
          <h2 class="login-form-title">欢迎登录</h2>

          <div class="login-field">
            <div class="login-input-line">
              <span class="login-input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </span>
              <input v-model="account" type="text" class="login-input" placeholder="请输入账号" @input="accountError = ''" @keyup.enter="handleLogin" />
            </div>
            <p class="login-field-error">{{ accountError || '' }}</p>
          </div>

          <div class="login-field">
            <div class="login-input-line">
              <span class="login-input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </span>
              <input v-model="password" :type="showPassword ? 'text' : 'password'" class="login-input" placeholder="请输入密码" @input="passwordError = ''" @keyup.enter="handleLogin" />
              <span class="login-pwd-eye" @click="showPassword = !showPassword">
                <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
              </span>
            </div>
            <p class="login-field-error">{{ passwordError || '' }}</p>
          </div>

          <a class="login-forgot" href="javascript:void(0)" @click="openResetDialog">忘记密码？</a>

          <p v-if="loginError" class="login-error-msg">{{ loginError }}</p>

          <button class="login-btn" :disabled="loading" @click="handleLogin">
            <svg v-if="!loading" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <span v-else class="login-spinner"></span>
            {{ loading ? '登录中...' : '登 录' }}
          </button>

          <div class="login-wechat">
            <div class="login-wechat-divider">
              <span class="login-wechat-line"></span>
              <span class="login-wechat-text">其他登录方式</span>
              <span class="login-wechat-line"></span>
            </div>
            <button class="login-wechat-btn" @click="handleWechatLogin">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="#07c160"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.324.324 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.49.49 0 0 1 .178-.554C23.028 18.48 24 16.82 24 14.98c0-3.21-2.931-5.952-7.062-6.122zm-2.18 2.769c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982z"/></svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 忘记密码弹窗 -->
    <Teleport to="body">
      <Transition name="dialog">
        <div v-if="showPwdReset" class="fixed inset-0 z-[200] flex items-center justify-center" @click.self="closeResetDialog">
          <div class="bg-black/50 absolute inset-0"></div>
          <div class="relative bg-white rounded-2xl shadow-2xl w-[440px] max-w-[92vw] overflow-hidden">
            <!-- 头部 -->
            <div class="flex items-center gap-3 px-6 py-5 border-b border-gray-100">
              <button class="flex items-center justify-center w-8 h-8 rounded-lg hover:bg-gray-100 transition-colors text-gray-400 hover:text-gray-600" @click="closeResetDialog">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
              </button>
              <span class="text-base font-semibold text-[#1a1a2e]">重置密码</span>
              <div class="flex gap-1.5 ml-auto">
                <span class="w-6 h-1 rounded-full" :class="resetStep === 1 ? 'bg-[#3a64ab]' : 'bg-[#d9d9d9]'"></span>
                <span class="w-6 h-1 rounded-full" :class="resetStep === 2 && !resetSuccess ? 'bg-[#3a64ab]' : 'bg-[#d9d9d9]'"></span>
              </div>
            </div>

            <!-- 表单态 -->
            <div v-if="!resetSuccess" class="px-6 py-6">
              <div class="flex flex-col gap-5">
                <!-- 账号 -->
                <div>
                  <label class="block text-[13px] font-medium text-[#555] mb-2">账号</label>
                  <div class="flex items-center gap-3 px-4 h-11 bg-[#f7f8fa] rounded-lg border border-transparent transition-colors focus-within:bg-white focus-within:border-[#3a64ab]">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    <input v-model="resetForm.username" type="text" class="flex-1 bg-transparent border-none outline-none text-[14px] text-[#333] placeholder:text-[#bbb]" placeholder="请输入账号" />
                  </div>
                </div>

                <!-- 手机号 -->
                <div>
                  <label class="block text-[13px] font-medium text-[#555] mb-2">绑定手机号</label>
                  <div class="flex items-center gap-3 px-4 h-11 bg-[#f7f8fa] rounded-lg border border-transparent transition-colors focus-within:bg-white focus-within:border-[#3a64ab]">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
                    <input v-model="resetForm.phone" type="text" class="flex-1 bg-transparent border-none outline-none text-[14px] text-[#333] placeholder:text-[#bbb]" placeholder="请输入绑定手机号" />
                  </div>
                </div>

                <template v-if="resetStep === 2">
                  <!-- 验证码 -->
                  <div>
                    <label class="block text-[13px] font-medium text-[#555] mb-2">短信验证码</label>
                    <div class="flex gap-3">
                      <div class="flex items-center gap-3 px-4 h-11 bg-[#f7f8fa] rounded-lg border border-transparent flex-1 transition-colors focus-within:bg-white focus-within:border-[#3a64ab]">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                        <input v-model="resetForm.code" type="text" maxlength="6" class="flex-1 bg-transparent border-none outline-none text-[14px] text-[#333] placeholder:text-[#bbb]" placeholder="6位验证码" />
                      </div>
                      <button
                        class="shrink-0 h-11 px-4 text-[13px] font-medium rounded-lg transition-all"
                        :class="countdown > 0 ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : 'bg-[#f0f3ff] text-[#3a64ab] hover:bg-[#e0e5ff]'"
                        :disabled="countdown > 0 || resetLoading"
                        @click="handleSendCode"
                      >{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</button>
                    </div>
                  </div>

                  <!-- 新密码 -->
                  <div>
                    <label class="block text-[13px] font-medium text-[#555] mb-2">新密码</label>
                    <div class="flex items-center gap-3 px-4 h-11 bg-[#f7f8fa] rounded-lg border border-transparent transition-colors focus-within:bg-white focus-within:border-[#3a64ab]">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                      <input v-model="resetForm.newPassword" type="password" class="flex-1 bg-transparent border-none outline-none text-[14px] text-[#333] placeholder:text-[#bbb]" placeholder="6-20位新密码" />
                    </div>
                  </div>

                  <!-- 确认密码 -->
                  <div>
                    <label class="block text-[13px] font-medium text-[#555] mb-2">确认新密码</label>
                    <div class="flex items-center gap-3 px-4 h-11 bg-[#f7f8fa] rounded-lg border border-transparent transition-colors focus-within:bg-white focus-within:border-[#3a64ab]">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                      <input v-model="resetForm.confirmPassword" type="password" class="flex-1 bg-transparent border-none outline-none text-[14px] text-[#333] placeholder:text-[#bbb]" placeholder="再次输入新密码" @keyup.enter="handleResetPassword" />
                    </div>
                  </div>
                </template>

                <p v-if="resetError" class="text-[13px] text-red-500 bg-red-50 px-4 py-2.5 rounded-lg">{{ resetError }}</p>

                <!-- 操作按钮 -->
                <button
                  v-if="resetStep === 1"
                  class="w-full h-11 bg-[#3a64ab] text-white text-[14px] font-medium rounded-lg hover:bg-[#264c8d] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                  :disabled="resetLoading"
                  @click="handleSendCode"
                >{{ resetLoading ? '发送中...' : '获取验证码' }}</button>
                <button
                  v-else
                  class="w-full h-11 bg-[#3a64ab] text-white text-[14px] font-medium rounded-lg hover:bg-[#264c8d] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                  :disabled="resetLoading"
                  @click="handleResetPassword"
                >{{ resetLoading ? '提交中...' : '重置密码' }}</button>
              </div>
            </div>

            <!-- 成功态 -->
            <div v-else class="px-6 py-10 text-center">
              <div class="w-16 h-16 mx-auto mb-5 rounded-full bg-[#f0fdf4] flex items-center justify-center">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2.5"><path d="M20 6L9 17l-5-5"/></svg>
              </div>
              <h3 class="text-lg font-semibold text-[#1a1a2e] mb-2">密码重置成功</h3>
              <p class="text-sm text-gray-400 mb-8">请使用新密码重新登录</p>
              <button class="w-full h-11 bg-[#3a64ab] text-white text-[14px] font-medium rounded-lg hover:bg-[#264c8d] transition-colors" @click="backToLogin">返回登录</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  background-repeat: no-repeat;
  background-position: center center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.48) 0%, rgba(0, 0, 0, 0.2) 50%, rgba(0, 0, 0, 0.45) 100%);
  backdrop-filter: blur(2px);
}

.login-content {
  position: relative;
  z-index: 1;
  display: flex;
  width: 960px;
  max-width: 94vw;
  min-height: 520px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.3);
}

.login-left {
  flex: 1;
  background: linear-gradient(160deg, rgba(58, 100, 171, 0.88) 0%, rgba(30, 58, 110, 0.92) 100%);
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 48px;
  color: #fff;
}

.login-left-sub { font-size: 14px; opacity: 0.75; letter-spacing: 3px; margin: 0 0 8px; }
.login-left-title { font-size: 36px; font-weight: 700; margin: 0 0 16px; letter-spacing: 4px; }
.login-left-desc { font-size: 15px; font-weight: 300; opacity: 0.85; margin: 0; line-height: 1.8; }

.login-right {
  flex: 1;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.login-form-inner { width: 100%; max-width: 340px; }
.login-form-title { font-size: 26px; font-weight: 600; color: #1a1a2e; margin: 0 0 40px; letter-spacing: 1px; }

.login-field { margin-bottom: 20px; }

.login-input-line {
  display: flex; align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  transition: border-color 0.3s;
}
.login-input-line:focus-within { border-bottom-color: #3a64ab; }

.login-input-icon { color: #888; display: flex; align-items: center; flex-shrink: 0; margin-right: 12px; transition: color 0.3s; }
.login-input-line:focus-within .login-input-icon { color: #3a64ab; }

.login-input { flex: 1; border: none; outline: none; font-size: 15px; color: #222; background: transparent; padding: 4px 0; }
.login-input::placeholder { color: #aaa; font-weight: 300; }

.login-pwd-eye { color: #aaa; display: flex; align-items: center; flex-shrink: 0; cursor: pointer; padding: 4px; transition: color 0.2s; }
.login-pwd-eye:hover { color: #3a64ab; }

.login-field-error { font-size: 12px; color: #ff4d4f; min-height: 18px; line-height: 18px; margin: 6px 0 0 30px; }

.login-forgot {
  display: block; text-align: right; font-size: 13px; color: #3a64ab;
  text-decoration: none; margin-top: -6px; margin-bottom: 12px; transition: color 0.2s;
}
.login-forgot:hover { color: #264c8d; }

.login-error-msg { font-size: 13px; color: #ff4d4f; text-align: center; margin: 8px 0; }

.login-btn {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  width: 100%; height: 48px; background: #3a64ab; color: #fff;
  border: none; border-radius: 24px; font-size: 16px; font-weight: 500;
  cursor: pointer; margin-top: 24px; letter-spacing: 2px;
  transition: background 0.3s, transform 0.2s;
}
.login-btn:hover:not(:disabled) { background: #264c8d; transform: translateY(-1px); }
.login-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.login-spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.login-wechat { margin-top: 36px; }
.login-wechat-divider { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.login-wechat-line { flex: 1; height: 1px; background: rgba(0, 0, 0, 0.08); }
.login-wechat-text { font-size: 12px; color: #aaa; white-space: nowrap; }

.login-wechat-btn {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 44px; background: transparent;
  border: 1px solid rgba(0, 0, 0, 0.08); border-radius: 22px;
  cursor: pointer; transition: border-color 0.3s, background 0.3s;
}
.login-wechat-btn:hover { border-color: #07c160; background: rgba(7, 193, 96, 0.04); }

/* ── 重置密码弹窗 ──────────────────────────── */
.reset-input {
  width: 100%; height: 44px; padding: 0 14px;
  border: 1px solid #e0e0e0; border-radius: 8px;
  font-size: 14px; outline: none; box-sizing: border-box;
  transition: border-color 0.2s;
}
.reset-input:focus { border-color: #3a64ab; }
.reset-input::placeholder { color: #aaa; }

.dialog-enter-active, .dialog-leave-active { transition: opacity 0.2s; }
.dialog-enter-active > :last-child,
.dialog-leave-active > :last-child { transition: transform 0.2s, opacity 0.2s; }
.dialog-enter-from, .dialog-leave-to { opacity: 0; }
.dialog-enter-from > :last-child { transform: scale(0.95); opacity: 0; }
.dialog-leave-to > :last-child { transform: scale(0.95); opacity: 0; }

@media (max-width: 768px) {
  .login-content { flex-direction: column; min-height: auto; max-width: 88vw; }
  .login-left { padding: 36px 32px; text-align: center; }
  .login-left-title { font-size: 28px; }
  .login-right { padding: 36px 32px; }
}
</style>
