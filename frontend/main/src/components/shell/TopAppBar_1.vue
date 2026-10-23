<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth.js'
import { changePassword as changePasswordApi } from '@/api/auth.js'

defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
})

const router = useRouter()
const { user, logout } = useAuth()

const selectedProject = ref('')
const showUserPopup = ref(false)
const showPwdDialog = ref(false)
const pwdSuccess = ref(false)
const pwdLoading = ref(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const pwdError = ref('')

function openPwdDialog() {
  showUserPopup.value = false
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdError.value = ''
  pwdSuccess.value = false
  showPwdDialog.value = true
}

function goToLogin() {
  window.location.replace('/login')
}

function closePwdDialog() {
  showPwdDialog.value = false
}

async function handleChangePassword() {
  pwdError.value = ''
  if (!pwdForm.oldPassword) {
    pwdError.value = '请输入原密码'
    return
  }
  if (!pwdForm.newPassword) {
    pwdError.value = '请输入新密码'
    return
  }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    pwdError.value = '新密码长度需为 6-20 位'
    return
  }
  if (pwdForm.newPassword === pwdForm.oldPassword) {
    pwdError.value = '新密码不能与原密码相同'
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  pwdLoading.value = true
  try {
    await changePasswordApi(pwdForm.oldPassword, pwdForm.newPassword)
    pwdSuccess.value = true
    logout()
  } catch (e) {
    const backendMsg = e.response?.data?.msg
    pwdError.value = backendMsg || e.message || '修改失败'
  } finally {
    pwdLoading.value = false
  }
}

function handleLogout() {
  logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <header class="h-[60px] bg-[#3a64ab] text-white flex justify-end items-center px-5 text-[13px] shrink-0">

    <!-- 用户区：hover 弹出气泡 -->
    <div class="relative ml-5">
      <span
        class="cursor-pointer select-none"
        @mouseenter="showUserPopup = true"
        @mouseleave="showUserPopup = false"
      >{{ user?.realName || user?.username || 'admin' }}</span>

      <Transition name="popup">
        <div
          v-if="showUserPopup"
          class="absolute top-full right-0 mt-2 bg-white rounded-lg shadow-lg border border-gray-100 py-1 min-w-[120px] z-50"
          @mouseenter="showUserPopup = true"
          @mouseleave="showUserPopup = false"
        >
          <div
            class="px-4 py-2.5 text-[#333] text-[13px] cursor-pointer hover:bg-[#f5f6fb] transition-colors flex items-center gap-2"
            @click="openPwdDialog"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <span>修改密码</span>
          </div>
        </div>
      </Transition>
    </div>

    <div class="ml-5 cursor-pointer" title="退出登录" @click="handleLogout">&#x23FB;</div>
  </header>

  <!-- 修改密码弹窗 -->
  <Teleport to="body">
    <Transition name="dialog">
      <div v-if="showPwdDialog" class="fixed inset-0 z-[200] flex items-center justify-center" @click.self="closePwdDialog">
        <div class="bg-black/40 absolute inset-0"></div>
        <div class="relative bg-white rounded-xl shadow-2xl w-[400px] max-w-[90vw] p-8">
          <!-- 表单态 -->
          <template v-if="!pwdSuccess">
            <h3 class="text-lg font-semibold text-[#1a1a2e] mb-6">修改密码</h3>

            <div class="flex flex-col gap-4">
              <div>
                <input
                  v-model="pwdForm.oldPassword"
                  type="password"
                  placeholder="原密码"
                  class="w-full h-11 px-4 border border-gray-200 rounded-lg text-sm outline-none focus:border-[#3a64ab] transition-colors"
                />
              </div>
              <div>
                <input
                  v-model="pwdForm.newPassword"
                  type="password"
                  placeholder="新密码（6-20位）"
                  class="w-full h-11 px-4 border border-gray-200 rounded-lg text-sm outline-none focus:border-[#3a64ab] transition-colors"
                />
              </div>
              <div>
                <input
                  v-model="pwdForm.confirmPassword"
                  type="password"
                  placeholder="确认新密码"
                  class="w-full h-11 px-4 border border-gray-200 rounded-lg text-sm outline-none focus:border-[#3a64ab] transition-colors"
                  @keyup.enter="handleChangePassword"
                />
              </div>
              <p v-if="pwdError" class="text-xs text-red-500">{{ pwdError }}</p>
            </div>

            <div class="flex justify-end gap-3 mt-6">
              <button
                class="px-5 py-2 text-sm text-[#666] bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
                @click="closePwdDialog"
              >取消</button>
              <button
                class="px-5 py-2 text-sm text-white bg-[#3a64ab] rounded-lg hover:bg-[#264c8d] transition-colors disabled:opacity-60"
                :disabled="pwdLoading"
                @click="handleChangePassword"
              >{{ pwdLoading ? '提交中...' : '确认修改' }}</button>
            </div>
          </template>

          <!-- 成功态 -->
          <template v-else>
            <div class="text-center py-4">
              <div class="w-14 h-14 mx-auto mb-4 rounded-full bg-green-50 flex items-center justify-center">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#52c41a" stroke-width="2.5"><path d="M20 6L9 17l-5-5"/></svg>
              </div>
              <h3 class="text-lg font-semibold text-[#1a1a2e] mb-2">密码修改成功</h3>
              <p class="text-sm text-gray-400 mb-6">请使用新密码重新登录</p>
              <button
                class="w-full h-11 text-sm text-white bg-[#3a64ab] rounded-lg hover:bg-[#264c8d] transition-colors"
                @click="goToLogin"
              >重新登录</button>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.popup-enter-from,
.popup-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.2s;
}
.dialog-enter-active > :last-child,
.dialog-leave-active > :last-child {
  transition: transform 0.2s, opacity 0.2s;
}
.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
.dialog-enter-from > :last-child {
  transform: scale(0.95);
  opacity: 0;
}
.dialog-leave-to > :last-child {
  transform: scale(0.95);
  opacity: 0;
}
</style>
