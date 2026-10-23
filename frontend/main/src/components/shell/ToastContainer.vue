<script setup>
import { useToast } from '@/utils/toast.js'

const { message, visible, type } = useToast()

const iconMap = {
  success: 'M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4 12 14.01 9 11.01',
  error: 'M18 6 6 18 M6 6l12 12',
  info: 'M12 16v-4 M12 8h.01',
}

const iconColor = { success: '#52c41a', error: '#ff4d4f', info: '#3a64ab' }
</script>

<template>
  <Teleport to="body">
    <Transition name="toast-fade">
      <div
        v-if="visible"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-[9999] bg-white rounded-lg shadow-lg border border-[#e8e8e8] px-5 py-3 flex items-center gap-2"
      >
        <svg class="w-4 h-4 shrink-0" viewBox="0 0 24 24" fill="none" :stroke="iconColor[type]" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path :d="iconMap[type]" />
        </svg>
        <span class="text-[13px] text-[#333]">{{ message }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.toast-fade-enter-active,
.toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from,
.toast-fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
