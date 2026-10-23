<script setup>
import { useConfirm } from '@/composables/useConfirm.js'

const { confirmState, onOk, onCancel } = useConfirm()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="confirmState.show" class="fixed inset-0 bg-black/40 z-[100] flex items-center justify-center" @click.self="onCancel">
        <div class="bg-white rounded-xl shadow-2xl w-[400px] max-w-[92vw] overflow-hidden" @click.stop>
          <div class="px-5 py-4 border-b border-[#f0f0f0]">
            <h3 class="text-[15px] font-semibold text-[#333]">{{ confirmState.title }}</h3>
          </div>
          <div class="px-5 py-5">
            <p class="text-[14px] text-[#333] leading-relaxed whitespace-pre-wrap">{{ confirmState.message }}</p>
          </div>
          <div class="px-5 py-3 border-t border-[#f0f0f0] flex justify-end gap-3">
            <button class="py-1.5 px-5 rounded bg-white border border-[#d9d9d9] text-[#666] text-[13px] cursor-pointer hover:border-[#3a64ab] hover:text-[#3a64ab] transition-all" @click="onCancel">取消</button>
            <button class="py-1.5 px-5 rounded bg-[#3a64ab] text-white text-[13px] cursor-pointer border-none hover:bg-[#2e528e] transition-colors" @click="onOk">确定</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }
</style>
