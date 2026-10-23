<script setup>
import { useToast } from '@/composables/useToast.js'

const { toast, toastType } = useToast()
</script>

<template>
  <Teleport to="body">
    <Transition name="toast-fade">
      <div
        v-if="toast"
        class="fixed top-5 right-5 z-[200] py-3 px-5 rounded-lg text-[13px] text-white shadow-lg max-w-[420px] leading-relaxed"
        :class="toastType === 'success' ? 'bg-[#52c41a]' : 'bg-[#ff4d4f]'"
      >{{ toast }}</div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.toast-fade-enter-active,
.toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from,
.toast-fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
